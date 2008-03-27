/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan Groschupf          - logError
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.IResourceTree;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.DeleteFileAction;
import com.vectrace.MercurialEclipse.actions.MoveFileAction;
import com.vectrace.MercurialEclipse.actions.StatusContainerAction;

/**
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * Hook into Eclipse rename and delete file operations so that the appropriate changes
 * can be tracked in Mercurial as well.
 */
public class HgMoveDeleteHook implements IMoveDeleteHook
{
  /**
   * @returns <code>true</code> if this file under this under Mercurial control.
   */
  private boolean isInMercurialRepo(IFile file, IProgressMonitor monitor)
  {
    IResource[] fileArray = {file};
//    System.out.println("HgMoveDeleteHook::isInMercurialRepo() StatusContainerAction(null,{" + file.toString() + "})");
    StatusContainerAction statusAction = new StatusContainerAction(null, fileArray);

    if(MercurialUtilities.isResourceInReposetory(file, false) != true)
    {
      //Resource could be inside a link or something do nothing
      // in the future this could check is this is another repository
      return false;
    }

    
    try
    {
      statusAction.run(monitor);
      final String result = statusAction.getResult();
      if(result == null)
      {
//        System.out.println("HgMoveDeleteHook::isInMercurialRepo() statusAction.getResult()=null");
        return false;
      }
      if((result.length() != 0) && result.startsWith("?"))
      {
//        System.out.println("HgMoveDeleteHook::isInMercurialRepo() false");
        return false;
      }     
    } 
    catch (Exception e)
    {
      System.out.println("HgMoveDeleteHook::isInMercurialRepo() Unable to get status " + e.getMessage());
      return false;
    }

    return true;
  }

  /**
   * @returns <code>true</code> if there are files under this folder that are under
   *          Mercurial control.
   */
  private boolean folderHasMercurialFiles(IFolder folder, IProgressMonitor monitor)
  {
//    System.out.println("HgMoveDeleteHook::folderHasMercurialFiles("+folder.toString() + ",monitor)");
    if(MercurialUtilities.isResourceInReposetory(folder, false) != true)
    {
      //Resource could be inside a link or something do nothing
      // in the future this could check is this is another repository
      return false;
    }

    
    try
    {
      IResource[] subtending = folder.members();
      int numResources;
      for(numResources = 0; numResources < subtending.length; numResources++)
      {
        if(subtending[numResources].getType() == IResource.FILE &&
           subtending[numResources].exists() &&
           isInMercurialRepo((IFile)subtending[numResources], monitor))
        {
          return true;
        }
      }    
    }
    catch(CoreException e)
    {
      // Let's assume that this means there are no resources under this one as it probably doesn't
      // properly exist. Let eclipse do everything.
      return false;
    }
    
    return false;
  }

  public boolean deleteFile(IResourceTree tree, IFile file, int updateFlags, IProgressMonitor monitor)
  {
    // Returning false indicates that the caller should invoke tree.standardDeleteFile to actually
    // remove the resource from the file system and eclipse.
//    System.out.println("HgMoveDeleteHook::deleteFile(tree," + file.toString() + ",updateFlags,monitor)");
    if(!isInMercurialRepo(file, monitor))
    {
      return false;
    }

    return deleteHgFiles(file, monitor);
  }

  public boolean deleteFolder(IResourceTree tree, IFolder folder, int updateFlags, IProgressMonitor monitor)
  {
    // Mercurial doesn't control directories. However, as a short cut performing an operation on a
    // folder will affect all subtending files. Check that there is at least 1 file and if so there is Mercurial
    // work to do, otherwise there is no Mercurial work to be done.
//    System.out.println("HgMoveDeleteHook::deleteFolder(tree," + folder.toString() + ",updateFlags,monitor)");
    if(!folderHasMercurialFiles(folder,monitor))
    {
      return false;
    }

    // NOTE: There are bugs with Mercurial 0.9.1 on Windows and folder delete/rename operation.
    //       See: http://www.selenic.com/mercurial/bts/issue343,
    //            http://www.selenic.com/mercurial/bts/issue303, etc.
    // Returning false indicates that the caller should invoke tree.standardDeleteFile to actually
    // remove the resource from the file system and eclipse.
    return deleteHgFiles(folder, monitor);
  }
  
  /**
   * Perform the file or folder (ie multiple file) delete.
   * @returns <code>false</code> if the action succeeds, <code>true</code> otherwise. This
   *          syntax is to match the desired return code for <code>deleteFile</code> and
   *          <code>deleteFolder</code>. 
   */
  private boolean deleteHgFiles(IResource resource, IProgressMonitor monitor)
  {
    // Delete the file from the Mercurial repository.
    DeleteFileAction deleteAction = new DeleteFileAction(null, resource);
    
    // TODO: Decide if we should have different Hg behaviour based on the force flag provided in
    //       updateFlags.
    try
    {
      deleteAction.run(monitor);
    }
    catch(Exception e)
    {
      System.out.println("Delete failed: " + e.getMessage());
      return true;
    }
    
    return false;
  }
  
  public boolean deleteProject(IResourceTree tree, IProject project, int updateFlags, IProgressMonitor monitor)
  {
    if((updateFlags & IResource.ALWAYS_DELETE_PROJECT_CONTENT) != 0)
    {
      // TODO: Need to delete the .hg directory...but how to?
      IFolder folder = project.getFolder(".hg");   

      try
      {
        folder.delete(updateFlags, monitor);
      }
      catch(CoreException e)
      {
        System.out.println("Unable to delete .hg directory: " + e.getMessage());
        return true;
      }
    }
    
    // TODO: Would be nice to check for any modification and confirm if a sync is desired first.
    return false;
  }

  public boolean moveFile(IResourceTree tree, IFile source, IFile destination, int updateFlags, IProgressMonitor monitor)
  {
//    System.out.println("HgMoveDeleteHook::moveFile(tree," + source.toString() + "," + destination.toString() + "updateFlags,monitor)");

    if(!isInMercurialRepo(source,monitor))
    {
      return false;
    }

    // Move the file in the Mercurial repository.
    if(!moveHgFiles( source, destination, monitor))
    {
      return true;
    }
    
    // We moved the file ourselves, need to tell.
    tree.movedFile(source, destination);

    // Returning true indicates that this method has moved resource in both the file system and eclipse.
    return true;
  }

  public boolean moveFolder(IResourceTree tree, IFolder source, IFolder destination, int updateFlags,
                            IProgressMonitor monitor)
  {
    // Mercurial doesn't control directories. However, as a short cut performing an operation on a
    // folder will affect all subtending files. Check that there is at least 1 file and if so there is Mercurial
    // work to do, otherwise there is no Mercurial work to be done.
//    System.out.println("HgMoveDeleteHook::moveFolder(tree," + source.toString() + "," + destination.toString() + "updateFlags,monitor)");

    if(!folderHasMercurialFiles(source,monitor))
    {
      return false;
    }

    // Move the folder (ie all subtending files) in the Mercurial repository.
    if(!moveHgFiles( source, destination, monitor))
    {
      return true;
    }
    
    // We moved the file ourselves, need to tell.
    tree.movedFolderSubtree(source, destination);

    // Returning true indicates that this method has moved resource in both the file system and eclipse.
    return true;
  }

  /**
   * Move the file or folder (ie multiple file).
   * @returns <code>true</code> if the action succeeds, <code>false</code> otherwise.
   */
  private boolean moveHgFiles(IResource source, IResource destination, IProgressMonitor monitor)
  {
    // Rename the file in the Mercurial repository.
    
    MoveFileAction moveAction = new MoveFileAction(null, source, destination);
    
    // TODO: Decide if we should have different Hg behaviour based on the force flag provided in
    //       updateFlags.
    try
    {
      moveAction.run(monitor);
    }
    catch(Exception e)
    {
      System.out.println("Move failed: " + e.getMessage());
      return false;
    }
    
    return true;
  }

  public boolean moveProject(IResourceTree tree, IProject source, IProjectDescription description, int updateFlags,
                             IProgressMonitor monitor)
  {
    // Punting to eclipse is fine as presumably all resources in the .hg folder are relative to the root and will remain intact.    
    return false;
  }

}
