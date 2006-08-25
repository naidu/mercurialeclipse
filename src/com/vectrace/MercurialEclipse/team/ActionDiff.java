/**
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * @author zingo
 *
 */
public class ActionDiff implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
	public ActionDiff() {
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
		System.out.println("ActionDiff:init(window)");
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
		String FullPath;
    Shell shell;
    IWorkbench workbench;
    
    proj=MercurialUtilities.getProject(selection);
		Repository=MercurialUtilities.getRepositoryPath(proj);
		if(Repository==null)
		{
			Repository="."; //never leave this empty add a . to point to current path
		}

    //Get shell & workbench
    if((window !=null) && (window.getShell() != null))
    {
      shell=window.getShell();
    }
    else
    {
      workbench = PlatformUI.getWorkbench();
      shell = workbench.getActiveWorkbenchWindow().getShell();
    }

    
    
		Object obj;
	  Iterator itr; 
	    // the last argument will be replaced with a path
		String launchCmd[] = { MercurialUtilities.getHGExecutable(),"--cwd", Repository ,"diff", "" };
	    itr=selection.iterator();
	    while(itr.hasNext())
	    {
	    	obj=itr.next();
	    	if (obj instanceof IResource)
	    	{
				//Setup and run command
		    	FullPath=( ((IResource) obj).getLocation() ).toString();
		    	launchCmd[4]=FullPath;
//				    System.out.println(">" + launchCmd[0] + " " + launchCmd[1] + " " + launchCmd[2 ] + " " + launchCmd[3] + " " + launchCmd[4]);
 				  String output=MercurialUtilities.ExecuteCommand(launchCmd,true);
          if(output!=null)
          {
            //output output in a window
            if(output.length()!=0)
            {
              MessageDialog.openInformation(shell,"Mercurial Eclipse Diff",  launchCmd[4] + "\n" + output);
            }
            else
            {
              MessageDialog.openInformation(shell,"Mercurial Eclipse NoDiff" ,"No diff for " + launchCmd[4]);
            }
          }
        }
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
