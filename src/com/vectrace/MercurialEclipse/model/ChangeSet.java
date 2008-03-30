/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Charles O'Farrell         - HgRevision
 *******************************************************************************/

package com.vectrace.MercurialEclipse.model;

import com.vectrace.MercurialEclipse.HgRevision;

public class ChangeSet
{
  private int changesetIndex;
  private String changeset;
  private String tag;
  private String user;
  private String date;
  private String files;
  private String description;
  
  public ChangeSet(int changesetIndex,String changeSet,String tag,String user, String date, String files, String description)
  {
    this.changesetIndex=changesetIndex;
    this.changeset=changeSet;
    this.tag = tag;
    this.user = user;
    this.date = date;
    this.files = files;
    this.description = description;
  }

  public ChangeSet(int changesetIndex,String changeSet,String user, String date)
  {
    this(changesetIndex,changeSet,null,user, date, null, null);
  }

  public int getChangesetIndex()
  {
    return changesetIndex;
  }

  public String getChangeset()
  {
    return changeset;
  }

  public String getTag()
  {
    return tag;
  }

  public String getUser()
  {
    return user;
  }

  public String getDate()
  {
    return date;
  }

  public String getFiles()
  {
    return files;
  }

  public String getDescription()
  {
    return description;
  }

  public HgRevision getRevision()
  {
    return new HgRevision(changeset, changesetIndex);
  }

}
