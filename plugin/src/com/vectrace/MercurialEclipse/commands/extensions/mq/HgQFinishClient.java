/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions.mq;

import java.util.List;

import org.eclipse.core.runtime.Assert;

import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Patch;

/**
 * @author bastian
 *
 */
public class HgQFinishClient extends AbstractClient {
	private static AbstractShellCommand makeCommand(HgRoot root) {
		Assert.isNotNull(root);
		AbstractShellCommand command = new HgCommand("qfinish", //$NON-NLS-1$
				"Invoking qfinish", root, true);
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(root));
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$

		return command;
	}

	@SuppressWarnings("null")
	public static String finish(HgRoot root, List<Patch> revs) throws HgException {
		Assert.isTrue(revs != null && revs.size() > 0);
		AbstractShellCommand command = makeCommand(root);

		for (Patch p : revs) {
			command.addOptions(p.getName());
		}

		return command.executeToString();
	}

	/**
	 * Calls qfinish -a
	 */
	public static String finishAllApplied(HgRoot root) throws HgException {
		AbstractShellCommand command = makeCommand(root);
		command.addOptions("--applied");

		return command.executeToString();
	}
}
