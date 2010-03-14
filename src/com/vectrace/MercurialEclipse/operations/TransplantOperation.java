/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient.TransplantOptions;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;

public class TransplantOperation extends HgOperation {

	private final TransplantOptions options;
	private final IHgRepositoryLocation repo;
	private final HgRoot hgRoot;

	public TransplantOperation(IRunnableContext context, HgRoot hgRoot, TransplantOptions options,
			IHgRepositoryLocation repo) {
		super(context);
		this.hgRoot = hgRoot;
		this.options = options;
		this.repo = repo;
	}

	@Override
	public void run(final IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {

		monitor.beginTask("Transplanting changesets...", IProgressMonitor.UNKNOWN); //$NON-NLS-1$

		// Timer which is used to monitor the monitor cancellation
		Timer t = new Timer("Transplant watcher", false);

		// only start timer if the operation is NOT running in the UI thread
		if(Display.getCurrent() == null){
			final Thread threadToCancel = Thread.currentThread();
			t.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					if (monitor.isCanceled() && !threadToCancel.isInterrupted()) {
						threadToCancel.interrupt();
					}
				}
			}, 500, 50);
		}

		try {
			result = HgTransplantClient.transplant(hgRoot, repo, options);
			if (result != null && result.length() != 0) {
				HgClients.getConsole().printMessage(result, null);
			}
		} catch (HgException e) {
			throw new InvocationTargetException(e);
		} finally {
			RefreshWorkspaceStatusJob job = new RefreshWorkspaceStatusJob(hgRoot,
					RefreshRootJob.ALL);
			job.schedule();
			job.join();
			t.cancel();
		}
	}

	@Override
	protected String getActionDescription() {
		return "Transplanting to " + hgRoot.getName();
	}

}