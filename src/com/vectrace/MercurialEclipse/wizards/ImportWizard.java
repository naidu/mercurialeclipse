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
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.ImportAction;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;


public class ImportWizard extends SyncRepoWizard
{  
  
  IProject project;
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection)
  {
    project = MercurialUtilities.getProject(selection);
    projectName = project.getName();
    setWindowTitle(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
    super.syncRepoLocationPage = (SyncRepoPage) new ImportRepoPage("ImportPage","Import changes from patchfile","Select a patch file to Import from",projectName,null);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  public boolean performFinish()
  {
//    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
//    final IProject project = workspace.getRoot().getProject(projectName);
    final String importFile = locationUrl;
    
    // Check that this project exist.
    if( project.getLocation() == null )
    {
//      System.out.println( "Project " + projectName + " don't exists why pull?");
      return false;
    }

    ImportAction importAction = new ImportAction(null, project, importFile,null);


    try
    {
      importAction.run();
      if(importAction.getResult().length() != 0)
      {
        Shell shell;
        IWorkbench workbench;

        workbench = PlatformUI.getWorkbench();
        shell = workbench.getActiveWorkbenchWindow().getShell();

        MessageDialog.openInformation(shell,"Mercurial Eclipse Import output",  importAction.getResult());
      }
    }
    catch (Exception e)
    {
    	MercurialEclipsePlugin.logError("import operation failed", e);
//      System.out.println("pull operation failed");
//      System.out.println(e.getMessage());
    }

    return true;
  }

  

}
