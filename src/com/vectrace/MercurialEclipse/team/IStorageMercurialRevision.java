/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.IdentifyAction;

/**
 * @author zingo
 *
 * This is a IStorage subclass that handle file revision
 *  
 */
public class IStorageMercurialRevision implements IStorage
{
  String revision;
//  IProject project;
  IResource resource;

  /**
   * 
   */
  
  public IStorageMercurialRevision(IResource res, String rev)
  {
    super();
//    project = proj;
    resource = res;
    revision=rev;
  }

  public IStorageMercurialRevision(IResource res, int rev)
  {
    super();
//    project = proj;
    resource = res;
    revision=String.valueOf(rev);   
  }

  public IStorageMercurialRevision(IResource res)
  {
    super();
//    project = proj;
    resource = res;
    revision=null;  //should be fetched from id    
    
    File workingDir=MercurialUtilities.getWorkingDir( res );
    IdentifyAction identifyAction = new IdentifyAction(null, res.getProject(), workingDir);
    try
    {
      identifyAction.run();
      revision = identifyAction.getChangeset(); 
    }
    catch (Exception e)
    {
      MercurialEclipsePlugin.logError("pull operation failed", e);
//      System.out.println("pull operation failed");
//      System.out.println(e.getMessage());
      
      IWorkbench workbench = PlatformUI.getWorkbench();
      Shell shell = workbench.getActiveWorkbenchWindow().getShell();
      MessageDialog.openInformation(shell,"Mercurial Eclipse couldn't identify hg revision of \n" + res.getName().toString() + "\nusing tip",  identifyAction.getResult());
      revision = "tip";
    }

    
  }

  
  public IStorageMercurialRevision(IResource res, int rev, int depth)
  {
    super();
//    project = proj;
    resource = res;
    revision=String.valueOf(rev);   
  }

  
  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter)
  {
//    System.out.println("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::getAdapter()" );
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#getContents()
   * 
   * generate data content of the so called "file" in this case a revision,
   * e.g. a hg cat --rev "rev" <file>
   * 
   */
  public InputStream getContents() throws CoreException
  {

    //Setup and run command
	String[] launchCmd;
	if(revision!=null)
	{
    
      String rev=revision;
      /*convert <rev number>:<hash> to <hash>*/
      int separator=rev.indexOf(':');
      if(separator!=-1)
      {
        rev=rev.substring(separator+1);
      }
      launchCmd = new String[] { MercurialUtilities.getHGExecutable(),
                             "cat", 
                             "--rev", 
                             rev,
                             "--",
                             MercurialUtilities.getResourceName(resource) 
                             };
	} else {
	      launchCmd = new String[] { MercurialUtilities.getHGExecutable(),
                  "cat", 
                  "--",
                  MercurialUtilities.getResourceName(resource) 
                  };
	}
    File workingDir=MercurialUtilities.getWorkingDir(resource);
      
    /* TODO using MercurialUtilities.ExecuteCommandToInputStream looks buggy as hell
     * and fail to diff files that are not really small (deadlock?) 
     * (see the javadoc of java.lang.Process for a possible explanation) 
     */
    ByteArrayOutputStream resultStream = MercurialUtilities.ExecuteCommandToByteArrayOutputStream(launchCmd,workingDir,true);
    return new ByteArrayInputStream(resultStream.toByteArray());
  }

  /* (non-Javadoc)setContents(
   * @see org.eclipse.core.resources.IStorage#getFullPath()
   */
  public IPath getFullPath()
  {
//    System.out.println("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::getFullPath()" );
    return resource.getFullPath().append(revision!=null?(" ["+revision+"]"):" [parent changeset]");
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#getName()
   */
  public String getName()
  {
//    System.out.print("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::getName()" );
    String name;
    if(revision!=null)
    {
      name = "[" + revision + "]" + resource.getName();
    }
    else
    {
      name = resource.getName();
    }
//    System.out.println("=" + name );
    
    return  name;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#isReadOnly()
   * 
   * You can't write to other revisions then the current selected e.g. ReadOnly
   * 
   */
  public boolean isReadOnly()
  {
//    System.out.println("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::isReadOnly()" );
    if(revision!=null)
    {
      return true;
    }
    else
    {
      // if no revision resource is the current one e.g. editable :)
      ResourceAttributes attributes = resource.getResourceAttributes();
      if (attributes != null) 
      {
        return attributes.isReadOnly();
      }
    }
    return true;  /* unknown state marked as read only for safety */
  }

  
}
