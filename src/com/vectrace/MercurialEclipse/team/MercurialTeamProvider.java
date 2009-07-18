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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;

import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author zingo
 * 
 */
public class MercurialTeamProvider extends RepositoryProvider {

    public static final String ID = "com.vectrace.MercurialEclipse.team.MercurialTeamProvider"; //$NON-NLS-1$

    /**
     * Qualified Name for the repository a project was cloned from.
     */
    public static final QualifiedName QUALIFIED_NAME_PROJECT_SOURCE_REPOSITORY = new QualifiedName(ID
            + ".projectSourceRepository", //$NON-NLS-1$
            "MercurialEclipseProjectSourceRepository"); //$NON-NLS-1$

    public static final QualifiedName QUALIFIED_NAME_DEFAULT_REVISION_LIMIT = new QualifiedName(ID
            + ".defaultRevisionLimit", "defaultRevisionLimit"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final Map<IProject, Boolean> HG_ROOTS = new HashMap<IProject, Boolean>();

    private MercurialHistoryProvider FileHistoryProvider = null;

    /**
	 * 
	 */
    public MercurialTeamProvider() {
        super();
    }

    @Override
    public void configureProject() throws CoreException {
        getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        getHgRoot(getProject());

        // try to find .hg directory to set it as private member
        IResource hgDir = getProject().findMember(".hg"); //$NON-NLS-1$
        if (hgDir != null && hgDir.exists()) {
            hgDir.setTeamPrivateMember(true);
            hgDir.setDerived(true);
        }
    }

    /**
     * Determines if the resources hg root is known. If it isn't known, Mercurial is called to determine it. The result
     * will be saved as project session property on the resource's project with the qualified name
     * {@link ResourceProperties#HG_ROOT}.
     * 
     * This property can be used to create a {@link java.io.File}.
     * 
     * @param resource
     *            the resource to get the hg root for
     * @return the canonical file path of the HgRoot
     * @throws CoreException
     */
    private static HgRoot getAndStoreHgRoot(IResource resource) throws HgException {
        assert (resource != null);
        IProject project = resource.getProject();
        HgRoot hgRoot = null;
        String noHgFoundMsg = "There is no hg repository here (.hg not found)!";
        if (project != null) {
        try {
            hgRoot = (HgRoot) project.getSessionProperty(ResourceProperties.HG_ROOT);
            if (hgRoot == null) {
                String rootPath = HgRootClient.getHgRoot(resource);
                if (rootPath == null || rootPath.length() == 0) {
                        throw new HgException(noHgFoundMsg);
                }
                hgRoot = new HgRoot(new File(rootPath));
                setRepositoryEncoding(project, hgRoot);
                project.setSessionProperty(ResourceProperties.HG_ROOT, hgRoot);
            }
            MercurialTeamProvider.HG_ROOTS.put(project, Boolean.valueOf(false));
        } catch (Exception e) {
            // the first low of exception handling: either log it and do not throw,
            // or throw and do not log.
            // MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
        } else {
            throw new HgException(noHgFoundMsg);
        }
        return hgRoot;
    }

    /**
     * @param project
     * @param hgRoot
     * @throws CoreException
     * @throws HgException
     */
    private static void setRepositoryEncoding(IProject project, HgRoot hgRoot) throws CoreException {
        String encoding = null;
        // project settings in Eclipse override all other settings
        if (project != null) {
            encoding = project.getDefaultCharset();
            if (encoding != null) {
                hgRoot.setEncoding(Charset.forName(encoding));
            }
        }
    }

    private static HgRoot getHgRootFile(File file) throws HgException {
        assert (file != null);
        try {
            File rootFile = HgRootClient.getHgRoot(file);
            HgRoot root = new HgRoot(rootFile);
            return root;
        } catch (Exception e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Determines if the resources hg root is known. If it isn't known, Mercurial is called to determine it. The result
     * will be saved as project persistent property on the resource's project with the qualified name
     * {@link ResourceProperties#HG_ROOT}.
     * 
     * This property can be used to create a {@link java.io.File}.
     * 
     * @param File
     *            the {@link java.io.File} to get the hg root for
     * @return the canonical file path of the HgRoot or null if no resource could be found in workspace that matches the
     *         file
     * @throws HgException
     *             if no hg root was found or a critical error occurred.
     */
    private static HgRoot getAndStoreHgRoot(File file) throws CoreException {
        assert (file != null);
        IResource resource = MercurialUtilities.convert(file);
        if (resource != null) {
            return getAndStoreHgRoot(resource);
        }
        return getHgRootFile(file);
    }

    /**
     * Gets the project hgrc as a {@link java.io.File}.
     * 
     * @param project
     *            the project to get the hgrc for
     * @return the {@link java.io.File} referencing the hgrc file, <code>null</code> if it doesn't exist.
     * @throws HgException
     *             if an error occured (e.g., no root could be found).
     */
    public static File getHgRootConfig(IResource resource) throws HgException {
        assert (resource != null);
        HgRoot hgRoot = getHgRoot(resource);
        return hgRoot.getConfig();
    }

    /**
     * Gets the hg root of a resource as {@link java.io.File}.
     * 
     * @param resource
     *            the resource to get the hg root for
     * @return the {@link java.io.File} referencing the hg root directory
     * @throws HgException
     *             if an error occurred (e.g. no root could be found)
     */
    public static HgRoot getHgRoot(IResource resource) throws HgException {
        assert (resource != null);
        try {
            return getAndStoreHgRoot(resource);
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
    public static HgRoot getHgRoot(File file) throws CoreException {
        assert (file != null);
        return getAndStoreHgRoot(file);
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
        HG_ROOTS.remove(project);
        project.setPersistentProperty(ResourceProperties.HG_ROOT, null);
        project.setPersistentProperty(ResourceProperties.MERGING, null);
        project.setSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED, null);

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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.RepositoryProvider#canHandleLinkedResourceURI()
     */
    @Override
    public boolean canHandleLinkedResourceURI() {
        return canHandleLinkedResources();
    }
}
