/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * TODO: LICENSE DETAILS
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.wizards.CloneRepoWizard;

/*
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * This class encapsulates the hg clone command.
 * 
 */
public class CloneRepositoryAction extends TeamOperation
{
  private HgRepositoryLocation repo;
  private IWorkspace workspace;
  private String cloneParameters;
  private String projectName;

  /**
   * @param context
   */
  public CloneRepositoryAction(IRunnableContext context, IWorkspace workspace, HgRepositoryLocation repo, String cloneParameters, String projectName)
  {
    super(context);

    this.workspace = workspace;
    this.repo = repo;
    this.cloneParameters = cloneParameters;
    this.projectName = projectName;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
  {
     //Setup and run command
      Shell shell = null;
    
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow win = workbench.getActiveWorkbenchWindow();
      //TODO win is always ńull we get no shell why why why?
      if(win!=null)
      {
        shell = win.getShell();
      }

    
    // TODO: Would be nice to have something that indicates progress
    //       but that would require that functionality from the utilities.
    monitor.beginTask("Mercurial clone operation", 1);

    /*    
    final String launchCmd[] =
    { 
      MercurialUtilities.getHGExecutable(), "--cwd", workspace.getRoot().getLocation().toOSString(), "clone", cloneParameters != null ? cloneParameters : "", repo.getUrl(), projectName };
    };
*/

    ArrayList launchCmd = new ArrayList();
    
    // Shell command setup.
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add( "clone" );
    if(cloneParameters != null)
    {
      launchCmd.add(cloneParameters );
    }
    launchCmd.add(repo.getUrl() );
    launchCmd.add(workspace.getRoot().getLocation().addTrailingSeparator().toOSString() + projectName );
    
    try
    {
      String output = MercurialUtilities.ExecuteCommand((String[])launchCmd.toArray(new String[0]), shell==null);

      if(output != null && shell != null)
      {
        //output output in a window
        if(output.length()!=0)
        {
          MessageDialog.openInformation(shell,"Mercurial Eclipse Clone from " + repo.getUrl(),  output);
        }
      }

    } catch (HgException e)
    {
      System.out.println(e.getMessage());
    }

    monitor.done();
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
   * 
   * The CloneRepositoryAction is not allowed to run as a job as the CloneRepoWizard will attempt to continue on
   * otherwise. TODO: Figure out how to make the clone repo wizard block. Presumably need a job monitor.
   */
  protected boolean canRunAsJob()
  {
    return false;
  }

  protected String getJobName()
  {
    String name = "Clone Mercurial Repository from " + repo.getUrl();
    return name;
  }

}
