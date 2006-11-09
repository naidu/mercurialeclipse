/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * TODO: LICENSE DETAILS
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

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
  public CloneRepositoryAction(IRunnableContext context, IWorkspace workspace, 
                               HgRepositoryLocation repo, String cloneParameters, String projectName)
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
    // TODO: Would be nice to have something that indicates progress
    //       but that would require that functionality from the utilities.
    monitor.beginTask("Mercurial clone operation", 1);

    final String launchCmd[] =
    { MercurialUtilities.getHGExecutable(), "--cwd", workspace.getRoot().getLocation().toOSString(),
        "clone", cloneParameters != null ? cloneParameters : "", repo.getUrl(), projectName };

    // TODO: Would be nice to have a failure code as I'm sure the hg command
    // does return
    // a proper failure code. I'm sure it can also throw something.
    try
    {
      MercurialUtilities.ExecuteCommand(launchCmd, true);
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
