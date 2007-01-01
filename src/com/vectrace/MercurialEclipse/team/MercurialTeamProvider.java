/**
 * 
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

/**
 * @author zingo
 *
 */
public class MercurialTeamProvider extends RepositoryProvider 
{

	/**
	 * 
	 */
	public MercurialTeamProvider() 
	{
		super();
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
   * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResources()
   */
  public boolean canHandleLinkedResources()
  {
    return true;
  }
}
