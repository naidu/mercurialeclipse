/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * The group containing both "dirty" files as also not yet committed changesets
 * <p>
 * This group CAN observe changes of the status cache. If it is added as a listener
 * to the {@link MercurialStatusCache}, it only tracks the state of the enabled (root)
 * projects (see {@link UncommittedChangesetManager#getProjects()}).
 *
 * @author Andrei
 */
public class UncommittedChangesetGroup extends ChangesetGroup implements Observer {


	private final List<IPropertyChangeListener> listeners;
	private final PropertyChangeEvent event;
	private HgSubscriberMergeContext context;

	private volatile boolean updateRequired;
	private volatile boolean cachingOn;

	private final MercurialStatusCache cache;
	private final Set<IFile> files;
	private final UncommittedChangesetManager ucsManager;

	public UncommittedChangesetGroup(UncommittedChangesetManager ucsManager) {
		super("Uncommitted", Direction.LOCAL);
		this.ucsManager = ucsManager;
		cache = MercurialStatusCache.getInstance();
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		event = new PropertyChangeEvent(this, "", null, "");
		files = new HashSet<IFile>();
	}

	/**
	 * @param sets non null
	 */
	public void setChangesets(Set<WorkingChangeSet> sets) {
		Set<ChangeSet> changesets = getChangesets();
		changesets.clear();
		changesets.addAll(sets);
	}

	public void addListener(IPropertyChangeListener listener){
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeListener(IPropertyChangeListener listener){
		listeners.remove(listener);
	}

	public void setContext(HgSubscriberMergeContext context) {
		this.context = context;
	}

	public void dispose() {
		MercurialStatusCache.getInstance().deleteObserver(this);
		clear();
	}

	public void clear(){
		files.clear();
		Set<ChangeSet> set = getChangesets();
		for (ChangeSet changeSet : set) {
			((WorkingChangeSet) changeSet).clear();
		}
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
			// update files in the default changeset
			ucsManager.getDefaultChangeset().add(file);

			// we need only one event
			if(cachingOn){
				updateRequired = true;
			} else {
				notifyListeners();
			}
		}
		return added;
	}

	public void remove(IResource file){
		// simply not supported, as it may be called not only from our code
	}

	/**
	 * TODO currently unused but initially implemented for the issue 10732
	 * @param paths
	 */
	protected void hide(IPath[] paths){
		if(context == null){
			return;
		}
		boolean changed = false;
		MercurialStatusCache statusCache = MercurialStatusCache.getInstance();
		Set<IProject> projects = getProjectSet();

		for (IPath path : paths) {
			if(path.segmentCount() < 2){
				continue;
			}
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject project = root.getProject(path.segment(0));
			if(project == null || !projects.contains(project)){
				continue;
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

	private Set<IProject> getProjectSet() {
		Set<IProject> projects = new HashSet<IProject>();
		if(ucsManager.getProjects() != null){
			projects.addAll(Arrays.asList(ucsManager.getProjects()));
		}
		return projects;
	}

	private void beginInput() {
		cachingOn = true;
	}

	private void endInput(IProgressMonitor monitor) {
		cachingOn = false;
		if(!updateRequired){
			return;
		}
		updateRequired = false;
		notifyListeners();
	}

	private void update(Set<IProject> projectSet){
		boolean changed = false;
		try {
			beginInput();
			clear();
			for (IProject project : projectSet) {
				changed |= update(project);
			}
		} finally {
			updateRequired |= changed;
			endInput(null);
		}
	}

	private boolean update(IProject project){
		Set<IProject> projects = getProjectSet();
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
					if(UncommittedChangesetGroup.this.equals(rule.cs)){
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

	private final class ExclusiveRule implements ISchedulingRule {
		private final UncommittedChangesetGroup cs;

		public ExclusiveRule(UncommittedChangesetGroup cs) {
			this.cs = cs;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExclusiveRule && cs.equals(((ExclusiveRule)rule).cs);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	public void update(Observable o, Object arg) {
		update(getProjectSet());
	}
}
