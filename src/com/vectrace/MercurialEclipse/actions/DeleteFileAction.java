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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;


/**
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
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

  protected String[] getHgCommand()
  {
    // TODO: Should we consider making  --force optional?
    final String launchCmd[] =
    { 
      MercurialUtilities.getHGExecutable(),
      "remove",
      "--force",
      resource.getLocation().toOSString() 
    };

    return launchCmd;
  }

  protected File getHgWorkingDir()
  {
//    return (resource.getLocation()).toFile();
    return MercurialUtilities.getWorkingDir(resource);
  }

  
  protected String getActionDescription()
  {
    return new String("Mercurial delete resource " + resource.getLocation() + " from the Mercurial repository");    
  }
}
