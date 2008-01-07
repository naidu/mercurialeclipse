/*******************************************************************************
 * Copyright (c) 2007 Zingo Andersen
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

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class RepositoryPullAction extends HgOperation
{
  private HgRepositoryLocation repo;
  private IProject project;
  private File workingDir;

  public RepositoryPullAction(IRunnableContext context, IProject project, HgRepositoryLocation repo,File workingDir)
  {
    super(context);
    this.repo = repo;
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
    launchCmd.add("pull");
    launchCmd.add(repo.getUrl());
    launchCmd.trimToSize();
   
    return (String[])launchCmd.toArray(new String[0]);
  }

  protected File getHgWorkingDir()
  {
    return workingDir;
  }

  
  protected String getActionDescription()
  {
    return new String("Mercurial pull changes from other reposetory");
  }
}
