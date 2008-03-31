/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class CommitAction extends HgOperation
{
  private String[] files;
  private String   commitMessage;
  private File workingDir;

  public CommitAction(IRunnableContext context, IProject project, String[] files, String commitMessage,File workingDir)
  {
    super(context);
    this.files = files;
    this.commitMessage = commitMessage;
    if(workingDir != null)
    {
      this.workingDir = workingDir;
    }
    else
    {
      this.workingDir = project.getLocation().toFile();
    }
  }

  public CommitAction(IRunnableContext context, IProject project, String files, String commitMessage,File workingDir)
  {
    super(context);

    this.files = new String[1];
    this.files[0] = files;
    this.commitMessage = commitMessage;
    if(workingDir != null)
    {
      this.workingDir = workingDir;
    }
    else
    {
      this.workingDir = project.getLocation().toFile();
    }
  }

  
  @Override
protected String[] getHgCommand()
  {   
    ArrayList<String> launchCmd = new ArrayList<String>();

    // Shell command setup.
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("commit");

    // Commit message
    launchCmd.add("--message");
//    launchCmd.add("\"" + commitMessage + "\"");
    launchCmd.add(commitMessage);

    // User name
    launchCmd.add("--user");
    launchCmd.add(MercurialUtilities.getHGUsername());

    launchCmd.add("--");
    
    // All the files.
    for(int file=0; file < files.length; file++)
    {
//      launchCmd.add("\"" + files[file] + "\"");
      launchCmd.add(files[file]);
    }
    launchCmd.trimToSize();
   
    return launchCmd.toArray(new String[0]);
  }

  @Override
protected File getHgWorkingDir()
  {
    return workingDir;
  }

  
  @Override
protected String getActionDescription()
  {
    return new String("Mercurial commit files");
  }
}
