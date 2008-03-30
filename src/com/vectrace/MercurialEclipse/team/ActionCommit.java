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
 *     Sebastian                 - windows end of line fix
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.AddFileAction;
import com.vectrace.MercurialEclipse.actions.CommitAction;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;

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

  /*
   * 
  static public File getWorkingDir(IResource obj) 
  {
    return (obj.getLocation()).removeLastSegments(1).toFile();
  }


   * 
   * 
   */
  
  public void run(IAction action)
  {
    Shell shell;
    IWorkbench workbench;
    Object obj;
    Iterator itr;
    IProject project = MercurialUtilities.getProject(selection);

    if (( window != null ) && ( window.getShell() != null ))
    {
      shell = window.getShell();
    } 
    else
    {
      workbench = PlatformUI.getWorkbench();
      shell = workbench.getActiveWorkbenchWindow().getShell();
    }
    
    //Loop trough all selections and put in the resources in the selectedResourceArray
    itr= selection.iterator();
    ArrayList<IResource> selectedResourceArrayList = new ArrayList<IResource>(selection.size()); 
    while(itr.hasNext())
    {
      obj=itr.next();
      if (obj instanceof IResource)
      {
//      TODO convert the iterate to this array
        selectedResourceArrayList.add((IResource)obj);
      }
    }        
    selectedResourceArrayList.trimToSize();  
    IResource[] selectedResourceArray = selectedResourceArrayList.toArray(new IResource[0]);
    
    // TODO have per file working dir 
    //Get working dir (use first selected for now) 
    File workingDir=MercurialUtilities.getWorkingDir(selectedResourceArray[0]);

    IProgressMonitor monitor = new NullProgressMonitor();
    CommitDialog commitDialog = new CommitDialog(shell,project,selectedResourceArray);
    boolean ok = (commitDialog.open() == Window.OK);
    if(ok)
    {
      File[] filesToAdd = commitDialog.getFilesToAdd();

      //TODO switch to per file workingdir to allow for multiple repositoried in the same project     
//      File workingDir=new File(MercurialUtilities.getRepositoryPath(project));

            
      for(int file=0; file < filesToAdd.length; file++)
      {
        AddFileAction addFilesAction = new AddFileAction(null,project,filesToAdd[file].toString(),workingDir);
        try
        {
          addFilesAction.run(monitor);
        } 
        catch (Exception e)
        {
          System.out.println("Unable to finish add prior to commit: " + e.getMessage());
        }
      }
      
//      File[] filesToCommit = commitDialog.getFilesToCommit();
      IResource[] resourcesToCommit = commitDialog.getResourcesToCommit();
      String messageToCommit = commitDialog.getCommitMessage();
      boolean notEmpty;
      String getRootCmd[] = { MercurialUtilities.getHGExecutable(),"root"};                

      if(resourcesToCommit.length > 0 )
      {
        String eol = System.getProperty("line.separator");
        do 
        {
          ArrayList<String> list = new ArrayList<String>();
          String repository=null;
          String this_repository;
          notEmpty=false;
          for(int res=0; res < resourcesToCommit.length; res++)
          {
            IResource oneFile = resourcesToCommit[res];
            if(oneFile != null)
            {
              if(list.size() == 0 )
              {
                //Always add first free
//                System.out.println("First:" + oneFile.toString());
                
                //Get Root (reposetory root)
                File getRootWorkingDir=MercurialUtilities.getWorkingDir(oneFile);
                try
                {
                  repository = MercurialUtilities.ExecuteCommand(getRootCmd,getRootWorkingDir,true);
                  /* 
                   * on windows versions of mercurial before 1.0 the line ending was \r\n
                   * but since 1.0 it is only \n
                   */
                  if (repository.endsWith(eol)) 
                  {
                    workingDir=new File(repository.substring(0,repository.length() - eol.length() ));
                  } 
                  else 
                  {
                    // assume it ends with '\n'
                    workingDir=new File(repository.substring(0,repository.length() - 1 ));
                  }
                }
                catch(HgException e)
                {
                	MercurialEclipsePlugin.logError(e);
//                  System.out.println( e.getMessage() );
                  repository = null;
                  workingDir=null;
                }
                String filename=MercurialUtilities.getResourceName(oneFile,workingDir);
                list.add(filename);
                resourcesToCommit[res]=null; //clear the one we take out            
                notEmpty=true;
//                System.out.println("Commit: Rep: " + repository + "file:" + filename);
              }
              else
              {
                //Get Root (reposetory root)
//                System.out.println(" Next:" + oneFile.toString());
                File getRootWorkingDir=MercurialUtilities.getWorkingDir(oneFile);
                try
                {
                  this_repository = MercurialUtilities.ExecuteCommand(getRootCmd,getRootWorkingDir,true);
                }
                catch(HgException e)
                {
                	MercurialEclipsePlugin.logError(e);
//                  System.out.println( e.getMessage() );
                  this_repository = null;
                }
                
                if(this_repository.compareTo(repository) == 0) // Match? Is this file in the same reposetory?
                {
                  String filename=MercurialUtilities.getResourceName(oneFile,workingDir);
                  list.add(filename);
                  resourcesToCommit[res]=null; //clear the one we take out            
                  notEmpty=true;
//                  System.out.println("        Rep: " + this_repository + "file:" + filename);
                }
//                else
//                {
//                  System.out.println("  Not   Rep: " + this_repository + "file:" + oneFile.toString());                
//                }
              }
            }            
          }
          
          if(notEmpty)
          {
            String[] filesToCommit_per_repo= list.toArray(new String[0]);            
            CommitAction commitAction = new CommitAction(null, project,filesToCommit_per_repo, messageToCommit,workingDir );
            try
            {
              commitAction.run(monitor);
            } 
            catch (Exception e)
            {
            	MercurialEclipsePlugin.logError("Unable to finish commit: ", e);
//              System.out.println("Unable to finish commit: " + e.getMessage());
            }
          }
        } while(notEmpty); //Loop until we are empty.        
      }         
    }
    DecoratorStatus.refresh();
    //TODO Refresh history view TeamUI.getHistoryView().refresh();
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
