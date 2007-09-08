/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2007 maj 2
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;

import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * @author zingo
 *
 */
public class MercurialFileRevision extends FileRevision
{
  
  ChangeSet changeSet; 
  
  public MercurialFileRevision(ChangeSet changeSet)
  {
    super();
    this.changeSet = changeSet;
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
    System.out.println("MercurialFileRevision::getName()");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#getStorage(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IStorage getStorage(IProgressMonitor monitor) throws CoreException
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialFileRevision::getStorage()");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#isPropertyMissing()
   */
  public boolean isPropertyMissing()
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialFileRevision::isPropertyMissing()");
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#withAllProperties(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialFileRevision::withAllProperties()");
    return null;
  }

}
