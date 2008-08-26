/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author bastian
 * 
 */
public class HgResourceMapping extends ResourceMapping {

    private HgFilesystemObject file;

    /**
     * Constructor
     * 
     * @param file
     *            the file to create a mapping for. This can also be a folder.
     * @throws CoreException
     * @throws IOException
     */
    public HgResourceMapping(File file) throws IOException, CoreException {
        if (file.isDirectory()) {
            this.file = new HgFolder(file);
        } else {
            this.file = new HgFile(file);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.resources.mapping.ResourceMapping#getModelObject()
     */
    @Override
    public Object getModelObject() {
        return file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.resources.mapping.ResourceMapping#getModelProviderId()
     */
    @Override
    public String getModelProviderId() {
        return MercurialTeamProvider.ID;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.resources.mapping.ResourceMapping#getProjects()
     */
    @Override
    public IProject[] getProjects() {
        List<IProject> projects = new ArrayList<IProject>();
        if (file instanceof HgFolder) {
            HgFolder folder = (HgFolder) file;
            List<File> projectFiles;
            try {
                projectFiles = folder.getProjectFiles();
                IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
                        .getRoot();
                for (File f : projectFiles) {
                    try {
                        IProject project = (IProject) workspaceRoot
                                .findContainersForLocation(new Path(f
                                        .getParentFile().getCanonicalPath()))[0];
                        projects.add(project);
                    } catch (IOException e) {
                        MercurialEclipsePlugin.logError(e);
                    }
                }
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
        return projects.toArray(new IProject[projects.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.resources.mapping.ResourceMapping#getTraversals(org.
     * eclipse.core.resources.mapping.ResourceMappingContext,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public ResourceTraversal[] getTraversals(ResourceMappingContext context,
            IProgressMonitor monitor) throws CoreException {
        IResource resource = MercurialUtilities.convert(file);
        int depth = IResource.DEPTH_ZERO;        
        ResourceTraversal resourceTraversal = new ResourceTraversal(
                new IResource[] { resource }, depth, IContainer.EXCLUDE_DERIVED);
        return new ResourceTraversal[] { resourceTraversal };
    }

}
