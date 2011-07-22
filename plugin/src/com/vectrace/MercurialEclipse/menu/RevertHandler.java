/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StructuredSelection;

import com.vectrace.MercurialEclipse.team.ActionRevert;

public class RevertHandler extends MultipleResourcesHandler {

	/**
	 * @see com.vectrace.MercurialEclipse.menu.MultipleResourcesHandler#run(java.util.List)
	 */
	@Override
	protected void run(List<IResource> resources) throws Exception {
		final ActionRevert revert = new ActionRevert();
		revert.selectionChanged(null, new StructuredSelection(resources));
		revert.run(null);
	}
}