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
 * Action to move files using Mercurial.
 */
public class MoveFileAction extends TeamOperation
{
  private IResource src;
  private IResource dest;

  public MoveFileAction(IRunnableContext context, IResource src, IResource dest)
  {
    super(context);

    this.src = src;
    this.dest = dest;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
  {
    // TODO: Would be nice to have something that indicates progress
    //       but that would require that functionality from the utilities.
    monitor.beginTask("Mercurial move file operation", 1);

    final String launchCmd[] =
    { MercurialUtilities.getHGExecutable(),
        "--cwd", MercurialUtilities.getRepositoryPath(src.getProject()),
        "rename", "--force", src.getLocation().toOSString(), dest.getLocation().toOSString() };

    try
    {
      MercurialUtilities.ExecuteCommand(launchCmd, true);
    } catch (HgException e)
    {
      System.out.println("Mercurial rename failed: " + e.getMessage());
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
    String name = "Move " + src.getLocation() + " int the Mercurial repository to " + dest.getLocation() + ".";
    return name;
  }  
}
