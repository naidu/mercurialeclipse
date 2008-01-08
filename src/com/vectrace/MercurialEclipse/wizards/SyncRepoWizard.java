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

public abstract class SyncRepoWizard extends Wizard implements IImportWizard, INewWizard
{
  
  SyncRepoPage syncRepoLocationPage;
  
  String locationUrl;
  String parameters;
  String projectName;
  
  public SyncRepoWizard() {
    super();
//    System.out.println( "new SyncRepoWizard()");
    setNeedsProgressMonitor(true);
  }

  public boolean canFinish()
  {
    return (locationUrl != null) && (projectName != null);
  }

  // TODO: This should become part of an interface
  public void setLocationUrl( String url )
  {
    this.locationUrl = url;
  }
  
  // TODO: This should become part of an interface
  public void setParameters( String parms )
  {
    this.parameters = parms;
  }
  
  // TODO: This should become part of an interface
  public void setProjectName( String projectName )
  {
    this.projectName = projectName;
  }

   
  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) 
  {
    setWindowTitle(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
    syncRepoLocationPage = new SyncRepoPage(true,"CreateRepoPage","Create Repository Location","Create a repository location to clone",null,null);
  }
  
  
  public void dispose()
  {
    syncRepoLocationPage.dispose();
    
    super.dispose();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.IWizard#addPages()
   */
  public void addPages()
  {
    super.addPages();
    addPage(syncRepoLocationPage);
  }

  public IWizardPage getNextPage(IWizardPage page)
  {
    return null;
  }

  public IWizardPage getPreviousPage(IWizardPage page)
  {
    return null;
  }
}
