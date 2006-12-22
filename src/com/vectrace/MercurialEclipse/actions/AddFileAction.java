/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * TODO: LICENSE DETAILS
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 *
 */
public class AddFileAction extends TeamOperation
{
  private String resource;
  private IProject project;

  /**
   * @param context
   */
  public AddFileAction(IRunnableContext context, IProject project, String resource)
  {
    super(context);

    this.project = project;
    this.resource = resource;
  }


  /* (non-Javadoc)
   * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
  {
    // TODO: Would be nice to have something that indicates progress
    //       but that would require that functionality from the utilities.
    monitor.beginTask(getActionDescription(), 1);

    // TODO: Should probably have an Hg super class as there are many similarities.
    final String launchCmd[] =
    { MercurialUtilities.getHGExecutable(),
        "--cwd", MercurialUtilities.getRepositoryPath(project),
        "add", "\"" + resource + "\"" };

    try
    {
      MercurialUtilities.ExecuteCommand(launchCmd, true);
    } catch (HgException e)
    {
      System.out.println("Mercurial add failed: " + e.getMessage());
    }

    // TODO: Should this be in a finally block?
    monitor.done();
  }

  // TODO: No background for now.
  protected boolean canRunAsJob()
  {
    return false;
  }

  protected String getJobName()
  {
    return getActionDescription();
  }
  
  private String getActionDescription()
  {
    return new String("Mercurial add resource " + resource + " from the Mercurial repository.");    
  }
}
