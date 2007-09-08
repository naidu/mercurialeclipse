/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2007 maj 2
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;

import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;


/**
 * @author zingo
 *
 */

public class MercurialFileHistoryProvider extends FileHistoryProvider
{
  
  public MercurialFileHistoryProvider()
  {
    super();
//    System.out.println("MercurialFileHistoryProvider::MercurialFileHistoryProvider()");
  }

  public IFileHistory getFileHistoryFor(IResource resource, int flags, IProgressMonitor monitor)
  {
    System.out.println("MercurialFileHistoryProvider::getFileHistoryFor(" + resource.toString() + ")");
    if (resource instanceof IResource && ((IResource) resource).getType() == IResource.FILE) 
    {
      RepositoryProvider provider = RepositoryProvider.getProvider(((IFile) resource).getProject());
      if (provider instanceof MercurialTeamProvider)
      {
        return new MercurialFileHistory((IFile)resource);
      }
    }
    return null;
  }
   
  public IFileHistory getFileHistoryFor(IFileStore store, int flags, IProgressMonitor monitor)
  {
    System.out.println("MercurialFileHistoryProvider::getFileHistoryFor(" + store.toString() + ")");
    return null; //new MercurialFileHistory();
  }

  public IFileRevision getWorkspaceFileRevision(IResource resource)
  {
    System.out.println("MercurialFileHistoryProvider::getWorkspaceFileRevision(" + resource.toString() + ")");
    return null;
  }

}
