/**
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.actions.AddFileAction;
import com.vectrace.MercurialEclipse.actions.CommitAction;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;

/**
 * @author zingo
 * 
 */
public class ActionCommit implements IWorkbenchWindowActionDelegate
{

  private IWorkbenchWindow window;

  private IStructuredSelection selection;

  public ActionCommit()
  {
    super();
  }

  /**
   * We can use this method to dispose of any system resources we previously
   * allocated.
   * 
   * @see IWorkbenchWindowActionDelegate#dispose
   */
  public void dispose()
  {

  }

  /**
   * We will cache window object in order to be able to provide parent shell for
   * the message dialog.
   * 
   * @see IWorkbenchWindowActionDelegate#init
   */
  public void init(IWorkbenchWindow window)
  {
    // System.out.println("ActionCommit:init(window)");
    this.window = window;
  }

  /**
   * The action has been activated. The argument of the method represents the
   * 'real' action sitting in the workbench UI.
   * 
   * @see IWorkbenchWindowActionDelegate#run
   */

  public void run(IAction action)
  {
    Shell shell;
    IWorkbench workbench;

    IProject project = MercurialUtilities.getProject(selection);

    if (( window != null ) && ( window.getShell() != null ))
    {
      shell = window.getShell();
    } else
    {
      workbench = PlatformUI.getWorkbench();
      shell = workbench.getActiveWorkbenchWindow().getShell();
    }

    IProgressMonitor monitor = new NullProgressMonitor();
    CommitDialog commitDialog = new CommitDialog(shell,project);
    boolean ok = (commitDialog.open() == Window.OK);
    if(ok)
    {
      String[] filesToAdd = commitDialog.getFilesToAdd();
      for(int file=0; file < filesToAdd.length; file++)
      {
        AddFileAction addFilesAction = new AddFileAction(null,
                                                         project,
                                                         filesToAdd[file]);
        try
        {
          addFilesAction.run(monitor);
        } catch (Exception e)
        {
          System.out.println("Unable to finish add prior to commit: " + e.getMessage());
        }
      }
      
      CommitAction commitAction = new CommitAction(null,
                                                   project,
                                                   commitDialog.getFilesToCommit(),
                                                   commitDialog.getCommitMessage());
    
      try
      {
        commitAction.run(monitor);
      } catch (Exception e)
      {
        System.out.println("Unable to finish commit: " + e.getMessage());
      }
    }

    DecoratorStatus.refresh();
  }

  /**
   * Selection in the workbench has been changed. We can change the state of the
   * 'real' action here if we want, but this can only happen after the delegate
   * has been created.
   * 
   * @see IWorkbenchWindowActionDelegate#selectionChanged
   */
  public void selectionChanged(IAction action, ISelection in_selection)
  {
    if (in_selection != null && in_selection instanceof IStructuredSelection)
    {
      selection = (IStructuredSelection) in_selection;
    }
  }

}
