package com.vectrace.MercurialEclipse.model;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class ChangeLog
{
  private Vector<ChangeSet> changeLog=new Vector<ChangeSet>();
//  private Set changeListeners = new HashSet();
  
  public ChangeLog(String changeLog)
  {
    super();
    
    if(changeLog==null)
    {
      return;
    }

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
          this.changeLog.add( new ChangeSet(changeset,tag,user,date,files,description));
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
