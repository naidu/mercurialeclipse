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
 *******************************************************************************/

package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class ChangeLog
{
  private Vector<ChangeSet> changeLog=new Vector<ChangeSet>();
//  private Set changeListeners = new HashSet();

  public ChangeLog()
  {
    
  }

  public ChangeLog(IResource changeLog_file)
  {
    ChangeChangeLog(changeLog_file);
  }

  public void ChangeChangeLog(IResource resource)
  {
    //Setup and run command
    File workingDir=MercurialUtilities.getWorkingDir(resource);
    String FullPath = MercurialUtilities.getResourceName(resource);

    ArrayList<String> launchCmd = new ArrayList<String>();

    // log command setup.
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("log");
    launchCmd.add("-v");

    if (resource instanceof IResource && ((IResource) resource).getType() == IResource.FILE) 
    {
      launchCmd.add("--follow");
    }
    launchCmd.add("--");
    if (!(resource instanceof IProject))
    {
      launchCmd.add(FullPath);
    }
    launchCmd.trimToSize();
    String launchCmdStr[] = (String[])launchCmd.toArray(new String[0]);
  
//    System.out.println("log:" + MercurialUtilities.getHGExecutable() + " log -v " + FullPath + " Workingdir:" + workingDir);
    try
    {
      String output = MercurialUtilities.ExecuteCommand(launchCmdStr, workingDir,true);
      if (output != null)
      {
        if (output.length() != 0)
        {
          ChangeChangeLog(output);
        }
      }
    } catch (HgException e)
    {
    	MercurialEclipsePlugin.logError(e);
//      System.out.println(e.getMessage());
    }

  }
  private void ChangeChangeLog(String changeLog)
  {   
    this.changeLog.clear();
    
    if(changeLog==null)
    {
      return;
    }

    int changesetIndex;
    String changeset;
    String tag;
    String user;
    String date;
    String files;
    String description;

    String token;

    
    String eol = System.getProperty("line.separator");
    StringTokenizer st = new StringTokenizer(changeLog, eol);
    
/*
changeset:   39:9224c59c4f17
tag:         tip
user:        zingo
date:        Sun Aug 20 00:07:53 2006 +0200
files:       META-INF/MANIFEST.MF plugin.xml
description:
Fix ticket #17 in a better way


changeset:   37:284d93450d4b
user:        zingo@localhost
date:        Sat Aug 19 23:28:43 2006 +0200
files:       plugin.xml
description:
Team popup only if under hg repository... Fixes ticket #17


changeset:   36:7f30553655f4
user:        zingo@localhost
date:        Sat Aug 19 23:13:15 2006 +0200
files:       plugin.xml
description:
Moved Prefs to team section fixes ticket #16
*/
    if(!st.hasMoreTokens())
    {
      return; //No data at all :(
    }

    token = st.nextToken(eol);

    while(st.hasMoreTokens())
    {

//      System.out.println("token: <" + token + ">");
      if(token.startsWith("changeset:")) 
      {

        int index;
        boolean moredata = true;
        
        changeset=null;
        tag=null;
        user=null;
        date=null;
        files=null;
        description=null;

        index = "changeset:".length();
        
        while(token.charAt(index)==' ') 
        { //remove begining spaces
          index++;
        }
        
        changeset = token.substring(index);
      
        while (st.hasMoreTokens() && moredata) 
        {
          
          token = st.nextToken(eol);
          
          if (token.startsWith("tag:")) 
          {
            index = "tag:".length();
            
              while(token.charAt(index)==' ') 
              { //remove begining spaces
                index++;
              }
              tag = token.substring(index);
              
          } 
          else if (token.startsWith("user:")) 
          {
            index = "user:".length();
            
              while(token.charAt(index)==' ') 
              { //remove begining spaces
                index++;
              }

            user = token.substring(index);
          } 
          else if (token.startsWith("date:")) 
          {
            index = "date:".length();
            
              while(token.charAt(index)==' ') 
              { //remove begining spaces
                index++;
              }
              
              date = token.substring(index);
          } 
          else if (token.startsWith("files:")) 
          {
            index = "files:".length();

              while(token.charAt(index)==' ') 
              { //remove begining spaces
                index++;
              }
              
              files = token.substring(index);
          } 
          else if (token.equals("description:")) 
          {
            
            description = st.nextToken(eol);
            
            while (st.hasMoreTokens() && moredata) {
              token = st.nextToken(eol);
              
              if(token.startsWith("changeset:")) 
              {
                moredata = false;
              } 
              else 
              {
                description = description + " | " + token;
              }

            }

          }

        }

//          System.out.println("changeset:   <" + changeset + ">");
//          System.out.println("tag:         <" + tag + ">");
//          System.out.println("user:        <" + user + ">");
//          System.out.println("date:        <" + date + ">");
//          System.out.println("files:       <" + files + ">");
//          System.out.println("description: <" + description + ">");
          changesetIndex= new Integer(changeset.split(":")[0]);
          this.changeLog.add( new ChangeSet(changesetIndex,changeset,tag,user,date,files,description));
      }
    }        
//    System.out.println("Done!!!");
  }
  
  public Vector<ChangeSet> getChangeLog()
  {
    return changeLog;
  }
  
//  public void removeChangeListener(ChangeLogViewContentProvider viewer) 
//  {
//    changeListeners.remove(viewer);
//  }

  /**
   * @param viewer
   */
//  public void addChangeListener(ChangeLogViewContentProvider viewer) 
//  {
//    changeListeners.add(viewer);
//  }
  
  
}
