/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2007 maj 2
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;

/**
 * @author zingo
 *
 */
public class MercurialFileHistory extends FileHistory
{

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getContributors(org.eclipse.team.core.history.IFileRevision)
   */
  public IFileRevision[] getContributors(IFileRevision revision)
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialFileHistory::getContributors()");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevision(java.lang.String)
   */
  public IFileRevision getFileRevision(String id)
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialFileHistory::getFileRevision(" + id + ")");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevisions()
   */
  public IFileRevision[] getFileRevisions()
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialFileHistory::getFileRevisions()");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getTargets(org.eclipse.team.core.history.IFileRevision)
   */
  public IFileRevision[] getTargets(IFileRevision revision)
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialFileHistory::getTargets()");
    return null;
  }

}
