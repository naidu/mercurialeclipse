/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;

import org.eclipse.core.resources.IResource;

public class CommitResource 
  {
    private String status;
    private File path;
    private IResource resource;
    private String convertStatus(String statusToken) 
    {
      if (statusToken.startsWith("M"))  //$NON-NLS-1$
      {
        // System.out.println("FILE_MODIFIED:path <" +
        // statusToken.toString() + ">");
        return CommitDialog.FILE_MODIFIED;
      } 
      else if (statusToken.startsWith("A"))  //$NON-NLS-1$
      {
        return CommitDialog.FILE_ADDED;
      } 
      else if (statusToken.startsWith("R"))  //$NON-NLS-1$
      {
        return CommitDialog.FILE_REMOVED;
      } 
      else if (statusToken.startsWith("?"))  //$NON-NLS-1$
      {
        return CommitDialog.FILE_UNTRACKED;
      }
      else if (statusToken.startsWith("!"))  //$NON-NLS-1$
      {
        // System.out.println("FILE_DELETED:path <" +
        // statusToken.toString() + ">");
        return CommitDialog.FILE_DELETED;
      }
      else 
      {
        return "status error: " + statusToken; //$NON-NLS-1$
      }
    }

    public CommitResource(String status, IResource resource, File path) 
    {
      this.status = convertStatus(status);
      this.resource = resource;
      this.path = path;
    }

    public String getStatus() 
    {
      return status;
    }

    public IResource getResource() 
    {
      return resource;
    }

    public File getPath() 
    {
      return path;
    }
  }