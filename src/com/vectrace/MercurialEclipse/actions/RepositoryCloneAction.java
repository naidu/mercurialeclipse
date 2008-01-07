/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Edited by Zingo Andersen
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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/*
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
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
    ArrayList launchCmd = new ArrayList();

    // clone command setup.
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("clone");
    if(cloneParameters != null)
    {
      launchCmd.add(cloneParameters);      
    }
    launchCmd.add(repo.getUrl());
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
