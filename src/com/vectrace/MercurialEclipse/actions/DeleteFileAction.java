/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * TODO: LICENSE DETAILS
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;


/**
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * Action to delete files using Mercurial.
 */
public class DeleteFileAction extends TeamOperation
{
  private IResource resource;

  /**
   * IResource should only be a file or folder as operations on a folder are implicitly on all
   * subtending files in Mercurial.
   */
  public DeleteFileAction(IRunnableContext context, IResource resource)
  {
    super(context);

    this.resource = resource;
  }

  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
  {
    // TODO: Would be nice to have something that indicates progress
    //       but that would require that functionality from the utilities.
    monitor.beginTask(getActionDescription(), 1);

    // TODO: Should we consider making  --force optional?
    final String launchCmd[] =
    { MercurialUtilities.getHGExecutable(),
        "--cwd", MercurialUtilities.getRepositoryPath(resource.getProject()),
        "remove", "--force", resource.getLocation().toOSString() };

    try
    {
      MercurialUtilities.ExecuteCommand(launchCmd, true);
    } catch (HgException e)
    {
      System.out.println("Mercurial delete failed: " + e.getMessage());
    }

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
    return new String("Mercurial delete resource " + resource.getLocation() + " from the Mercurial repository.");    
  }
}
