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
package com.vectrace.MercurialEclipse.wizards.mq;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQNewClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;

/**
 * @author bastian
 *
 */
public class QNewWizard extends HgOperationWizard {

	private static class NewOperation extends HgOperation {

		private final HgRoot root;
		private final String message;
		private final String includes;
		private final String excludes;
		private final String user;
		private final String date;
		private final String patchName;

		public NewOperation(IRunnableContext context, HgRoot root, QNewWizardPage page) {
			super(context);

			this.root = root;
			this.message = page.getCommitTextDocument().get();
			this.includes = page.getIncludeTextField().getText();
			this.excludes = page.getExcludeTextField().getText();
			this.user = page.getUserTextField().getText();
			this.date = page.getDate().getText();
			this.patchName = page.getPatchNameTextField().getText();
		}

		/**
		 *  @see com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
		 */
		@Override
		protected String getActionDescription() {
			return Messages.getString("QNewWizard.actionDescription"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("QNewWizard.beginTask"), 2); //$NON-NLS-1$
			monitor.worked(1);
			monitor.subTask(Messages.getString("QNewWizard.subTask.callMercurial")); //$NON-NLS-1$

			try {
				HgQNewClient.createNewPatch(root, message, includes, excludes, user, date,
						patchName);
				monitor.worked(1);
				monitor.done();
			} catch (HgException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
		}
	}

	private final HgRoot root;

	public QNewWizard(HgRoot root) {
		super(Messages.getString("QNewWizard.title")); //$NON-NLS-1$
		this.root = root;
		setNeedsProgressMonitor(true);
		page = new QNewWizardPage(Messages.getString("QNewWizard.pageName"), Messages.getString("QNewWizard.pageTitle"), null, null, //$NON-NLS-1$ //$NON-NLS-2$
				true);
		initPage(Messages.getString("QNewWizard.pageDescription"), //$NON-NLS-1$
				page);
		addPage(page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#initOperation()
	 */
	@Override
	protected HgOperation initOperation() {
		return new NewOperation(getContainer(), root, (QNewWizardPage) page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		super.operationFinished();
		PatchQueueView.getView().populateTable();
		new RefreshWorkspaceStatusJob(root, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
	}
}
