/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2007 maj 2
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistoryProvider;

/**
 * @author zingo
 *
 */

public class MercurialFileHistoryProvider extends FileHistoryProvider
{
  
  public MercurialFileHistoryProvider()
  {
    super();
    System.out.println("MercurialFileHistoryProvider::MercurialFileHistoryProvider()");
    // TODO Auto-generated constructor stub
  }

  public IFileHistory getFileHistoryFor(IResource resource, int flags, IProgressMonitor monitor)
  {
    System.out.println("MercurialFileHistoryProvider::getFileHistoryFor(" + resource.toString() + ")");
    return new MercurialFileHistory();
  }
   
  public IFileRevision getWorkspaceFileRevision(IResource resource)
  {
    System.out.println("MercurialFileHistoryProvider::getWorkspaceFileRevision(" + resource.toString() + ")");
    return new MercurialFileRevision();
  }
  public IFileHistory getFileHistoryFor(IFileStore store, int flags, IProgressMonitor monitor)
  {
    System.out.println("MercurialFileHistoryProvider::getFileHistoryFor(" + store.toString() + ")");
    return new MercurialFileHistory();
  }
  
}
