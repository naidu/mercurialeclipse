/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	     implementation
 * Philip Graf   load current commit text
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards.mq;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.Document;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQHeaderClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQRefreshClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.views.PatchQueueView;
import com.vectrace.MercurialEclipse.wizards.HgWizard;

/**
 * @author bastian
 */
public class QRefreshWizard extends HgWizard {
	private final QNewWizardPage page;

	private class RefreshOperation extends HgOperation {

		public RefreshOperation(IRunnableContext context) {
			super(context);
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
				HgQRefreshClient
						.refresh(resource, page.getCommitTextDocument().get(),
								page
								.getForceCheckBox().getSelection(), page
								.getGitCheckBox().getSelection(), page
								.getIncludeTextField().getText(), page
								.getExcludeTextField().getText(), page
								.getUserTextField().getText(), page.getDate()
								.getText());
				monitor.worked(1);
				new RefreshRootJob(AbstractClient.getHgRoot(resource), RefreshRootJob.LOCAL_AND_OUTGOING).schedule();
				monitor.done();
			} catch (HgException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
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
			page.setCommitTextDocument(new Document(HgQHeaderClient.getHeader(resource)));
		} catch (HgException e) {
			MercurialEclipsePlugin.logWarning("Cannot read header of current patch.", e);
		}
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		RefreshOperation refreshOperation = new RefreshOperation(getContainer());
		try {
			getContainer().run(false, false, refreshOperation);
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			page.setErrorMessage(e.getLocalizedMessage());
			return false;
		}
		PatchQueueView.getView().populateTable();
		return true;
	}

}
