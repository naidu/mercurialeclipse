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
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
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

	private static final String REMOVED = "removed";
	private static final String ADDED = "added";
	private final List<IPropertyChangeListener> listeners;
	private final List<PropertyChangeEvent> eventCache;
	private boolean cachingOn;
	private final Set<IProject> projects;

	private HgSubscriberMergeContext context;

	public WorkingChangeSet(String name) {
		super(-1, name, null, null, "", null, "", null, null); //$NON-NLS-1$
		direction = Direction.OUTGOING;
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		eventCache = new ArrayList<PropertyChangeEvent>();
		projects = new CopyOnWriteArraySet<IProject>();
		files = new CopyOnWriteArraySet<IFile>();
	}

	public void add(IFile file){
		if(context != null && context.isHidden(file)){
			return;
		}
		boolean added = files.add(file);
		if(added) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, ADDED, null, file);
			// we need only one event
			if(cachingOn && eventCache.isEmpty()){
				eventCache.add(event);
			} else {
				for (IPropertyChangeListener listener : listeners) {
					listener.propertyChange(event);
				}
			}
		}
	}

	@Override
	public void remove(IResource file){
		boolean removed = files.remove(file);
		if(removed) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, REMOVED, file, null);
			if(cachingOn && eventCache.isEmpty()){
				eventCache.add(event);
			} else {
				for (IPropertyChangeListener listener : listeners) {
					listener.propertyChange(event);
				}
			}
		}
	}

	public void hide(IPath[] paths){
		if(context == null){
			return;
		}
		boolean changed = false;
		for (IPath path : paths) {
			path = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(path);
			IFile file = ResourceUtils.getFileHandle(path);
			if(files.contains(file)){
				context.hide(file);
				files.remove(file);
				changed = true;
			}
		}
		PropertyChangeEvent event = new PropertyChangeEvent(this, REMOVED, null, null);
		if(changed){
			eventCache.add(event);
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
		if(eventCache.isEmpty()){
			return;
		}
		Collections.reverse(eventCache);
		for (IPropertyChangeListener listener : listeners) {
			for (PropertyChangeEvent event : eventCache) {
				listener.propertyChange(event);
			}
		}
		eventCache.clear();
	}

	@SuppressWarnings("unchecked")
	public void update(Observable o, Object arg) {
		if (!(arg instanceof Set)) {
			return;
		}
		MercurialStatusCache cache = MercurialStatusCache.getInstance();
		try {
			beginInput();
			Set<IResource> changed = (Set<IResource>) arg;

			final int bits = MercurialStatusCache.MODIFIED_MASK;
			for (IResource resource : changed) {
				IProject project = resource.getProject();
				if(!projects.contains(project)){
					continue;
				}
				if(resource instanceof IProject || changed.contains(project)){
					clear();
					Set<IFile> files2 = cache.getFiles(bits, project);
					if(files2.isEmpty()){
						PropertyChangeEvent event = new PropertyChangeEvent(this, ADDED, null, project);
						eventCache.add(event);
					} else {
						for (IFile file : files2) {
							add(file);
						}
					}
					break;
				} if (resource instanceof IFile) {
					if (cache.isClean(resource)) {
						remove(resource);
					} else if(!cache.isIgnored(resource)){
						add((IFile) resource);
					}
				}
			}
		} finally {
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

}