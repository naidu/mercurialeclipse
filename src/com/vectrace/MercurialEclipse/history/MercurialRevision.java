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
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

/**
 * @author zingo
 *
 */
public class MercurialRevision extends FileRevision
{
  IResource resource; 
  ChangeSet changeSet; 
  private String revision;
  private String hash;
  IStorageMercurialRevision iStorageMercurialRevision; //Cached data
  
  public MercurialRevision(ChangeSet changeSet,IResource resource)
  {
    super();
    this.changeSet = changeSet;
    String[] changeSetParts = changeSet.getChangeset().split(":");
	this.revision = changeSetParts[0];
	this.hash = changeSetParts[1];
    this.resource=resource;
  }
  
  public ChangeSet getChangeSet()
  {
    return changeSet;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#getName()
   */
  public String getName()
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialRevision::getName() = " + file.getName());
    return resource.getName();
  }

  @Override
public String getContentIdentifier() 
  {
//    System.out.println("MercurialRevision::getContentIdentifier() = " + changeSet.getChangeset());
    return changeSet.getChangeset();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#getStorage(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
		// System.out.println("MercurialRevision::getStorage()");
		if (iStorageMercurialRevision == null) {			
			iStorageMercurialRevision = new IStorageMercurialRevision(resource,
					revision, hash);
		}
		return iStorageMercurialRevision;
	}

  /*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.team.core.history.IFileRevision#isPropertyMissing()
	 */
  public boolean isPropertyMissing()
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialRevision::isPropertyMissing()");
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#withAllProperties(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialRevision::withAllProperties()");
    return null;
  }



/**
 * @return the revision
 */
public String getRevision() {
	return revision;
}



/**
 * @param revision the revision to set
 */
public void setRevision(String revision) {
	this.revision = revision;
}



/**
 * @return the hash
 */
public String getHash() {
	return hash;
}



/**
 * @param hash the hash to set
 */
public void setHash(String hash) {
	this.hash = hash;
}

}
