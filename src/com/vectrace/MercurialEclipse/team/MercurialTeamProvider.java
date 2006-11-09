/**
 * 
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author zingo
 *
 */
public class MercurialTeamProvider extends RepositoryProvider 
{
	private static final QualifiedName RepositoryQualifier = new QualifiedName(MercurialEclipsePlugin.getDefault().getBundle().getSymbolicName(), "RepositoryPath");
	String RepositoryPath;
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
//		System.out.println("MercurialTeamProvider.configureProject()");
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
	/*
	 * 
	 */
	public void setRepositoryPath(String repo)
	{
		RepositoryPath = repo;
		IProject proj = getProject();
//		System.out.println("MercurialTeamProvider.setRepositoryPath() ->" + repo);
		if(proj!=null)
		{
			try 
			{
				proj.setPersistentProperty(RepositoryQualifier, repo);
			}
			catch (CoreException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	public String getRepositoryPath()
	{
		if(RepositoryPath==null)
		{
			IProject proj = getProject();
			if(proj!=null)
			{
				try 
				{
					RepositoryPath=proj.getPersistentProperty(RepositoryQualifier);
				}
				catch (CoreException e) 
				{
					e.printStackTrace();
				}
				if(RepositoryPath==null)
				{
					//We had no Reposetory Path in the property setting
					// Something have been messing with the setting
					// No problem we try to find it.
//					System.out.println("MercurialTeamProvider.getRepositoryPath() proj.getPersistentProperty() faild");

					RepositoryPath=MercurialUtilities.search4MercurialRoot(proj);
//					System.out.println("MercurialTeamProvider.getRepositoryPath() Serching for repository:" + RepositoryPath);
					if(RepositoryPath!=null)
					{
						//Write the found repository path into the property setting.
//						System.out.println("MercurialTeamProvider.getRepositoryPath() Writing repositorypath into PersitantProperty() :" + RepositoryPath);
						setRepositoryPath(RepositoryPath);
					}
				}
			}
		}
//		System.out.println("MercurialTeamProvider.getRepositoryPath() ->" + RepositoryPath);
		
		return RepositoryPath;
	}

}
