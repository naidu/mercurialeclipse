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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;


/**
 * 
 * Action to delete files using Mercurial.
 */
public class DeleteFileAction extends HgOperation
{
  private IResource resource;


  /**
   * IResource should only be a file or folder as operations on a folder are implicitly on all
   * subtending files in Mercurial.
   */
  public DeleteFileAction(IRunnableContext context, IResource resource)
  {
    super(context);

    this.resource = resource;  
  }

  @Override
protected String[] getHgCommand()
  {
    // TODO: Should we consider making  --force optional?
    final String launchCmd[] =
    { 
      MercurialUtilities.getHGExecutable(),
      "remove",
      "--force",  /* Remove even if changed */
//      "--after",  /* Do not do the rename just record it Eclipse will do it */
      "--",
      resource.getLocation().toOSString() 
    };

    return launchCmd;
  }

  @Override
protected File getHgWorkingDir()
  {
//    return (resource.getLocation()).toFile();
    return MercurialUtilities.getWorkingDir(resource);
  }

  
  @Override
protected String getActionDescription()
  {
    return new String("Mercurial delete resource " + resource.getLocation() + " from the Mercurial repository");    
  }
}
