/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.RepositoryPushAction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author zingo
 *
 */
public class PushRepoWizard extends SyncRepoWizard
{  
  
  IProject project;
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  @Override
public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    project = MercurialUtilities.getProject(selection);
    projectName = project.getName();
    setWindowTitle(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
    super.syncRepoLocationPage = new SyncRepoPage(false,"PushRepoPage","Push changes to repository","Select a repository location to push to",projectName,null);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
public boolean performFinish()
  {
//    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
//    final IProject project = workspace.getRoot().getProject(projectName);
    final HgRepositoryLocation repo = new HgRepositoryLocation(locationUrl);
    
    // Check that this project exist.
    if( project.getLocation() == null )
    {
      String msg = "Project " + projectName + " don't exists why push?";
      MercurialEclipsePlugin.logError(msg, null);
//	System.out.println( string);
      return false;
    }

    RepositoryPushAction repositoryPushAction = new RepositoryPushAction(null, project, repo,null);

    try
    {
      repositoryPushAction.run();
      if(repositoryPushAction.getResult().length() != 0)
      {
        Shell shell;
        IWorkbench workbench;

        workbench = PlatformUI.getWorkbench();
        shell = workbench.getActiveWorkbenchWindow().getShell();

        MessageDialog.openInformation(shell,"Mercurial Eclipse Push output",  repositoryPushAction.getResult());
      }
    }
    catch (Exception e)
    {
//      System.out.println("push operation failed");
//      System.out.println(e.getMessage());
    	 MercurialEclipsePlugin.logError("push operation failed",e);
    }

    // It appears good. Stash the repo location.
    MercurialEclipsePlugin.getRepoManager().addRepoLocation(repo);

    return true;
  }
}
