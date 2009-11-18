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
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * A temporary changeset which holds not commited resources. This changeset cannot be used
 * as a usual changeset, as many of it's functionality is not supported or limited.
 *
 * @author Andrei
 */
public class WorkingChangeSet extends ChangeSet implements Observer {

	private static final String REMOVED = "removed";
	private static final String ADDED = "added";
	private final List<IPropertyChangeListener> listeners;
	private final List<PropertyChangeEvent> eventCache;
	private boolean cachingOn;

	public WorkingChangeSet(String name) {
		super(-1, name, null, null, "", null, "", null, null); //$NON-NLS-1$
		direction = Direction.OUTGOING;
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
		eventCache = new ArrayList<PropertyChangeEvent>();
	}

	public void add(IFile file){
		Set<IFile> files2 = getFiles();
		files = new HashSet<IFile>(files2);
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
		Set<IFile> files2 = getFiles();
		files = new HashSet<IFile>(files2);
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
		files = new HashSet<IFile>();
	}

	public void beginInput() {
		cachingOn = true;
	}

	public void endInput(IProgressMonitor monitor) {
		cachingOn = false;
		Collections.reverse(eventCache);
		for (IPropertyChangeListener listener : listeners) {
			for (PropertyChangeEvent event : eventCache) {
				listener.propertyChange(event);
			}
		}
		eventCache.clear();
	}

	public void update(Observable o, Object arg) {
//		if (resource instanceof IFile) {
//			if (STATUS_CACHE.isClean(resource) || !resource.exists()) {
//				uncommittedSet.remove(resource);
//			} else {
//				uncommittedSet.add((IFile) resource);
//			}
//		}
	}
}