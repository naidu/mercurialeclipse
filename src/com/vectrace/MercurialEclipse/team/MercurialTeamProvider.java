/**
 * 
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;

/**
 * @author zingo
 *
 */
public class MercurialTeamProvider extends RepositoryProvider 
{

  
  MercurialFileHistoryProvider FileHistoryProvider; 
	/**
	 * 
	 */
	public MercurialTeamProvider() 
	{
		super();
    FileHistoryProvider = null; //Delay creation until needed new MercurialFileHistoryProvider();
//		System.out.println("MercurialTeamProvider.MercurialTeamProvider()");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.RepositoryProvider#configureProject()
	 */
	//@Override
	public void configureProject() throws CoreException 
	{
		// System.out.println("MercurialTeamProvider.configureProject()");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException 
	{
//		System.out.println("MercurialTeamProvider.deconfigure()");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.RepositoryProvider#getID()
	 */
	//@Override
	public String getID() 
	{
		String ID;
	    ID=getClass().getName();
//		System.out.println("MercurialTeamProvider.getID() ID="+ID+" RepositoryPath=" + getRepositoryPath());
		return ID;
	}

  public IMoveDeleteHook getMoveDeleteHook()
  {
//    System.out.println("MercurialTeamProvider.getMoveDeleteHook()");
    return new HgMoveDeleteHook();
  }

  
  
  /* (non-Javadoc)
   * @see org.eclipse.team.core.RepositoryProvider#getFileHistoryProvider()
   */
  public IFileHistoryProvider getFileHistoryProvider()
  {
    // TODO Auto-generated method stub
    if( FileHistoryProvider == null)
    {
      FileHistoryProvider = new MercurialFileHistoryProvider();
    }
    System.out.println("getFileHistoryProvider()");
    return FileHistoryProvider;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResources()
   */
  public boolean canHandleLinkedResources()
  {
    return true;
  }
}
