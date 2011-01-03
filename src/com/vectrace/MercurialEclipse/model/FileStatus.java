/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov 		 - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class FileStatus {

	public static enum Action {
		/** 'M' */
		MODIFIED(),
		/** 'A' */
		ADDED(),
		/** 'R' */
		REMOVED();
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((hgRoot == null) ? 0 : hgRoot.hashCode());
		result = prime * result + ((path == null) ? 0 : path.toPortableString().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FileStatus)) {
			return false;
		}
		FileStatus other = (FileStatus) obj;
		if (action != other.action) {
			return false;
		}
		if (hgRoot == null) {
			if (other.hgRoot != null) {
				return false;
			}
		} else if (!hgRoot.equals(other.hgRoot)) {
			return false;
		}
		if (path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!path.toPortableString().equals(other.path.toPortableString())) {
			return false;
		}
		return true;
	}
}
