/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.core.runtime.IAdaptable;

import com.aragost.javahg.commands.ResolveStatusLine;

public class ResolveStatus implements IAdaptable {

	private final IAdaptable adaptable;
	private final ResolveStatusLine.Type flag;

	public ResolveStatus(IAdaptable adaptable, ResolveStatusLine.Type flag) {
		this.adaptable = adaptable;
		this.flag = flag;
	}

	public Object getAdapter(Class adapter) {
		if (adaptable == null) {
			return null;
		}
		return adaptable.getAdapter(adapter);
	}

	public ResolveStatusLine.Type getFlag() {
		return this.flag;
	}

	public String getStatus() {
		return flag == ResolveStatusLine.Type.UNRESOLVED ? Messages
				.getString("FlaggedAdaptable.unresolvedStatus") : Messages
				.getString("FlaggedAdaptable.resolvedStatus");
	}

	public boolean isUnresolved() {
		return flag == ResolveStatusLine.Type.UNRESOLVED;
	}

	public boolean isResolved() {
		return flag == ResolveStatusLine.Type.RESOLVED;
	}
}
