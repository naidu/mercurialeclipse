/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov         - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

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

import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * A temporary changeset which holds not commited resources. This changeset cannot be used
 * as a usual changeset, as many of it's functionality is not supported or limited.
 * <p>
 * This changeset CAN observe changes of the status cache. If it is added as a listener
 * to the {@link MercurialStatusCache}, it only tracks the state of the enabled (root)
 * projects (see {@link #setProjects(IProject[])}).
 *
 * For history see WorkingChangeSet
 *
 * @author Andrei
 */
public class UncommittedChangeSet extends WorkingChangeSet implements Observer, IUncommitted {

	private final List<IPropertyChangeListener> listeners;
	private volatile boolean updateRequired;
	private volatile boolean cachingOn;
	private final Set<IProject> projects;

	private HgSubscriberMergeContext context;
	private final PropertyChangeEvent event;
	private final MercurialStatusCache cache = MercurialStatusCache.getInstance();

	public UncommittedChangeSet() {
		super("Uncommitted");

		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		projects = new HashSet<IProject>();
		files = new HashSet<IFile>();
		event = new PropertyChangeEvent(this, "", null, "");
	}

	public boolean add(IFile file){
		if(context != null && context.isHidden(file)){
			return false;
		}
		if(cache.isDirectory(ResourceUtils.getPath(file))){
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
					if(UncommittedChangeSet.this.equals(rule.cs)){
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

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted#addListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	public void addListener(IPropertyChangeListener listener){
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted#removeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	public void removeListener(IPropertyChangeListener listener){
		listeners.remove(listener);
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

	/**
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
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
	 *
	 * @see com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted#setProjects(org.eclipse.core.resources.IProject[])
	 */
	public void setProjects(IProject[] projects) {
		synchronized (this.projects) {
			this.projects.clear();
			for (IProject project : projects) {
				this.projects.add(project);
				HgRoot root = MercurialTeamProvider.getHgRoot(project);
				if(root != null) {
					this.projects.add(root.getResource());
				}
			}
		}
	}

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted#getProjects()
	 */
	public IProject[] getProjects() {
		synchronized (this.projects) {
			return this.projects.toArray(new IProject[projects.size()]);
		}
	}

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted#dispose()
	 */
	public void dispose() {
		MercurialStatusCache.getInstance().deleteObserver(this);
		clear();
		synchronized (projects) {
			projects.clear();
		}
	}

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted#setContext(com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext)
	 */
	public void setContext(HgSubscriberMergeContext context) {
		this.context = context;
	}

	/**
	 * @see com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider.IUncommitted#getContext()
	 */
	public HgSubscriberMergeContext getContext() {
		return context;
	}

	private final class ExclusiveRule implements ISchedulingRule {
		private final UncommittedChangeSet cs;

		public ExclusiveRule(UncommittedChangeSet cs) {
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