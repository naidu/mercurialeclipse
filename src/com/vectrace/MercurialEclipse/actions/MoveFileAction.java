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
 * Action to move files using Mercurial.
 */
public class MoveFileAction extends HgOperation
{
  private IResource src;
  private IResource dest;

  public MoveFileAction(IRunnableContext context, IResource src, IResource dest)
  {
    super(context);
//    System.out.println("new MoveFileAction");

    this.src = src;
    this.dest = dest;
  }

  protected String[] getHgCommand()
  {
    final String launchCmd[] =
    { 
      MercurialUtilities.getHGExecutable(),
      "rename",
      "--force",  /* Rename even if changed */
//      "--after",  /* Do not do the rename just record it Eclipse will do it */
      "--",
      src.getLocation().toOSString(), dest.getLocation().toOSString() 
    };

    return launchCmd;
  }

  protected File getHgWorkingDir()
  {
//  return (((IResource) src).getLocation()).toFile();
    return MercurialUtilities.getWorkingDir(src);
  }

  
  protected String getActionDescription()
  {
    return new String("Move " + src.getLocation() + " to " + dest.getLocation());
  }  
}
