/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * john	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;

/**
 * A command to invoke hg definitely outside of an hg root.
 *
 * @author john
 */
public class RootlessHgCommand extends AbstractShellCommand {

	public RootlessHgCommand(String command, boolean escapeFiles) {
		super(null, null, escapeFiles);
	}

	public RootlessHgCommand(String string, File workingDir, boolean escapeFiles) {
		super(null, workingDir, escapeFiles);
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.commands.AbstractShellCommand#getExecutable()
	 */
	@Override
	protected String getExecutable() {
		return HgClients.getExecutable();
	}
}
