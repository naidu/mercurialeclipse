/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2007 maj 2
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.history;

import java.util.Vector;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import com.vectrace.MercurialEclipse.model.ChangeLog;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author zingo
 *
 */
public class MercurialHistory extends FileHistory
{
  private IFile file;
  protected IFileRevision[] revisions;
  ChangeLog changeLog;


  public MercurialHistory(IFile file)
  {
    super();
    this.file = file;
  }
  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getContributors(org.eclipse.team.core.history.IFileRevision)
   */
  public IFileRevision[] getContributors(IFileRevision revision)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistory::getContributors()");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevision(java.lang.String)
   */
  public IFileRevision getFileRevision(String id)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistory::getFileRevision(" + id + ")");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevisions()
   */
  public IFileRevision[] getFileRevisions()
  {
//    System.out.println("MercurialHistory::getFileRevisions()");
    return revisions;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getTargets(org.eclipse.team.core.history.IFileRevision)
   */
  public IFileRevision[] getTargets(IFileRevision revision)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistory::getTargets()");
    return null;
  }

  public void refresh(IProgressMonitor monitor) throws CoreException 
  {
//    System.out.println("MercurialHistory::refresh() (home made)");
    RepositoryProvider provider = RepositoryProvider.getProvider(file.getProject());
    if (provider != null && provider instanceof MercurialTeamProvider) 
    {
      
      changeLog = new ChangeLog(file);
      
//      changeLog.ChangeChangeLog(in_resource);
      Vector<ChangeSet> chageSets = changeLog.getChangeLog();
      
      
      revisions = new IFileRevision[chageSets.size()];
      for(int i=0;i<chageSets.size();i++)
      {
        revisions[i]=new MercurialRevision(chageSets.get(i),file);
      }
    }
  } 
  
}
