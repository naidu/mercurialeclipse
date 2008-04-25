/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Sebastian Herbszt         - Fix for windows
 *     Stefan G                  - minor updates
 *     Stefan Groschupf          - logError 
 *     Stefan C                  - Code cleanup
 *******************************************************************************/

package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class ChangeLog
{
  private List<ChangeSet> changeLog = new ArrayList<ChangeSet>();

  public ChangeLog(IResource changeLog_file)
  {
    log(changeLog_file);
  }

  private void log(IResource resource)
  {
    //Setup and run command
    File workingDir=MercurialUtilities.getWorkingDir(resource);
    String FullPath = MercurialUtilities.getResourceName(resource);

    ArrayList<String> launchCmd = new ArrayList<String>();

    // log command setup.
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("log");
    launchCmd.add("-v");

    if (resource.getType() == IResource.FILE) 
    {
      launchCmd.add("--follow");
    }
    launchCmd.add("--");
    if (!(resource instanceof IProject))
    {
      launchCmd.add(FullPath);
    }
    launchCmd.trimToSize();
    String launchCmdStr[] = launchCmd.toArray(new String[0]);
  
    try
    {
      String output = MercurialUtilities.ExecuteCommand(launchCmdStr, workingDir,true);
      if (output != null && output.length() != 0)
      {
        changeLog = createFromStdOut(output);
      }
    } catch (HgException e)
    {
    	MercurialEclipsePlugin.logError(e);
    }
  }

  protected static List<ChangeSet> createFromStdOut(String input)
  {   
    List<ChangeSet> changeLog = new ArrayList<ChangeSet>();
    
    if(input==null)
    {
      return changeLog;
    }

    String token;
    String eol = System.getProperty("line.separator");
    StringTokenizer st = new StringTokenizer(input, eol);

    if(!st.hasMoreTokens())
    {
      return changeLog; //No data at all :(
    }

    token = st.nextToken(eol);

    while(st.hasMoreTokens())
    {
      if(token.startsWith("changeset:")) 
      {
        boolean moredata = true;

        ChangeSet change = new ChangeSet();
        String changeset = trim(token, "changeset:");
        change.setChangeset(changeset);
        change.setChangesetIndex(new Integer(changeset.split(":")[0]));
        
        while (st.hasMoreTokens() && moredata) 
        {
          token = st.nextToken(eol);
          
          if (token.startsWith("tag:")) 
          {
            change.setTag(trim(token, "tag:"));
          } 
          else if (token.startsWith("user:")) 
          {
            change.setUser(trim(token, "user:"));
          } 
          else if (token.startsWith("date:")) 
          {
            change.setDate(trim(token, "date:"));
          } 
          else if (token.startsWith("files:")) 
          {
            change.setFiles(trim(token, "files:"));
          }
          else if (token.startsWith("parent:")) 
          {
            change.addParent(HgRevision.parse(trim(token, "parent:")));
          }
          else if (token.equals("description:")) 
          {
            StringBuilder description = new StringBuilder(st.nextToken(eol));
            
            while (st.hasMoreTokens() && moredata)
            {
              token = st.nextToken(eol);  
              if(token.startsWith("changeset:")) 
              {
                moredata = false;
              } 
              else 
              {
                description.append(" | ").append(token);
              }
            }
            change.setDescription(description.toString());
          }
        }
        changeLog.add(change);
      }
    }
    return changeLog;
  }

  private static String trim(String token, String string)
  {
    int index = string.length();
    while(token.charAt(index)==' ') 
    { 
      index++;
    }
    return token.substring(index);
  }
  
  public List<ChangeSet> getChangeLog()
  {
    return changeLog;
  }
}
