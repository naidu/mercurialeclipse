/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

public class HgRevision
{
  public static final HgRevision TIP = new HgRevision("tip");
  private final String changeset;
  private final long revision;

  protected HgRevision(String changeset) {
    this(changeset, -1);
  }
  
  public HgRevision(String changeset, long revision)
  {
    this.changeset = changeset;
    this.revision = revision;
  }
  
  public String getChangeset()
  {
    return changeset;
  }
  
  public long getRevision()
  {
    return revision;
  }
  
  @Override
  public boolean equals(Object obj)
  {
    HgRevision r = (HgRevision)obj;
    return r.revision == revision || r.changeset.equals(changeset);
  }
  
  @Override
  public String toString()
  {
    return revision + ":" + changeset;
  }
}
