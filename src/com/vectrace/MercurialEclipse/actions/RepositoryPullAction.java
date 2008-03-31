/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - Implementation
 *     Stefan C                  - Code cleanup
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
  private File workingDir;

  public RepositoryPullAction(IRunnableContext context, IProject project, HgRepositoryLocation repo,File workingDir)
  {
    super(context);
    this.repo = repo;
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
    launchCmd.add("pull");
    launchCmd.add("--");
    launchCmd.add(repo.getUrl());
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
    return new String("Mercurial pull changes from other reposetory");
  }
}
