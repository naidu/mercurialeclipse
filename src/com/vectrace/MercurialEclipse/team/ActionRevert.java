/**
 * com.vectrace.MercurialEclipse (c) Vectrace Sep 12, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;


import java.io.File;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.exception.HgException;


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
//    System.out.println("ActionAdd:init(window)");
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
    proj=MercurialUtilities.getProject(selection);
    
    System.out.println("Revert:");

   
//    IWorkspaceRunnable myRunnable = new IWorkspaceRunnable() 
    {
//      public void run(IProgressMonitor monitor) throws CoreException 
      {
        Object obj;
        Iterator itr; 
        String FullPath;
        String Repository;
//        IProject proj;
        System.out.println("Revert in runnable");
        proj=MercurialUtilities.getProject(selection);
        Repository=MercurialUtilities.getRepositoryPath(proj);
        if(Repository==null)
        {
          Repository="."; //never leave this empty add a . to point to current path
        }

        // the last argument will be replaced with a path
        String launchCmd[] = { MercurialUtilities.getHGExecutable(),"revert", "" };

        
        //do the actual work in here
        itr=selection.iterator();
        while(itr.hasNext())
        {
          obj=itr.next();
          
         
          if (obj instanceof IResource)
          {
            IResource resource=(IResource) obj;

            if(MercurialUtilities.isResourceInReposetory(resource, true) == true)
            {
              //Resource could be inside a link or something do nothing
              // in the future this could check is this is another repository

              //Setup and run command
              File workingDir=MercurialUtilities.getWorkingDir(resource);
              launchCmd[2] = MercurialUtilities.getResourceName(resource);
//              System.out.println("Revert = " + FullPath);
  //            IResourceChangeEvent event = new IResourceChangeEvent();
        
              try
              {
                resource.touch(null);
              }
              catch (CoreException e)
              {
                e.printStackTrace();
              } 
  
              try
              {
                MercurialUtilities.ExecuteCommand(launchCmd,workingDir,true);          
                resource.touch(null);
              } catch (HgException e)
              {
                System.out.println(e.getMessage());
              } catch (CoreException e)
              {
                e.printStackTrace();
              } 
            }

          }
        }
//        notify();
        DecoratorStatus.refresh();
        
    }
  } //;
//  IWorkspace workspace = ResourcesPlugin.getWorkspace();
//  try
//  {
//    workspace.run(myRunnable, proj, IWorkspace.AVOID_UPDATE, null);
//    try
//    {
//      myRunnable.wait();
//    }
//    catch (InterruptedException e)
//    {
//      e.printStackTrace();
//    }
//  }
//  catch (CoreException e)
//  {
//    e.printStackTrace();
//  }

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
