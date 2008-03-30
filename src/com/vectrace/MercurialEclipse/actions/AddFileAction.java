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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class AddFileAction extends HgOperation
{
  private String resource;
//  private IProject project;
  private File workingDir;

  /**
   * @param context
   */
  public AddFileAction(IRunnableContext context, IProject project, String resource, File workingDir)
  {
    super(context);

//    this.project = project;
    this.resource = resource;
    this.workingDir = workingDir;
  }

  @Override
protected String[] getHgCommand()
  {

    final String launchCmd[] =
    {  MercurialUtilities.getHGExecutable(), 
        "add",
        "--",
        resource  };

    return launchCmd;
  }
  
  @Override
protected File getHgWorkingDir()
  {
    return workingDir;
  }
  
  @Override
protected String getActionDescription()
  {
    return new String("Mercurial add resource " + resource + " from the Mercurial repository");    
  }
}
