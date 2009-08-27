/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 *
 */
public class ResourceUtils {

    /**
     * Checks which editor is active an determines the IResource that is edited.
     */
    public static IResource getActiveResourceFromEditor() {
        IEditorPart editorPart = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActiveEditor();

        if (editorPart != null) {
            IFileEditorInput input = (IFileEditorInput) editorPart
                    .getEditorInput();
            IFile file = ResourceUtil.getFile(input);
            return file;
        }
        return null;
    }

    /**
     * @param resource a handle to possibly non-existing resource
     * @return a (file) path representing given resource
     */
    public static File getFileHandle(IResource resource) {
        IPath path = getPath(resource);
        return path.toFile();
    }

    /**
     * @param resource a handle to possibly non-existing resource
     * @return a (file) path representing given resource
     */
    public static IPath getPath(IResource resource) {
        IPath path = resource.getLocation();
        if(path == null){
            // file was removed
            path = resource.getProject().getLocation().append(
                    resource.getFullPath().removeFirstSegments(1));
        }
        return path;
    }

    /**
     * Converts a {@link java.io.File} to a workspace resource
     *
     * @param file
     * @return
     * @throws HgException
     */
    public static IResource convert(File file) throws HgException {
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource;
        if (file.isDirectory()) {
            resource = root.getContainerForLocation(new Path(canonicalPath));
        } else {
            resource = root.getFileForLocation(new Path(canonicalPath));
        }
        return resource;
    }

    /**
     * For a given path, tries to find out first <b>existing</b> parent directory
     * @param path may be null
     * @return may return null
     */
    public static File getFirstExistingDirectory(File path) {
        while (path != null && !path.isDirectory()) {
            path = path.getParentFile();
        }
        return path;
    }

    /**
     * @param resources non null
     * @return never null
     */
    public static Map<IProject, List<IResource>> groupByProject(List<IResource> resources) {
        Map<IProject, List<IResource>> result = new HashMap<IProject, List<IResource>>();
        for (IResource resource : resources) {
            IProject root = resource.getProject();
            List<IResource> list = result.get(root);
            if (list == null) {
                list = new ArrayList<IResource>();
                result.put(root, list);
            }
            list.add(resource);
        }
        return result;
    }

    /**
     * @param resources non null
     * @return never null
     */
    public static Map<HgRoot, List<IResource>> groupByRoot(List<IResource> resources) throws HgException {
        Map<HgRoot, List<IResource>> result = new HashMap<HgRoot, List<IResource>>();
        for (IResource resource : resources) {
            HgRoot root = MercurialTeamProvider.getHgRoot(resource);
            List<IResource> list = result.get(root);
            if (list == null) {
                list = new ArrayList<IResource>();
                result.put(root, list);
            }
            list.add(resource);
        }
        return result;
    }
}
