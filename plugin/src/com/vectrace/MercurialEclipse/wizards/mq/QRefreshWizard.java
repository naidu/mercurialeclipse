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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQHeaderClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQRefreshClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgOperationWizard;

/**
 * @author bastian
 */
public class QRefreshWizard extends HgOperationWizard {

	private static class RefreshOperation extends HgOperation {

		private final IResource resource;
		private final String message;
		private final String includes;
		private final String excludes;
		private final String user;
		private final String date;

		public RefreshOperation(IRunnableContext context, IResource resource, QNewWizardPage page) {
			super(context);

			this.resource = resource;
			this.message = page.getCommitTextDocument().get();
			this.includes = page.getIncludeTextField().getText();
			this.excludes = page.getExcludeTextField().getText();
			this.user = page.getUserTextField().getText();
			this.date = page.getDate().getText();
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
			monitor
					.beginTask(
							Messages.getString("QRefreshWizard.beginTask"), 2); //$NON-NLS-1$
			monitor.worked(1);
			monitor.subTask(Messages
					.getString("QRefreshWizard.subTask.callMercurial")); //$NON-NLS-1$

			try {
				HgQRefreshClient.refresh(resource, message, includes, excludes, user, date);
				monitor.worked(1);
				HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
				if(hgRoot != null) {
					new RefreshRootJob(hgRoot, RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
				}
			} catch (HgException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			} finally {
				monitor.done();
			}
		}
	}

	private final IResource resource;

	public QRefreshWizard(IResource resource) {
		super(Messages.getString("QRefreshWizard.title")); //$NON-NLS-1$
		this.resource = resource;
		setNeedsProgressMonitor(true);
		page = new QNewWizardPage(
				Messages.getString("QRefreshWizard.pageName"), Messages.getString("QRefreshWizard.pageTitle"), //$NON-NLS-1$ //$NON-NLS-2$
				null, null, resource, false);

		initPage(Messages.getString("QRefreshWizard.pageDescription"), page); //$NON-NLS-1$
		try {
			((QNewWizardPage)page).setCommitTextDocument(new Document(HgQHeaderClient.getHeader(resource)));
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
		return new RefreshOperation(getContainer(), resource, (QNewWizardPage) page);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#operationFinished()
	 */
	@Override
	protected void operationFinished() {
		super.operationFinished();
		PatchQueueView.getView().populateTable();
		new RefreshWorkspaceStatusJob(MercurialRootCache.getInstance().getHgRoot(resource),
				RefreshRootJob.WORKSPACE).schedule();
	}
}
