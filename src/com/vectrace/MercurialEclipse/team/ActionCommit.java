/**
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;


/**
 * @author zingo
 *
 */
public class ActionCommit implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
	public ActionCommit() {
		super();
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {

	}


	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		System.out.println("ActionStatus:init(window)");
		this.window = window;
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	

	public void run(IAction action) {
		IProject proj;
		String Repository;
    InputDialog commitDialog;
    Shell shell;
    IWorkbench workbench;
    
		proj=MercurialUtilities.getProject(selection);
		Repository=MercurialUtilities.getRepositoryPath(proj);
		if(Repository==null)
		{
			Repository="."; //never leave this empty add a . to point to current path
		}
		//Setup and run command
    
  if((window !=null) && (window.getShell() != null))
  {
    shell=window.getShell();
  }
  else
  {
    workbench = PlatformUI.getWorkbench();
    shell = workbench.getActiveWorkbenchWindow().getShell();
  }


  commitDialog = new InputDialog(shell,"Mercurial Eclipse Commit","Enter commit message",null,null);
  commitDialog.open();


  if(commitDialog.getValue() != null) 
  { //OK wa pressed and not Cancel
    //System.out.println("InputDialog: <OK> " + commitDialog.getValue());
    String launchCmd[] = { MercurialUtilities.getHGExecutable(),"--cwd", Repository ,"commit", "-m",commitDialog.getValue() };
    MercurialUtilities.ExecuteCommand(launchCmd);
  }
  
    
    
    
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
