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
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author Peter
 * 
 * Mercurial status operation.
 *
 */
public class StatusContainerAction extends HgOperation
{

  private IResource[] resources;

  /**
   * @param context
   */
  public StatusContainerAction(IRunnableContext context, IResource[] resources)
  {
    super(context);

    this.resources = resources;
  }

  @Override
protected String[] getHgCommand()
  {
    ArrayList<String> launchCmd = new ArrayList<String>(resources.length + 4);
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("status");
    launchCmd.add("--");
    if( resources.length == 0 )
    {
//    	System.out.println("StatusContainerAction::getHgCommand() resources.length == 0");
    }
    for(int res = 0; res < resources.length; res++)
    {
      // Mercurial doesn't control directories or projects and so will just return that they're
      // untracked.
      launchCmd.add(resources[res].getLocation().toOSString());
    }
    launchCmd.trimToSize();
    
    return launchCmd.toArray(new String[0]);
  }

  @Override
protected File getHgWorkingDir()
  {
    return MercurialUtilities.getWorkingDir(resources[0]);
  }

  
  @Override
protected String getActionDescription()
  {
    return new String("Mercurial get status " + resources[0].getLocation() + " from the Mercurial repository.");
  }

  public File getWorkingDir()
  {
    return getHgWorkingDir();
  }

  
}
