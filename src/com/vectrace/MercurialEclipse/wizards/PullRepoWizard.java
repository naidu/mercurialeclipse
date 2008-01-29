/**
 * 
 */
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.RepositoryCloneAction;
import com.vectrace.MercurialEclipse.actions.RepositoryPullAction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author zingo
 *
 */
public class PullRepoWizard extends SyncRepoWizard
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
    super.syncRepoLocationPage = new SyncRepoPage(false,"PullRepoPage","Pull changes from repository","Select a repository location to pull from",projectName,null);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  public boolean performFinish()
  {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
//    final IProject project = workspace.getRoot().getProject(projectName);
    final HgRepositoryLocation repo = new HgRepositoryLocation(locationUrl);
    
    // Check that this project exist.
    if( project.getLocation() == null )
    {
//      System.out.println( "Project " + projectName + " don't exists why pull?");
      return false;
    }

    RepositoryPullAction repositoryPullAction = new RepositoryPullAction(null, project, repo,null);


    try
    {
      repositoryPullAction.run();
      if(repositoryPullAction.getResult().length() != 0)
      {
        Shell shell;
        IWorkbench workbench;

        workbench = PlatformUI.getWorkbench();
        shell = workbench.getActiveWorkbenchWindow().getShell();

        MessageDialog.openInformation(shell,"Mercurial Eclipse Pull output",  repositoryPullAction.getResult());
      }
    }
    catch (Exception e)
    {
    	MercurialEclipsePlugin.logError("pull operation failed", e);
//      System.out.println("pull operation failed");
//      System.out.println(e.getMessage());
    }

    // It appears good. Stash the repo location.
    MercurialEclipsePlugin.getRepoManager().addRepoLocation(repo);

    return true;
  }

  

}
