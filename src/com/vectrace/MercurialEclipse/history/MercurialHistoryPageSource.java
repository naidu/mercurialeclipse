/**
 * 
 */
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.ui.history.HistoryPageSource;
import org.eclipse.ui.part.Page;

import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author zingo
 *
 */
public class MercurialHistoryPageSource extends HistoryPageSource
{
   MercurialHistoryProvider fileHistoryProvider;
   
  public MercurialHistoryPageSource(MercurialHistoryProvider fileHistoryProvider)
  {
    super();
//    System.out.println("MercurialHistoryPageSource::MercurialHistoryPageSource()");
    this.fileHistoryProvider = fileHistoryProvider;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPageSource#canShowHistoryFor(java.lang.Object)
   */
  public boolean canShowHistoryFor(Object object)
  {
    // TODO Check if the file is in a Mercurial repository
//    System.out.println("MercurialHistoryPageSource::canShowHistoryFor( " +object.toString() + ")");

    if (object instanceof IResource && ((IResource) object).getType() == IResource.FILE) 
    {
      RepositoryProvider provider = RepositoryProvider.getProvider(((IFile) object).getProject());
      if (provider instanceof MercurialTeamProvider)
      {
        return true;
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPageSource#createPage(java.lang.Object)
   */
  public Page createPage(Object object)
  {
//    System.out.println("MercurialHistoryPageSource::createPage()");
    if (object instanceof IResource && ((IResource) object).getType() == IResource.FILE) 
    {
      return new MercurialHistoryPage((IResource) object);
    }
    return null;
  }

}
