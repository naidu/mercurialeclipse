/**
 * com.vectrace.MercurialEclipse (c) Vectrace Sep 12, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;


import java.util.Iterator;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


/**
 * @author zingo
 *
 */

public class ActionRevert implements IWorkbenchWindowActionDelegate {

//  private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
  public ActionRevert() {
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
    System.out.println("ActionAdd:init(window)");
//    this.window = window;
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
    proj=MercurialUtilities.getProject(selection);
    Repository=MercurialUtilities.getRepositoryPath(proj);
    if(Repository==null)
    {
      Repository="."; //never leave this empty add a . to point to current path
    }

    Object obj;
      Iterator itr; 
      // the last argument will be replaced with a path
    String launchCmd[] = { MercurialUtilities.getHGExecutable(),"--cwd", Repository ,"revert", "" };
/*
    
    IWorkspaceRunnable myRunnable = new IWorkspaceRunnable() 
    {
      public void run(IProgressMonitor monitor) throws CoreException 
      {
*/
        //do the actual work in here
        itr=selection.iterator();
        while(itr.hasNext())
        {
          obj=itr.next();
          if (obj instanceof IResource)
          {
            IResource resource=(IResource) obj;
          //Setup and run command
            FullPath=resource.getLocation().toString();
            launchCmd[4]=FullPath;
//            IResourceChangeEvent event = new IResourceChangeEvent();
      
            try
            {
              resource.touch(null);
            }
            catch (CoreException e)
            {
              e.printStackTrace();
            } 
            MercurialUtilities.ExecuteCommand(launchCmd,true);          
            try
            {
              resource.touch(null);
            }
            catch (CoreException e)
            {
              e.printStackTrace();
            } 
          }
        }
        DecoratorStatus.refresh();
        
        /*
    }
  }
  IWorkspace workspace = ResourcesPlugin.getWorkspace();
  workspace.run(myRunnable, myProject, IWorkspace.AVOID_UPDATE, null);
 */ 
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
