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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class HgCommand extends AbstractShellCommand {

	private static final Set<String> COMMANDS_CONFLICTING_WITH_USER_ARG =
		Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
				"clone", "pull", "resolve", "showconfig", "status", "unbundle"
		)));

	private String lastUserName;

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

	/**
	 * <b>NOTE!</b> this method works only for hg commands which knows "-u" argument
	 * AND which understood "-u" as user name. There are commands which accept "-u" but
	 * threat is differently: like "resolve" or "status" (see {@link #isConflictingWithUserArg()}).
	 *
	 * @param user might be null or empty. In such case, a default user name weill be used.
	 * @throws IllegalArgumentException if the command uses "-u" NOT as user name parameter
	 */
	public void addUserName(String user) throws IllegalArgumentException {

		// avoid empty user
		user = user != null ? user : MercurialUtilities.getDefaultUserName();
		if(user != null) {
			user = user.trim();
			if (user.length() == 0) {
				user = null;
			} else {
				user = quote(user);
			}
		}
		if(user != null) {
			if (isConflictingWithUserArg()) {
				throw new IllegalArgumentException("Command '" + command
						+ "' uses '-u' argument NOT as user name!");
			}
			options.add("-u"); //$NON-NLS-1$
			options.add(user);
			this.lastUserName = user;
		} else {
			this.lastUserName = null;
		}
	}

	/**
	 * Remembers the user name given as option (see {@link #addUserName(String)}) as the default
	 * user name for current hg root. Should be called only after the command was successfully
	 * executed. If no hg root or no user name option was given, does nothing.
	 */
	public void rememberUserName(){
		if(lastUserName == null){
			return;
		}
		HgRoot hgRoot;
		try {
			hgRoot = getHgRoot();
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return;
		}
		String commitName = HgCommitMessageManager.getDefaultCommitName(hgRoot);
		if(!commitName.equals(lastUserName)) {
			HgCommitMessageManager.setDefaultCommitName(hgRoot, lastUserName);
		}
	}

	private boolean isConflictingWithUserArg() {
		if(command == null){
			// TODO can it happen???
			return false;
		}
		return COMMANDS_CONFLICTING_WITH_USER_ARG.contains(command);
	}

	/**
	 * @param str non null, non empty string
	 * @return non null string with escaped quotes (depending on the OS)
	 */
	private static String quote(String str) {
		if (!MercurialUtilities.isWindows()) {
			return str;
		}
		// escape quotes, otherwise commit will fail at least on windows
		return str.replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected boolean executeToStream(OutputStream output, int timeout,
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
