/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import com.vectrace.MercurialEclipse.wizards.ImportWizard;


public class ActionImport implements IWorkbenchWindowActionDelegate 
{

  private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
  private IStructuredSelection selection;
    
  public ActionImport() 
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
    ImportWizard importWizard = new ImportWizard();
    importWizard.init(workbench, selection);
    
    WizardDialog pullWizardDialog = new WizardDialog(shell,importWizard);
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
