/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class FileStatus {

	public static enum Action {
		MODIFIED('M'),
		ADDED('A'),
		REMOVED('R');

		private char action;

		private Action(char action) {
			this.action = action;
		}

		@Override
		public String toString() {
			return Character.toString(action);
		}
	}

	private final Action action;
	private final IPath path;
	private final HgRoot hgRoot;
	private IPath absPath;

	public FileStatus(Action action, String path, HgRoot hgRoot) {
		this.action = action;
		this.hgRoot = hgRoot;
		this.path = new Path(path);
	}

	public Action getAction() {
		return action;
	}

	public IPath getRootRelativePath() {
		return path;
	}

	public IPath getAbsolutePath(){
		if(absPath == null){
			absPath = hgRoot.toAbsolute(getRootRelativePath());
		}
		return absPath;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FileStatus [");
		if (action != null) {
			builder.append("action=");
			builder.append(action.name());
			builder.append(", ");
		}
		if (path != null) {
			builder.append("path=");
			builder.append(path);
		}
		builder.append("]");
		return builder.toString();
	}
}
