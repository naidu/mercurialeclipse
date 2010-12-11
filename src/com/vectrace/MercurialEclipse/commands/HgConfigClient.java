/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class HgConfigClient extends AbstractClient {
	public static String getHgConfigLine(IResource dir, String key)
			throws HgException {
		String[] lines = getHgConfigLines(dir, key);
		return lines[0];
	}

	private static String[] getHgConfigLines(IResource root, String key)
			throws HgException {
		AbstractShellCommand cmd = new RootlessHgCommand("showconfig");
		cmd.addOptions(key);
		String[] lines = cmd.executeToString().split("\n"); //$NON-NLS-1$
		return lines;
	}
}
