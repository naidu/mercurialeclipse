/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.vectrace.MercurialEclipse.dialogs.CommitResource;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class UntrackedFilesFilter extends ViewerFilter {
	private final boolean allowMissing;

	public UntrackedFilesFilter(boolean allowMissing){
		super();
		this.allowMissing = allowMissing;
	}

	/**
	 * Filter out untracked files.
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof CommitResource){
			char status = ((CommitResource) element).getStatus();
			if (status == MercurialStatusCache.CHAR_UNKNOWN) {
				return false;
			}
			if (!allowMissing && status == MercurialStatusCache.CHAR_MISSING) {
				return false;
			}
		}
		return true;
	}
}