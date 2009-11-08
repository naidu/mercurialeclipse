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
package com.vectrace.MercurialEclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgBranchClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author bastian
 *
 */
public class AddBranchWizard extends HgWizard {
	private final AddBranchPage branchPage;
	private final IResource resource;

	private class AddBranchOperation extends HgOperation {

		public AddBranchOperation(IRunnableContext context) {
			super(context);
		}

		@Override
		protected String getActionDescription() {
			return Messages.getString("AddBranchWizard.AddBranchOperation.actionDescription"); //$NON-NLS-1$
		}

		@Override
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			try {
				monitor.beginTask(Messages.getString("AddBranchWizard.AddBranchOperation.taskName"), 1); //$NON-NLS-1$
				HgBranchClient.addBranch(resource, branchPage
						.getBranchNameTextField().getText(), MercurialUtilities
						.getHGUsername(), branchPage.getForceCheckBox()
						.getSelection());
				monitor.worked(1);
				HgClients.getConsole().printMessage(result, null);
				if(resource instanceof IProject){
					IProject project = (IProject) resource;
					String branch = HgBranchClient.getActiveBranch(project.getLocation().toFile());
					MercurialTeamProvider.setCurrentBranch(branch, project);
				}
				resource.touch(monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
			monitor.done();
		}
	}

	public AddBranchWizard(IResource resource) {
		super(Messages.getString("AddBranchWizard.windowTitle")); //$NON-NLS-1$
		this.resource = resource;
		setNeedsProgressMonitor(true);
		branchPage = new AddBranchPage(Messages.getString("AddBranchWizard.branchPage.name"), //$NON-NLS-1$
				Messages.getString("AddBranchWizard.branchPage.title"), MercurialEclipsePlugin.getImageDescriptor("wizards/newstream_wizban.gif"), //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("AddBranchWizard.branchPage.description")); //$NON-NLS-1$
		addPage(branchPage);
	}

	@Override
	public boolean performFinish() {
		AddBranchOperation op = new AddBranchOperation(getContainer());
		try {
			getContainer().run(false, false, op);
		} catch (Exception e) {
			branchPage.setErrorMessage(e.getLocalizedMessage());
			return false;
		}
		return super.performFinish();
	}
}
