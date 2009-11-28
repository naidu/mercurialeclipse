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
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

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

	public WorkingChangeSet(String name) {
		super(-1, name, null, null, "", null, "", null, null); //$NON-NLS-1$
		direction = Direction.OUTGOING;
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		projects = new CopyOnWriteArraySet<IProject>();
		files = new CopyOnWriteArraySet<IFile>();
		event = new PropertyChangeEvent(this, "", null, "");
	}

	public void add(IFile file){
		if(context != null && context.isHidden(file)){
			return;
		}
		boolean added = files.add(file);
		if(added) {
			// we need only one event
			if(cachingOn){
				updateRequired = true;
			} else {
				notifyListeners();
			}
		}
	}

	private void notifyListeners() {
		Job job = new Job("Uncommitted changeset update"){
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
		};
		Job.getJobManager().cancel(ExclusiveRule.class);
		job.setRule(new ExclusiveRule());
		job.schedule(50);
	}

	@Override
	public void remove(IResource file){
		boolean removed = files.remove(file);
		if(removed) {
			if(cachingOn){
				updateRequired = true;
			} else {
				notifyListeners();
			}
		}
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
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
			if(project == null){
				continue;
			}
			IResource res = project.findMember(path.removeFirstSegments(1));
			// only allow to hide files which are dirty
			if(res instanceof IFile && !statusCache.isClean(res)){
				IFile file = (IFile) res;
				if(files.contains(file)){
					context.hide(file);
					files.remove(file);
					changed = true;
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
		files.clear();
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

	@SuppressWarnings("unchecked")
	public void update(Observable o, Object arg) {
		if (!(arg instanceof Set)) {
			return;
		}
		MercurialStatusCache cache = MercurialStatusCache.getInstance();
		boolean needUpdate = false;
		Set<IResource> changed = (Set<IResource>) arg;
		Map<IProject, List<IResource>> byProject = ResourceUtils.groupByProject(changed);
		Set<Entry<IProject,List<IResource>>> entrySet = byProject.entrySet();
		final int bits = MercurialStatusCache.MODIFIED_MASK;
		try {
			beginInput();
			for (Entry<IProject, List<IResource>> entry : entrySet) {
				IProject project = entry.getKey();
				if(!projects.contains(project)){
					continue;
				}
				clear();
				Set<IFile> files2 = cache.getFiles(bits, project);
				if(files2.isEmpty()){
					needUpdate = true;
				} else {
					for (IFile file : files2) {
						add(file);
					}
				}
//				List<IResource> list = entry.getValue();
//				for (IResource resource : list) {
//					if(resource instanceof IProject || list.contains(project)){
//						clear();
//						Set<IFile> files2 = cache.getFiles(bits, project);
//						if(files2.isEmpty()){
//							needUpdate = true;
//						} else {
//							for (IFile file : files2) {
//								add(file);
//							}
//						}
//						break;
//					} if (resource instanceof IFile) {
//						if (cache.isClean(resource)) {
//							remove(resource);
//						} else if(!cache.isIgnored(resource)){
//							add((IFile) resource);
//						}
//					}
//				}
			}
		} finally {
			updateRequired |= needUpdate;
			endInput(null);
		}
	}

	/**
	 * @param projects non null project list the changeset is responsible for
	 */
	public void setRoots(IProject[] projects) {
		this.projects.clear();
		for (IProject project : projects) {
			this.projects.add(project);
		}
	}

	public void dispose() {
		MercurialStatusCache.getInstance().deleteObserver(this);
		clear();
		projects.clear();
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
		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			return rule instanceof ExclusiveRule;
		}
	}
}