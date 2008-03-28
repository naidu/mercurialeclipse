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
 *     Stefan Groschupf          - logError
 *******************************************************************************/

package com.vectrace.MercurialEclipse.team;


import java.io.File;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import sun.security.jgss.spi.MechanismFactory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;

public class ActionAdd implements IWorkbenchWindowActionDelegate {

//	private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
	public ActionAdd() {
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
		String Repository;
		proj=MercurialUtilities.getProject(selection);
		Repository=MercurialUtilities.getRepositoryPath(proj);
		if(Repository==null)
		{
			Repository="."; //never leave this empty add a . to point to current path
		}

    // TODO: Refactor using ActionAdd
	  	Object obj;
	    Iterator itr; 
	    // the last argument will be replaced with a path
  		String launchCmd[] = { MercurialUtilities.getHGExecutable(),"add","--", "" };
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
  		    	launchCmd[3] = MercurialUtilities.getResourceName(resource);
//            System.out.println("Add>" + launchCmd[0] + " " + launchCmd[1] + " " + launchCmd[2 ] + "---->Workdir:" + workingDir.toString());
            try
            {
              MercurialUtilities.ExecuteCommand(launchCmd,workingDir,true);
            }
            catch(HgException e)
            {
            	MercurialEclipsePlugin.logError(e);
//              System.out.println( e.getMessage() );
            }
          }
	    	}
	    }
      
      DecoratorStatus.refresh();
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
