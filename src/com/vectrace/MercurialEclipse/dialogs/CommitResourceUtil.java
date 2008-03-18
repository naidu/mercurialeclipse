package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.Team;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.StatusContainerAction;

public final class CommitResourceUtil
{

	private final IProject project;

	public CommitResourceUtil(IProject project)
	{
		this.project = project;
	}

	public CommitResource[] getCommitResources(IResource[] inResources)
	{
		// Get the path to the project go we can get everything underneath
		// that has changed. Once we get that, filter on the appropriate
		// items.
		// IResource[] projectArray = {project};
		// StatusContainerAction statusAction = new StatusContainerAction(null,
		// projectArray);
		StatusContainerAction statusAction = new StatusContainerAction(null,inResources);
		File workingDir = statusAction.getWorkingDir();
		try
		{
			statusAction.run();
			String result = statusAction.getResult();
			return spliceList(result, workingDir, inResources);
		}
    catch (Exception e)
		{
//			System.out.println("CommitDialog::fillFileList() Error:");
//			System.out.println("Project:" + getProject().toString());
//			System.out.println("Unable to get status " + e.getMessage());
			String msg = "Project " + getProject().toString() + ": unable to get status " + e.getMessage();
			MercurialEclipsePlugin.logError(msg,e);			return null;
		}
	}

	/**
	 * Finds if there is a IFile that matches the fileName Warning Recursive!!!
	 * 
	 * @param string
	 * @param fileNameWithWorkingDir
	 *          Use this to try to match the outpack to the IResource in the
	 *          inResources array
	 * @param inResource
	 *          the resourse to check if it is a IFolder we to a recursive
	 *          search...
	 * @return matching IResource or null
	 */
	private IResource findIResource(String fileName,String fileNameWithWorkingDir, IResource inResource)
	{
		IResource thisResource = null;

		if (inResource instanceof IFile)
		{
			IFile thisIFile = (IFile) inResource;
			if (thisIFile.getLocation().toOSString().equals(fileNameWithWorkingDir))
			{
				return thisIFile; // Found a match
			}
		} 
    else if (inResource instanceof IFolder)
		{
			try
			{
				IFolder thisIFolder = (IFolder) inResource;
				IResource folderResources[] = thisIFolder.members();
				for (int res = 0; res < folderResources.length; res++)
				{
					// Mercurial doesn't control directories or projects and so
					// will just return that they're
					// untracked.

					thisResource = findIResource(fileName, fileNameWithWorkingDir,
							folderResources[res]);
					if (thisResource != null)
					{
						return thisResource; // Found a resource
					}
				}
			} 
      catch (CoreException e)
			{
				MercurialEclipsePlugin.logError(e);
			}
		}
		return thisResource;
	}

	/**
	 * 
	 * @param string
	 * @param workingDir
	 *          Use this to try to match the outpack to the IResource in the
	 *          inResources array
	 * @return
	 */
	private CommitResource[] spliceList(String string, File workingDir,
			IResource[] inResources)
	{
		/*
		 * System.out.println("Changed resources: "); System.out.println(string);
		 * System.out.println("workingDir:" + workingDir.toString());
		 * System.out.println(" IResources:"); for(int res = 0; res <
		 * inResources.length; res++) { // Mercurial doesn't control directories or
		 * projects and so will just return that they're // untracked.
		 * System.out.println(" <" + inResources[res].getLocation().toOSString() +
		 * ">"); }
		 */
		ArrayList list = new ArrayList();
		StringTokenizer st = new StringTokenizer(string);
		String status;
		String fileName;
		IResource thisResource;
		String fileNameWithWorkingDir;
		String eol = System.getProperty("line.separator");

		// Tokens are always in pairs as lines are in the form "A
		// TEST_FOLDER\test_file2.c"
		// where the first token is the status and the 2nd is the path relative
		// to the project.
		while (st.hasMoreTokens())
		{
			status = st.nextToken(" ").trim();
			fileName = st.nextToken(eol).trim();
			thisResource = null;
			fileNameWithWorkingDir = workingDir + File.separator + fileName;

			for (int res = 0; res < inResources.length; res++)
			{
				// Mercurial doesn't control directories or projects and so will
				// just return that they're untracked.

				thisResource = findIResource(fileName, fileNameWithWorkingDir,inResources[res]);
				if (thisResource == null)
				{
					continue; // Found a resource
				}
			}

			if (thisResource == null)
			{
				// Create a resource could be a deleted file we want to commit
				IPath projPath = getProject().getLocation();
				// System.out.println("projPath.toOSString() <" +
				// projPath.toOSString() + ">");
				// System.out.println("fileNameWithWorkingDir <" +
				// fileNameWithWorkingDir + ">");
				if (fileNameWithWorkingDir.startsWith(projPath.toOSString()))
				{ // Relative
					// path
					// from
					// Project
					String fileNameWithWorkingDirFromProject = fileNameWithWorkingDir.substring(projPath.toOSString().length());
					IFile file = getProject().getFile(fileNameWithWorkingDirFromProject);
					thisResource = (IResource) file;
				} 
				else
				{ // This is a full path
					IFile file = getProject().getFile(fileNameWithWorkingDir);
					thisResource = (IResource) file;
				}
			}
			/*
			 * if(thisResource.exists()) { System.out.println(" Output <" + fileName + ">
			 * Resource <" + thisResource.toString() + ">"); } else {
			 * System.out.println(" Output <" + fileName + "> Resource <" +
			 * thisResource.toString() + "> Fake resource!"); }
			 */
			if (!status.startsWith("?") || !Team.isIgnoredHint(thisResource))
			{
			  //file is allready managed
			  //or file is not in "ignore list"
				list.add(new CommitResource(status, thisResource, new File(fileName)));
			}
		}

		return (CommitResource[]) list.toArray(new CommitResource[0]);
	}

	private IProject getProject()
	{
		return project;
	}
}
