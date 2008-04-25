/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError 
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

/**
 * @author zingo
 *
 */
public class MercurialRevision extends FileRevision
{
  private IResource resource; 
  private ChangeSet changeSet; 
  private IStorageMercurialRevision iStorageMercurialRevision; //Cached data
  private final GChangeSet gChangeSet;

  public MercurialRevision(ChangeSet changeSet, GChangeSet gChangeSet, IResource resource)
  {
    super();
    this.changeSet = changeSet;
    this.gChangeSet = gChangeSet;
    this.resource=resource;
  }

  public ChangeSet getChangeSet()
  {
    return changeSet;
  }

  public GChangeSet getGChangeSet()
  {
    return gChangeSet;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#getName()
   */
  public String getName()
  {
    return resource.getName();
  }

  @Override
  public String getContentIdentifier() 
  {
    return changeSet.getChangeset();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#getStorage(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IStorage getStorage(IProgressMonitor monitor) throws CoreException
  {
    if(iStorageMercurialRevision==null)
    {
      iStorageMercurialRevision = new IStorageMercurialRevision(resource,changeSet.getChangeset());
    }
    return iStorageMercurialRevision;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#isPropertyMissing()
   */
  public boolean isPropertyMissing()
  {
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#withAllProperties(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException
  {
    return null;
  }

}
