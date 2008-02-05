/*******************************************************************************
 * Copyright (c) 2008 Vectrace (Zingo Andersen) 
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;

import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;

/**
 * @author zingo
 *
 */
public class MercurialTeamProvider extends RepositoryProvider 
{

   public static final String ID = "com.vectrace.MercurialEclipse.team.MercurialTeamProvider";
  MercurialHistoryProvider FileHistoryProvider; 
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
    if( FileHistoryProvider == null)
    {
      FileHistoryProvider = new MercurialHistoryProvider();
    }
//    System.out.println("getFileHistoryProvider()");
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
