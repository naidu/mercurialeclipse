/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - many updates
 *     Stefan Groschupf          - logError
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.compare.CompareUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.IdentifyAction;
import com.vectrace.MercurialEclipse.exception.HgException;


public class ActionDiff implements IWorkbenchWindowActionDelegate 
{
   
//  private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
	public ActionDiff() 
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
//		System.out.println("ActionDiff:init(window)");
//		this.window = window;
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
//		String Repository;
    
    proj=MercurialUtilities.getProject(selection);
//		Repository=MercurialUtilities.getRepositoryPath(proj);
//		if(Repository==null)
//		{
//			Repository="."; //never leave this empty add a . to point to current path
//		}

		Object obj;
	  Iterator itr; 
    // the last argument will be replaced with a path
    itr=selection.iterator();
    while(itr.hasNext())
    {
    	obj=itr.next();
    	if (obj instanceof IResource)
    	{
    	  
    	  SyncInfoCompareInput comparedialog = getCompareInput((IResource)obj);
    	  if(comparedialog!=null)
    	  {
          CompareUI.openCompareEditor(comparedialog);
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

  public SyncInfoCompareInput getCompareInput(IResource obj)
  {
    //Setup and run command identify, this us used to get the base changeset to diff against
    //tip can't be used since work can be done in older revison ( hg up <old rev> )
    //String FullPath = ( ((IResource) obj).getLocation() ).toString();

    String changeset;
    File workingDir=MercurialUtilities.getWorkingDir( obj );

    IdentifyAction identifyAction = new IdentifyAction(null, obj.getProject(), workingDir);
    try
    {
      identifyAction.run();
      changeset = identifyAction.getChangeset(); 
    }
    catch (Exception e)
    {
      MercurialEclipsePlugin.logError("pull operation failed", e);
//      System.out.println("pull operation failed");
//      System.out.println(e.getMessage());
      
      IWorkbench workbench = PlatformUI.getWorkbench();
      Shell shell = workbench.getActiveWorkbenchWindow().getShell();
      MessageDialog.openInformation(shell,"Mercurial Eclipse couldn't identify hg revision of " + obj.getName().toString(),  identifyAction.getResult());
      return null;
    }
    
    try
    {

//tmp testing

/*     
          try
          {
            PlatformUI.getWorkbench().getProgressService().run(true, true,      
              new IRunnableWithProgress() 
              {
                 public void run(IProgressMonitor monitor) 
                 {
                    //do UI work
                   final int ticks = 600000;
                   monitor.beginTask("Doing some work", ticks);
                   try 
                   {
                      for (int i = 0; i < ticks; i++) {
                         if (monitor.isCanceled())
                            return; //status.CANCEL_STATUS;
                         monitor.subTask("Processing tick #" + i);
                         //... do some work ...
                         monitor.worked(1);
                         System.out.println("loop=" + i);
                      }
                   } 
                   finally 
                   {
                      monitor.done();
                   }
                   return;
                 }
              }
            );
          }
          catch (InvocationTargetException e)
          {
            MercurialEclipsePlugin.logError(e);
          }
          catch (InterruptedException e)
          {
            MercurialEclipsePlugin.logError(e);
          }      
*/      
/*          
      // Create a file system subscriber and specify that the
   // subscriber will synchronize with the provided file system location
      MercurialRepositorySubscriber subscriber = new MercurialRepositorySubscriber();

   // Allow the subscriber to refresh its state
   subscriber.refresh(subscriber.roots(), IResource.DEPTH_INFINITE, null);
*/
/*
   // Collect all the synchronization states and print
   IResource[] children = subscriber.roots();
   for(int i=0; i < children.length; i++) {
     printSyncState(subscriber,children[i]);
   }
*/
//tmp testing done
      
      // Setup and run command diff

      MercurialRepositorySubscriber subscriber = new MercurialRepositorySubscriber();
/*
      System.out.println("diff(" + obj.toString() + ",...,...)");
      printSyncState(subscriber,obj);
*/
//      IStorageMercurialRevision iStorage = new IStorageMercurialRevision( (IResource) obj, changeset);
//      SyncInfo syncInfo = subscriber.getSyncInfo((IResource) obj, iStorage, iStorage);
      SyncInfo syncInfo = subscriber.getSyncInfo(obj);
      SyncInfoCompareInput comparedialog = new SyncInfoCompareInput("diff", syncInfo);
      return comparedialog;
    } 
    catch (HgException e)
    {
    	MercurialEclipsePlugin.logError(e);
//      System.out.println(e.getMessage());
    } 
    catch (TeamException e)
    {
    	MercurialEclipsePlugin.logError(e);
//      e.printStackTrace();
    }
    return null;
  }

  void printSyncState(MercurialRepositorySubscriber subscriber, IResource resource) 
  {
    IResource[] children;
    try
    {
      if(resource != null)
      {
        System.out.println(subscriber.getSyncInfo(resource).toString());
      }
      else
      {
        System.out.println("printSyncState: resoure is null :(");
        
      }
      children = subscriber.members(resource);
      for(int i=0; i < children.length; i++) 
      {
        IResource child = children[i];
        if(! child.exists()) 
        {
          System.out.println(resource.getFullPath() + " doesn't exist in the workspace");
        }
        printSyncState(subscriber, children[i]);
      }
    }
    catch (TeamException e)
    {
      MercurialEclipsePlugin.logError(e);
    }

  }

  
}
