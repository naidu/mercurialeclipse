/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bastian                  - implementation
 *     Philip Graf              - load current commit text
 *     Andrei Loskutov          - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards.mq;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQHeaderClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQRefreshClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;
import com.vectrace.MercurialEclipse.wizards.mq.QNewWizard.FileOperation;

/**
 * @author bastian
 */
public class QRefreshWizard extends HgOperationWizard {

	private static class RefreshOperation extends FileOperation {

		public RefreshOperation(IRunnableContext context, HgRoot root, QNewWizardPage page) {
			super(context, root, page);
		}

		@Override
		protected String getActionDescription() {
			return Messages.getString("QRefreshWizard.actionDescription"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("QRefreshWizard.beginTask"), 4); //$NON-NLS-1$
			monitor.worked(1);

			try {
				addRemoveFiles(monitor);
				monitor.subTask(Messages.getString("QRefreshWizard.subTask.callMercurial")); //$NON-NLS-1$
				HgQRefreshClient.refresh(root, message, allResources, user, date);
				monitor.worked(1);
			} catch (HgException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			} finally {
				monitor.done();
			}
		}
	}

	private final HgRoot root;

	public QRefreshWizard(HgRoot root) {
		super(Messages.getString("QRefreshWizard.title")); //$NON-NLS-1$
		this.root = root;
		setNeedsProgressMonitor(true);
		page = new QNewWizardPage(
				Messages.getString("QRefreshWizard.pageName"), Messages.getString("QRefreshWizard.pageTitle"), //$NON-NLS-1$ //$NON-NLS-2$
				null, null, root, false);

		initPage(Messages.getString("QRefreshWizard.pageDescription"), page); //$NON-NLS-1$
		try {
			((QNewWizardPage)page).setCommitTextDocument(new Document(HgQHeaderClient.getHeader(root)));
		} catch (HgException e) {
			MercurialEclipsePlugin.logWarning("Cannot read header of current patch.", e);
		}
		addPage(page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#initOperation()
	 */
	@Override
	protected HgOperation initOperation() {
		return new RefreshOperation(getContainer(), root, (QNewWizardPage) page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		super.operationFinished();
		PatchQueueView.getView().populateTable();
		new RefreshWorkspaceStatusJob(root, RefreshRootJob.LOCAL).schedule();
	}
}
