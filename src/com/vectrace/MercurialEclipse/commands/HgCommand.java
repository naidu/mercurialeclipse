/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.core.resources.IContainer;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class HgCommand extends AbstractShellCommand {

	public HgCommand(List<String> commands, File workingDir, boolean escapeFiles) {
		super(commands, workingDir, escapeFiles);
	}

	public HgCommand(String command, File workingDir, boolean escapeFiles) {
		super();
		this.command = command;
		this.workingDir = workingDir;
		this.escapeFiles = escapeFiles;
	}

	public HgCommand(String command, IContainer container,
			boolean escapeFiles) {
		this(command, container.getLocation().toFile(), escapeFiles);
	}

	public HgCommand(String command, boolean escapeFiles) {
		this(command, (File) null, escapeFiles);
	}

	protected String getHgExecutable() {
		return HgClients.getExecutable();
	}

	public HgRoot getHgRoot() throws HgException{
		return getHgRoot(workingDir);
	}

	protected String getDefaultUserName() {
		return HgClients.getDefaultUserName();
	}

	protected void addUserName(String user) {
		this.options.add("-u"); //$NON-NLS-1$
		// avoid empty user
		if(user != null) {
			user = user.trim();
			if (user.length() == 0) {
				user = null;
			}
		}
		this.options.add(user != null ? user : getDefaultUserName());
	}

	@Override
	public boolean executeToStream(OutputStream output, int timeout,
			boolean expectPositiveReturnValue) throws HgException {

		// Request non-interactivity flag
		List<String> cmd = getCommands();
		cmd.add(1, "-y");
		commands = cmd;
		// delegate to superclass
		return super.executeToStream(output, timeout, expectPositiveReturnValue);
	}

	@Override
	protected String getExecutable() {
		return getHgExecutable();
	}
}
