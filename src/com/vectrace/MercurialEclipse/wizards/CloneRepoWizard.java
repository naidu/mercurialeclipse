/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * TODO: LICENSE DETAILS
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
import com.vectrace.MercurialEclipse.actions.CloneRepositoryAction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/*
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * This class implements the import wizard extension and the new wizard
 * extension.
 * 
 */

public class CloneRepoWizard extends Wizard implements IImportWizard, INewWizard
{
	
  private CloneRepoWizardCreateRepoLocationPage createRepoLocationPage;
  
  private String locationUrl;
  private String cloneParameters;
  private String projectName;
  
	public CloneRepoWizard() {
		super();
    System.out.println( "new CloneRepoWizard()");
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
  public void setCloneParameters( String parms )
  {
    this.cloneParameters = parms;
  }
  
  // TODO: This should become part of an interface
  public void setProjectName( String projectName )
  {
    this.projectName = projectName;
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
    
    CloneRepositoryAction cloneRepoAction = 
         new CloneRepositoryAction(null, workspace, repo, cloneParameters, projectName);

    try
    {
      cloneRepoAction.run();
    }
    catch (Exception e)
    {
      System.out.println("Clone operation failed");
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
	 
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		createRepoLocationPage = new CloneRepoWizardCreateRepoLocationPage("CreateRepoPage","Create Repository Location",null);
	}
	
  
	public void dispose()
  {
    createRepoLocationPage.dispose();
    
    super.dispose();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.IWizard#addPages()
   */
  public void addPages()
  {
    super.addPages();
    addPage(createRepoLocationPage);
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
