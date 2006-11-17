/**
 * 
 */
package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author Peter
 * 
 * Mercurial status operation.
 *
 */
public class StatusContainerAction extends TeamOperation
{

  private IResource[] resources;
  private String result;

  /**
   * @param context
   */
  public StatusContainerAction(IRunnableContext context, IResource[] resources)
  {
    super(context);

    this.resources = resources;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
  {
    // TODO: Would be nice to have something that indicates progress
    //       but that would require that functionality from the utilities.
    monitor.beginTask(getActionDescription(), 1);

    ArrayList launchCmd = new ArrayList(resources.length+4);
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("--cwd");
    launchCmd.add(MercurialUtilities.getRepositoryPath(resources[0].getProject()));
    launchCmd.add("status");
    for(int res = 0; res < resources.length; res++)
    {
      // Only add files as Mercurial doesn't control directories.
      if(resources[res].getType() == IResource.FILE)
      {
        launchCmd.add(resources[res].getLocation().toOSString());
      }
    }
    launchCmd.trimToSize();

    try
    {
      this.result = MercurialUtilities.ExecuteCommand((String[])launchCmd.toArray(new String[0]), true);
    } catch (HgException e)
    {
      System.out.println("Mercurial status failed: " + e.getMessage());
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
    return new String("Mercurial get status " + resources[0].getLocation() + " from the Mercurial repository.");    
  }
  
  public String getResult()
  {
    return this.result;
  }
}
