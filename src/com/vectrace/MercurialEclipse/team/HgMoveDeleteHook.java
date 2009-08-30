/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch           - Code reformatting to code style and refreshes
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
import com.vectrace.MercurialEclipse.commands.HgRemoveClient;
import com.vectrace.MercurialEclipse.commands.HgRenameClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;


/**
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 *
 *         Hook into Eclipse rename and delete file operations so that the
 *         appropriate changes can be tracked in Mercurial as well.
 */
public class HgMoveDeleteHook implements IMoveDeleteHook {

    private static final MercurialStatusCache CACHE = MercurialStatusCache.getInstance();

    /**
     * @returns <code>true</code> if this file under this under Mercurial
     *          control.
     */
    private boolean isInMercurialRepo(IResource file, IProgressMonitor monitor) {
        return CACHE.isSupervised(file);
    }

    /**
     * Determines if a folder has supervised files
     *
     * @returns <code>true</code> if there are files under this folder that are
     *          under Mercurial control.
     */
    private boolean folderHasMercurialFiles(IFolder folder,
            IProgressMonitor monitor) {
        if (!isInMercurialRepo(folder, monitor)) {
            // Resource could be inside a link or something do nothing
            // in the future this could check is this is another repository
            return false;
        }

        try {
            IResource[] children = folder.members();
            for (IResource resource : children) {
                if (resource.getType() == IResource.FILE) {
                    if (resource.exists() && isInMercurialRepo(resource, monitor)) {
                        return true;
                    }
                } else {
                    if(folderHasMercurialFiles((IFolder) resource, monitor)){
                        return true;
                    }
                }
            }
        } catch (CoreException e) {
            /*
             * Let's assume that this means there are no resources under this
             * one as it probably doesn't properly exist. Let eclipse do
             * everything.
             */
            return false;
        }

        return false;
    }

    public boolean deleteFile(IResourceTree tree, IFile file, int updateFlags,
            IProgressMonitor monitor) {
        /*
         * Returning false indicates that the caller should invoke
         * tree.standardDeleteFile to actually remove the resource from the file
         * system and eclipse.
         */

        if (!isInMercurialRepo(file, monitor) || file.isDerived()) {
            return false;
        }

        return deleteHgFiles(tree, file, monitor);
    }

    public boolean deleteFolder(IResourceTree tree, IFolder folder,
            int updateFlags, IProgressMonitor monitor) {
        /*
         * Mercurial doesn't control directories. However, as a short cut
         * performing an operation on a folder will affect all subtending files.
         * Check that there is at least 1 file and if so there is Mercurial work
         * to do, otherwise there is no Mercurial work to be done.
         */
        if (!folderHasMercurialFiles(folder, monitor)) {
            return false;
        }

        /*
         * NOTE: There are bugs with Mercurial 0.9.1 on Windows and folder
         * delete/rename operation. See:
         * http://www.selenic.com/mercurial/bts/issue343,
         * http://www.selenic.com/mercurial/bts/issue303, etc. Returning false
         * indicates that the caller should invoke tree.standardDeleteFile to
         * actually remove the resource from the file system and eclipse.
         */
        return deleteHgFiles(tree, folder, monitor);
    }

    /**
     * Perform the file or folder (ie multiple file) delete.
     *
     * @returns <code>false</code> if the action succeeds, <code>true</code>
     *          otherwise. This syntax is to match the desired return code for
     *          <code>deleteFile</code> and <code>deleteFolder</code>.
     */
    private boolean deleteHgFiles(IResourceTree tree, IResource resource, IProgressMonitor monitor) {
        // TODO: Decide if we should have different Hg behaviour based on the
        // force flag provided in updateFlags.
        try {
            // Delete the file from the Mercurial repository.
            HgRemoveClient.removeResource(resource, monitor);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            return false;
        }

        // We removed the file ourselves, need to tell.
        if(resource.getType() == IResource.FOLDER) {
            tree.deletedFolder((IFolder) resource);
        } else {
            tree.deletedFile((IFile) resource);
        }

        // Returning true indicates that this method has removed resource in both
        // the file system and eclipse.
        return true;
    }

    public boolean deleteProject(IResourceTree tree, IProject project,
            int updateFlags, IProgressMonitor monitor) {
        if ((updateFlags & IResource.ALWAYS_DELETE_PROJECT_CONTENT) != 0) {
            IFolder folder = project.getFolder(".hg"); //$NON-NLS-1$
            try {
                folder.delete(updateFlags, monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
                return true;
            }
        }

        return false;
    }

    public boolean moveFile(IResourceTree tree, IFile source,
            IFile destination, int updateFlags, IProgressMonitor monitor) {

        if (!isInMercurialRepo(source, monitor)) {
            return false;
        }

        // Move the file in the Mercurial repository.
        if (!moveHgFiles(source, destination, monitor)) {
            return true;
        }

        // We moved the file ourselves, need to tell.
        tree.movedFile(source, destination);

        // Returning true indicates that this method has moved resource in both
        // the file system and eclipse.
        return true;
    }

    public boolean moveFolder(IResourceTree tree, IFolder source,
            IFolder destination, int updateFlags, IProgressMonitor monitor) {
        /*
         * Mercurial doesn't control directories. However, as a short cut
         * performing an operation on a folder will affect all subtending files.
         * Check that there is at least 1 file and if so there is Mercurial work
         * to do, otherwise there is no Mercurial work to be done.
         */
        if (!folderHasMercurialFiles(source, monitor)) {
            return false;
        }

        // Move the folder (ie all subtending files) in the Mercurial
        // repository.
        if (!moveHgFiles(source, destination, monitor)) {
            return true;
        }

        // We moved the file ourselves, need to tell.
        tree.movedFolderSubtree(source, destination);

        // Returning true indicates that this method has moved resource in both
        // the file system and eclipse.
        return true;
    }

    /**
     * Move the file or folder (ie multiple file).
     *
     * @returns <code>true</code> if the action succeeds, <code>false</code>
     *          otherwise.
     */
    private boolean moveHgFiles(IResource source, IResource destination,
            IProgressMonitor monitor) {
        // Rename the file in the Mercurial repository.

        // TODO: Decide if we should have different Hg behaviour based on the
        // force flag provided in
        // updateFlags.
        try {
            HgRenameClient.renameResource(source, destination, monitor);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(Messages.getString("HgMoveDeleteHook.moveFailed"), e);
            return false;
        }
        return true;
    }

    public boolean moveProject(IResourceTree tree, IProject source,
            IProjectDescription description, int updateFlags,
            IProgressMonitor monitor) {
        // Punting to eclipse is fine as presumably all resources in the .hg
        // folder are relative to the root and will remain intact.
        return false;
    }

}
