/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2007 maj 2
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
  IFile file; 
  ChangeSet changeSet; 
  IStorageMercurialRevision iStorageMercurialRevision; //Cached data
  
  public MercurialRevision(ChangeSet changeSet,IFile resource)
  {
    super();
    this.changeSet = changeSet;
    this.file=resource;
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
    return file.getName();
  }

  public String getContentIdentifier() 
  {
//    System.out.println("MercurialRevision::getContentIdentifier() = " + changeSet.getChangeset());
    return changeSet.getChangeset();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileRevision#getStorage(org.eclipse.core.runtime.IProgressMonitor)
   */
  public IStorage getStorage(IProgressMonitor monitor) throws CoreException
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialRevision::getStorage()");
    if(iStorageMercurialRevision==null)
    {
      IProject proj=file.getProject();
      iStorageMercurialRevision = new IStorageMercurialRevision(proj,file,changeSet.getChangeset());
    }
    return iStorageMercurialRevision;
  }

  /* (non-Javadoc)
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

}
