/*******************************************************************************
 * Copyright (c) 2011 John Peberdy and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.team.ActionAnnotate;

public class ShowAnnotationsHandler extends SingleResourceHandler {

	/**
	 * @see com.vectrace.MercurialEclipse.menu.SingleResourceHandler#run(org.eclipse.core.resources.IResource)
	 */
	@Override
	protected void run(IResource resource) throws Exception {
		if (resource != null) {
			ActionAnnotate.run(resource);
		}
	}
}