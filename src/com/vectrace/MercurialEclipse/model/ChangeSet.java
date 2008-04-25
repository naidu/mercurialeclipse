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

import java.util.ArrayList;
import java.util.List;

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
  private List<HgRevision> parents = new ArrayList<HgRevision>();

  public ChangeSet()
  {
    super();
  }

  public ChangeSet(int changesetIndex, String changeSet, String user, String date)
  {
    this.changesetIndex = changesetIndex;
    this.changeset = changeSet;
    this.user = user;
    this.date = date;
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
  
  public List<HgRevision> getParents()
  {
    return parents;
  }

  public void setChangesetIndex(int changesetIndex)
  {
    this.changesetIndex = changesetIndex;
  }

  public void setChangeset(String changeset)
  {
    this.changeset = changeset;
  }

  public void setTag(String tag)
  {
    this.tag = tag;
  }

  public void setUser(String user)
  {
    this.user = user;
  }

  public void setDate(String date)
  {
    this.date = date;
  }

  public void setFiles(String files)
  {
    this.files = files;
  }

  public void setDescription(String description)
  {
    this.description = description;
  }

  public void addParent(HgRevision parent)
  {
    parents.add(parent);
  }
}
