/*******************************************************************************

 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - some updates
 *     Bastian Doetsch           - new qualified name for project sets
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
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
   
    /**
     * Qualified Name for the repository a project was cloned from.
     */
	public static final QualifiedName QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY = new QualifiedName(
			ID + ".projectSourceRepository",
			"MercurialEclipseProjectSourceRepository");

	public static final QualifiedName QUALIFIED_NAME_DEFAULT_REVISION_LIMIT = new QualifiedName(
			ID + ".defaultRevisionLimit",
			"defaultRevisionLimit");
	
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
	@Override
    public void configureProject() throws CoreException 
	{
		getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource resource = getProject().getFolder(".hg");
		if (resource != null && resource.exists()) {
		    resource.setTeamPrivateMember(true);
		}
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
	@Override
    public String getID() 
	{
		return ID;
	}

  @Override
public IMoveDeleteHook getMoveDeleteHook()
  {
//    System.out.println("MercurialTeamProvider.getMoveDeleteHook()");
    return new HgMoveDeleteHook();
  }

  
  
  /* (non-Javadoc)
   * @see org.eclipse.team.core.RepositoryProvider#getFileHistoryProvider()
   */
  @Override
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
  @Override
public boolean canHandleLinkedResources()
  {
    return true;
  }
}
