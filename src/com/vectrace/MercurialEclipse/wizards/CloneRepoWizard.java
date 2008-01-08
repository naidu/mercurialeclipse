/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.RepositoryCloneAction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/*
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * This class implements the import wizard extension and the new wizard
 * extension.
 * 
 */

public class CloneRepoWizard extends SyncRepoWizard
{
	
  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) 
  {
    setWindowTitle(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
    syncRepoLocationPage = new WizardCreateRepoLocationPage(true,"CreateRepoPage","Create Repository Location","Create a repository location to clone","no repo name",null);
  }


  /* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish()
  {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    final IProject project = workspace.getRoot().getProject(projectName);
    final HgRepositoryLocation repo = new HgRepositoryLocation(locationUrl);
    
    // Check that this project doesn't exist.
    if( project.getLocation() != null )
    {
      // TODO: Ask if user wants to torch everything there before the clone, otherwise fail.
      System.out.println( "Project " + projectName + " already exists");
      return false;
    }
    
    RepositoryCloneAction cloneRepoAction = new RepositoryCloneAction(null, workspace, repo, parameters, projectName,null);

    try
    {
      cloneRepoAction.run();
    }
    catch (Exception e)
    {
      System.out.println("Clone operation failed");
      System.out.println(e.getMessage());
    }

    // FIXME: Project creation must be done after the clone otherwise the
    // clone command will barf. Not quite sure why a destination directory isn't
    // really allowed to exist with the hg clone command...
    // At any rate we have a potential race condition on project creation if
    // anything to do with project is done before the clone operation.
    try
    {
      project.create(null);
      project.open(null);
    }
    catch(CoreException e)
    {
      // TODO: Should kill the project if we could map everything
      return false;      
    }

    try
    {
      // Register the project with Team. This will bring all the files that
      // we cloned into the project.
      RepositoryProvider.map(project, MercurialTeamProvider.class.getName());
      RepositoryProvider.getProvider(project, MercurialTeamProvider.class.getName());
    }
    catch(TeamException e)
    {
      // TODO: Should kill the project if we could map everything
      return false;
    }

    // It appears good. Stash the repo location.
    MercurialEclipsePlugin.getRepoManager().addRepoLocation(repo);

    return true;
  }	
  
}
