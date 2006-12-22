package com.vectrace.MercurialEclipse.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class CommitAction extends TeamOperation
{
  private String[] files;
  private String   commitMessage;
  private IProject project;

  public CommitAction(IRunnableContext context, IProject project, String[] files, String commitMessage)
  {
    super(context);
    this.files = files;
    this.commitMessage = commitMessage;
    this.project = project;
  }
 
  /* (non-Javadoc)
   * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
  {
    // TODO: Would be nice to have something that indicates progress
    //       but that would require that functionality from the utilities.
    monitor.beginTask("Mercurial clone operation", 1);

    ArrayList launchCmd = new ArrayList();
      
    // Shell command setup.
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("--cwd");
//    launchCmd.add("\"" + project.getLocation().toOSString() + "\"");
    launchCmd.add( project.getLocation().toOSString() );
    launchCmd.add("commit");
    
    // Commit message
    launchCmd.add("--message");
//    launchCmd.add("\"" + commitMessage + "\"");
    launchCmd.add(commitMessage);

    // User name
    launchCmd.add("--user");
    launchCmd.add(MercurialUtilities.getHGUsername());
    
    // All the files.
    for(int file=0; file < files.length; file++)
    {
//      launchCmd.add("\"" + files[file] + "\"");
      launchCmd.add(files[file]);
    }
    
    launchCmd.trimToSize();
    
    try
    {
      MercurialUtilities.ExecuteCommand((String[])launchCmd.toArray(new String[0]), true);
    } catch (HgException e)
    {
      System.out.println(e.getMessage());
    }

    monitor.done();
  }

}
