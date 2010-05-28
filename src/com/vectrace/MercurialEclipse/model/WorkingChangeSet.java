/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * A temporary changeset which holds not commited resources. This changeset cannot be used
 * as a usual changeset, as many of it's functionality is not supported or limited.
 * <p>
 * This changeset CAN observe changes of the status cache. If it is added as a listener
 * to the {@link MercurialStatusCache}, it only tracks the state of the enabled (root)
 * projects (see {@link #setRoots(IProject[])}).
 *
 * @author Andrei
 */
public class WorkingChangeSet extends ChangeSet implements Observer {

	private final List<IPropertyChangeListener> listeners;
	private volatile boolean updateRequired;
	private volatile boolean cachingOn;
	private final Set<IProject> projects;

	private HgSubscriberMergeContext context;
	private final PropertyChangeEvent event;
	private final MercurialStatusCache cache = MercurialStatusCache.getInstance();

	public WorkingChangeSet(String name) {
		super(-1, name, null, null, "", null, "", null, null); //$NON-NLS-1$
		direction = Direction.OUTGOING;
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		projects = new HashSet<IProject>();
		files = new HashSet<IFile>();
		event = new PropertyChangeEvent(this, "", null, "");
	}

	private boolean add(IFile file){
		if(context != null && context.isHidden(file)){
			return false;
		}
		if(cache.isDirectory(file.getLocation())){
			return false;
		}
		boolean added;
		synchronized (files){
			added = files.add(file);
		}
		if(added) {
			// we need only one event
			if(cachingOn){
				updateRequired = true;
			} else {
				notifyListeners();
			}
		}
		return added;
	}

	private void notifyListeners() {
		Job updateJob = new Job("Uncommitted changeset update"){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (IPropertyChangeListener listener : listeners) {
					listener.propertyChange(event);
				}
				monitor.done();
				return Status.OK_STATUS;
			}
			@Override
			public boolean belongsTo(Object family) {
				return family == ExclusiveRule.class;
			}

			@Override
			public boolean shouldSchedule() {
				Job[] jobs = Job.getJobManager().find(ExclusiveRule.class);
				for (Job job : jobs) {
					ExclusiveRule rule = (ExclusiveRule) job.getRule();
					if(WorkingChangeSet.this.equals(rule.cs)){
						// do not schedule me because exactly the same job is waiting to be started!
						return false;
					}
				}
				return true;
			}
		};
		updateJob.setRule(new ExclusiveRule(this));
		updateJob.schedule(50);
	}

	@Override
	public void remove(IResource file){
		// simply not supported, as it may be called not only from our code
	}

	public void hide(IPath[] paths){
		if(context == null){
			return;
		}
		boolean changed = false;
		MercurialStatusCache statusCache = MercurialStatusCache.getInstance();
		for (IPath path : paths) {
			if(path.segmentCount() < 2){
				continue;
			}
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(path.segment(0));
			synchronized (projects) {
				if(project == null || !projects.contains(project)){
					continue;
				}
			}
			IResource res = project.findMember(path.removeFirstSegments(1));
			// only allow to hide files which are dirty
			if(res instanceof IFile && !statusCache.isClean(res)){
				IFile file = (IFile) res;
				synchronized (files) {
					if(files.contains(file)){
						context.hide(file);
						files.remove(file);
						changed = true;
					}
				}
			}
		}
		if(changed){
			updateRequired = true;
			endInput(null);
		}
	}

	public void addListener(IPropertyChangeListener listener){
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(IPropertyChangeListener listener){
		listeners.remove(listener);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		String changeset = getChangeset();
		int size = getFiles().size();
		if(size == 0){
			return changeset + " (empty)";
		}
		return changeset + " (" + size + ")";
	}

	public void clear(){
		synchronized (files){
			files.clear();
		}
	}

	public void beginInput() {
		cachingOn = true;
	}

	public void endInput(IProgressMonitor monitor) {
		cachingOn = false;
		if(!updateRequired){
			return;
		}
		updateRequired = false;
		notifyListeners();
	}

	public void update(Observable o, Object arg) {
		boolean changed = false;
		try {
			beginInput();
			clear();
			synchronized (projects) {
				for (IProject project : projects) {
					changed |= update(project);
				}
			}
		} finally {
			updateRequired |= changed;
			endInput(null);
		}
	}

	private boolean update(IProject project){
		if(!projects.contains(project)){
			return false;
		}
		final int bits = MercurialStatusCache.MODIFIED_MASK;
		Set<IFile> files2 = cache.getFiles(bits, project);
		if(files2.isEmpty()){
			return true;
		}
		boolean changed = false;
		for (IFile file : files2) {
			changed |= add(file);
		}
		return changed;
	}

	/**
	 * @param projects non null project list the changeset is responsible for
	 */
	public void setRoots(IProject[] projects) {
		synchronized (this.projects) {
			this.projects.clear();
			for (IProject project : projects) {
				this.projects.add(project);
			}
		}
	}

	public void dispose() {
		MercurialStatusCache.getInstance().deleteObserver(this);
		clear();
		synchronized (projects) {
			projects.clear();
		}
	}

	public void setContext(HgSubscriberMergeContext context) {
		this.context = context;
	}

	@Override
	public FileFromChangeSet[] getChangesetFiles() {
		Set<IFile> files2 = getFiles();
		int diffKind = Differencer.CHANGE | Differencer.RIGHT;

		List<FileFromChangeSet> fcs = new ArrayList<FileFromChangeSet>(files2.size());
		for (IFile file : files2) {
			fcs.add(new FileFromChangeSet(this, file, diffKind));
		}
		return fcs.toArray(new FileFromChangeSet[0]);
	}

	private final class ExclusiveRule implements ISchedulingRule {
		private final WorkingChangeSet cs;

		public ExclusiveRule(WorkingChangeSet cs) {
			this.cs = cs;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExclusiveRule && cs.equals(((ExclusiveRule) rule).cs);
		}
	}
}