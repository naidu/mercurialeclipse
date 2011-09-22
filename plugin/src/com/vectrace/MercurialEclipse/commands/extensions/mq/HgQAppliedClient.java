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
public class HgQAppliedClient extends AbstractClient {
	public static List<Patch> getAppliedPatches(HgRoot root) throws HgException {
		Assert.isNotNull(root);
		AbstractShellCommand command = new HgCommand("qapplied", "Invoking qapplied", root, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(root));
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$
		command.addOptions("-v"); //$NON-NLS-1$
		command.addOptions("-s"); //$NON-NLS-1$
		return HgQSeriesClient.parse(command.executeToString());
	}

	public static List<Patch> getUnappliedPatches(HgRoot root) throws HgException{
		Assert.isNotNull(root);
		AbstractShellCommand command = new HgCommand("qunapplied", "Invoking qunapplied", root, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(root));
		command.addOptions("--config", "extensions.hgext.mq="); //$NON-NLS-1$ //$NON-NLS-2$
		command.addOptions("-v"); //$NON-NLS-1$
		command.addOptions("-s"); //$NON-NLS-1$
		return HgQSeriesClient.parse(command.executeToString());
	}

}
