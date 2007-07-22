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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 *
 */
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

  protected String[] getHgCommand()
  {

    final String launchCmd[] =
    {  MercurialUtilities.getHGExecutable(), 
        "add",
        resource  };

    return launchCmd;
  }
  
  protected File getHgWorkingDir()
  {
    return workingDir;
  }
  
  protected String getActionDescription()
  {
    return new String("Mercurial add resource " + resource + " from the Mercurial repository");    
  }
}
