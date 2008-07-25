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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;

/**
 * @author zingo
 * 
 */
public class MercurialTeamProvider extends RepositoryProvider {

    public static final String ID = "com.vectrace.MercurialEclipse.team.MercurialTeamProvider";

    /**
     * Qualified Name for the repository a project was cloned from.
     */
    public static final QualifiedName QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY = new QualifiedName(
            ID + ".projectSourceRepository",
            "MercurialEclipseProjectSourceRepository");

    public static final QualifiedName QUALIFIED_NAME_DEFAULT_REVISION_LIMIT = new QualifiedName(
            ID + ".defaultRevisionLimit", "defaultRevisionLimit");

    private static final Map<IProject, Boolean> HG_ROOTS = new HashMap<IProject, Boolean>();

    private MercurialHistoryProvider FileHistoryProvider = null;

    /**
	 * 
	 */
    public MercurialTeamProvider() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.RepositoryProvider#configureProject()
     */
    @Override
    public void configureProject() throws CoreException {
        getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        getHgRoot(getProject());
    }

    /**
     * Determines if the resources hg root is known. If it isn't known,
     * Mercurial is called to determine it. The result will be saved as project
     * persistent property on the resource's project with the qualified name
     * {@link ResourceProperties#HG_ROOT}.
     * 
     * This property can be used to create a {@link java.io.File}.
     * 
     * @param resource
     *            the resource to get the hg root for
     * @return the canonical file path of the HgRoot
     * @throws CoreException
     */
    private static String getAndStoreHgRootPath(IResource resource)
            throws CoreException {
        assert (resource != null);
        IProject project = resource.getProject();
        assert (project != null);
        String root = project.getPersistentProperty(ResourceProperties.HG_ROOT);
        if (root == null) {
            root = HgRootClient.getHgRoot(resource);
        }
        if (root != null && root.length() != 0) {
            MercurialTeamProvider.HG_ROOTS.put(project, Boolean.valueOf(false));
        } else {
            throw new HgException(project.getName()
                    + " does not belong to a Hg repository.");
        }
        return root;
    }

    /**
     * Determines if the resources hg root is known. If it isn't known,
     * Mercurial is called to determine it. The result will be saved as project
     * persistent property on the resource's project with the qualified name
     * {@link ResourceProperties#HG_ROOT}.
     * 
     * This property can be used to create a {@link java.io.File}.
     * 
     * @param File
     *            the {@link java.io.File} to get the hg root for
     * @return the canonical file path of the HgRoot or null if no resource
     *         could be found in workspace that matches the file
     * @throws HgException
     *             if no hg root was found or a critical error occurred.
     */
    private static String getAndStoreHgRootPath(File file) throws CoreException {
        assert (file != null);

        try {
            IResource resource = ResourcesPlugin.getWorkspace().getRoot()
                    .getFileForLocation(new Path(file.getCanonicalPath()));
            return getAndStoreHgRootPath(resource);
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Gets the hg root of a resource as {@link java.io.File}.
     * 
     * @param resource
     *            the resource to get the hg root for
     * @return the {@link java.io.File} referencing the hg root directory
     * @throws HgException
     */
    public static File getHgRoot(IResource resource) throws HgException {
        assert (resource != null);
        try {
            return new File(getAndStoreHgRootPath(resource));
        } catch (CoreException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Gets the hg root of a resource as {@link java.io.File}.
     * 
     * @param file
     *            a {@link java.io.File}
     * @return the file object of the root.
     * @throws CoreException
     */
    public static File getHgRoot(File file) throws CoreException {
        assert (file != null);
        return new File(getAndStoreHgRootPath(file));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.resources.IProjectNature#deconfigure()
     */
    public void deconfigure() throws CoreException {
        IProject project = getProject();
        assert (project != null);
        // cleanup
        /*
         * 
         * Since Eclipse 3.4 I guess we have to rely on the GC here until we
         * drop support for Eclipse 3.3
         * 
         * project.getPersistentProperties().clear();
         * project.getSessionProperties().clear();
         */
        HG_ROOTS.remove(project);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.RepositoryProvider#getID()
     */
    @Override
    public String getID() {
        return ID;
    }

    @Override
    public IMoveDeleteHook getMoveDeleteHook() {
        return new HgMoveDeleteHook();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.RepositoryProvider#getFileHistoryProvider()
     */
    @Override
    public IFileHistoryProvider getFileHistoryProvider() {
        if (FileHistoryProvider == null) {
            FileHistoryProvider = new MercurialHistoryProvider();
        }
        // System.out.println("getFileHistoryProvider()");
        return FileHistoryProvider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResources()
     */
    @Override
    public boolean canHandleLinkedResources() {
        return true;
    }
}
