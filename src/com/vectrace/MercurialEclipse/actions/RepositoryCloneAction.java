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
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/*
 * 
 * This class encapsulates the hg clone command.
 * 
 */
public class RepositoryCloneAction extends HgOperation
{
  private HgRepositoryLocation repo;
  private IWorkspace workspace;
  private String cloneParameters;
  private String projectName;
  private File workingDir;

  /**
   * @param context
   */
  public RepositoryCloneAction(IRunnableContext context, IWorkspace workspace, HgRepositoryLocation repo, String cloneParameters, String projectName, File workingDir)
  {
    super(context);

    this.workspace = workspace;
    this.repo = repo;
    this.cloneParameters = cloneParameters;
    this.projectName = projectName;
    if(workingDir != null)
    {
      this.workingDir = workingDir;
    }
    else
    {
      this.workingDir = workspace.getRoot().getLocation().toFile();
    }
  }

  protected String[] getHgCommand()
  {
    ArrayList<String> launchCmd = new ArrayList<String>();

    // clone command setup.
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("clone");
    if(cloneParameters != null)
    {
      launchCmd.add(cloneParameters);      
    }
    launchCmd.add(repo.getUrl());
    launchCmd.add("--");
    launchCmd.add(projectName);
    launchCmd.trimToSize();
   
    return (String[])launchCmd.toArray(new String[0]);    
  }

  protected File getHgWorkingDir()
  {
    return workingDir;
  }

  /*
   * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
   * 
   * The CloneRepositoryAction is not allowed to run as a job as the CloneRepoWizard will attempt to continue on
   * otherwise. TODO: Figure out how to make the clone repo wizard block. Presumably need a job monitor.
   */
  protected boolean canRunAsJob()
  {
    return false;
  }

  protected String getActionDescription()
  {
	return new String("Mercurial clone repository " + repo.getUrl());
  }
}
