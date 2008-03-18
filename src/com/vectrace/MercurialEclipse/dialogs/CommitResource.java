/**
 * StefanC
 */
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
      statusToken = statusToken.trim();
      if (statusToken.startsWith("M")) 
      {
        // System.out.println("FILE_MODIFIED:path <" +
        // statusToken.toString() + ">");
        return CommitDialog.FILE_MODIFIED;
      } 
      else if (statusToken.startsWith("A")) 
      {
        return CommitDialog.FILE_ADDED;
      } 
      else if (statusToken.startsWith("R")) 
      {
        return CommitDialog.FILE_REMOVED;
      } 
      else if (statusToken.startsWith("?")) 
      {
        return CommitDialog.FILE_UNTRACKED;
      }
      else if (statusToken.startsWith("!")) 
      {
        // System.out.println("FILE_DELETED:path <" +
        // statusToken.toString() + ">");
        return CommitDialog.FILE_DELETED;
      }
      else 
      {
        return "status error: " + statusToken.toString();
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