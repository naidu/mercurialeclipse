/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
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
  private IProject project;
  private File workingDir;

  public CommitAction(IRunnableContext context, IProject project, String[] files, String commitMessage,File workingDir)
  {
    super(context);
    this.files = files;
    this.commitMessage = commitMessage;
    this.project = project;
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
    this.project = project;
    if(workingDir != null)
    {
      this.workingDir = workingDir;
    }
    else
    {
      this.workingDir = project.getLocation().toFile();
    }
  }

  
  protected String[] getHgCommand()
  {   
    ArrayList launchCmd = new ArrayList();

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
    
    // All the files.
    for(int file=0; file < files.length; file++)
    {
//      launchCmd.add("\"" + files[file] + "\"");
      launchCmd.add(files[file]);
    }
    launchCmd.trimToSize();
   
    return (String[])launchCmd.toArray(new String[0]);
  }

  protected File getHgWorkingDir()
  {
    return workingDir;
  }

  
  protected String getActionDescription()
  {
    return new String("Mercurial commit files");
  }
}
