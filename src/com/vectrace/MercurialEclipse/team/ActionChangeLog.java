/**
 * com.vectrace.MercurialEclipse (c) Vectrace Aug 28, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;


/**
 * @author zingo
 *
 */
public class ActionChangeLog implements IWorkbenchWindowActionDelegate {

  private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
  public ActionChangeLog() {
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
//    System.out.println("ActionCommit:init(window)");
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
    final IWorkbenchPage activePage = MercurialEclipsePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
    final Shell shell = Display.getDefault().getActiveShell();
    try 
    {
      new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() 
        {
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException 
          {
            final IResource resource = (IResource) selection.getFirstElement();
            Runnable r = new Runnable() 
              {
              public void run() 
                {
                  TeamUI.showHistoryFor(activePage, resource, null);
                }
              };
              Display.getDefault().asyncExec(r);
          }
        });
    } 
    catch (InvocationTargetException e) 
    {
      System.out.println( e.getMessage() );
    } 
    catch (InterruptedException e) 
    {
      System.out.println( e.getMessage() );
    }

/*    
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
    
    Object obj;
    Iterator itr; 

    
    IWorkbenchPage activePage = MercurialEclipsePlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
    //Show View
    IViewPart viewPart = null;
    try 
    {
      viewPart = activePage.showView(MercurialEclipsePlugin.ID_ChangeLogView);
    } 
    catch (PartInitException e) 
    {
      System.out.println(e.getMessage());
      e.printStackTrace();
    }            
   
    ChangeLogView changeLogView = (ChangeLogView) viewPart;

    // the last argument will be replaced with a path
    itr=selection.iterator();
    while(itr.hasNext())
    {
      obj=itr.next();

      if (obj instanceof IResource)
      {
//        System.out.println("log: if (obj instanceof IResource) == TRUE");
        IResource resource=(IResource) obj;
        if(MercurialUtilities.isResourceInReposetory(resource, true) == true)
        {
          // send output to the ChangeLogView               
            changeLogView.showChangeLog(resource);
//            System.out.println("changeLogView.showChangeLog(FullPath,output)");
        }
      }
    }
    
//  DecoratorStatus.refresh();
 
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
