/**
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import com.vectrace.MercurialEclipse.actions.RepositoryPullAction;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.wizards.PullRepoWizard;


/**
 * @author zingo
 *
 */
public class ActionPull implements IWorkbenchWindowActionDelegate 
{

  private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
  private IStructuredSelection selection;
    
  public ActionPull() 
  {
    super();
  }

  /**
   * We can use this method to dispose of any system
   * resources we previously allocated.
   * @see IWorkbenchWindowActionDelegate#dispose
   */
  public void dispose() 
  {

  }


  /**
   * We will cache window object in order to
   * be able to provide parent shell for the message dialog.
   * @see IWorkbenchWindowActionDelegate#init
   */
  public void init(IWorkbenchWindow window) 
  {
//    System.out.println("ActionPull:init(window)");
    this.window = window;
  }

  /**
   * The action has been activated. The argument of the
   * method represents the 'real' action sitting
   * in the workbench UI.
   * @see IWorkbenchWindowActionDelegate#run
   */
  

  public void run(IAction action) 
  {
    IProject proj;
    String Repository;
    Shell shell;
    IWorkbench workbench;
   
    proj=MercurialUtilities.getProject(selection);
    Repository=MercurialUtilities.getRepositoryPath(proj);
    if(Repository==null)
    {
      Repository="."; //never leave this empty add a . to point to current path
    }

    //Get shell & workbench
    workbench = PlatformUI.getWorkbench();
    if((window !=null) && (window.getShell() != null))
    {
      shell=window.getShell();
    }
    else
    {
      shell = workbench.getActiveWorkbenchWindow().getShell();
    }


    
    //Setup and run command
    PullRepoWizard pullRepoWizard = new PullRepoWizard();
    pullRepoWizard.init(workbench, selection);
    
    WizardDialog pullWizardDialog = new WizardDialog(shell,pullRepoWizard);
//    pullWizardDialog.setBlockOnOpen(true); 
    pullWizardDialog.open();
  }
  
  
  /**
   * Selection in the workbench has been changed. We 
   * can change the state of the 'real' action here
   * if we want, but this can only happen after 
   * the delegate has been created.
   * @see IWorkbenchWindowActionDelegate#selectionChanged
   */
  public void selectionChanged(IAction action, ISelection in_selection) 
  {
    if( in_selection != null && in_selection instanceof IStructuredSelection )
    {
      selection = ( IStructuredSelection )in_selection;
    }
  }


  
}
