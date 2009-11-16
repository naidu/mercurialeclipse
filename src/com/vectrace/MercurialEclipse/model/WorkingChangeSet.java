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

import java.util.HashSet;
import java.util.List;
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
public class WorkingChangeSet extends ChangeSet {

	private static final String REMOVED = "removed";
	private static final String ADDED = "added";
	private final List<IPropertyChangeListener> listeners;

	public WorkingChangeSet(String name) {
		super(-1, name, null, null, "", null, "", null, null); //$NON-NLS-1$
		direction = Direction.OUTGOING;
		listeners = new CopyOnWriteArrayList<IPropertyChangeListener>();
	}

	public void add(IFile file){
		Set<IFile> files2 = getFiles();
		files = new HashSet<IFile>(files2);
		boolean added = files.add(file);
		if(added) {
			for (IPropertyChangeListener listener : listeners) {
				listener.propertyChange(new PropertyChangeEvent(this, ADDED, null, file));
			}
		}
	}

	@Override
	public void remove(IResource file){
		Set<IFile> files2 = getFiles();
		files = new HashSet<IFile>(files2);
		boolean removed = files.remove(file);
		if(removed) {
			for (IPropertyChangeListener listener : listeners) {
				listener.propertyChange(new PropertyChangeEvent(this, REMOVED, file, null));
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

	public void beginInput() {
		// TODO Auto-generated method stub

	}

	public void endInput(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}
}