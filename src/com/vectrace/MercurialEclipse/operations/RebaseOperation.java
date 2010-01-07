/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.HgRebaseClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author bastian
 */
public class RebaseOperation extends HgOperation {

	private final HgRoot hgRoot;
	private int sourceRev = -1;
	private int destRev = -1;
	private int baseRev = -1;
	private final boolean collapse;
	private final boolean abort;
	private final boolean cont;

	public RebaseOperation(IRunnableContext context, HgRoot hgRoot,
			int sourceRev, int destRev, int baseRev, boolean collapse,
			boolean abort, boolean cont) {
		super(context);
		this.hgRoot = hgRoot;
		this.sourceRev = sourceRev;
		this.destRev = destRev;
		this.baseRev = baseRev;
		this.collapse = collapse;
		this.abort = abort;
		this.cont = cont;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(getActionDescription(), 3);
		try {
			monitor.worked(1);
			monitor.subTask(Messages.getString("RebaseOperation.calling")); //$NON-NLS-1$
			result = HgRebaseClient.rebase(hgRoot,
					sourceRev,
					baseRev, destRev, collapse, cont, abort);
			monitor.worked(1);
			monitor.subTask(Messages.getString("RebaseOperation.refreshing")); //$NON-NLS-1$
			LocalChangesetCache.getInstance().refreshAllLocalRevisions(hgRoot, true);
			monitor.worked(1);
		} catch (HgException e) {
			throw new InvocationTargetException(e, e.getLocalizedMessage());
		}
		monitor.done();
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("RebaseOperation.rebasing"); //$NON-NLS-1$
	}

}
