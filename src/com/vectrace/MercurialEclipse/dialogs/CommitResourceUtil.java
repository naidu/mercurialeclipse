/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Jérôme Nègre              - some fixes
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.Team;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.StatusContainerAction;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public final class CommitResourceUtil {

    private final HgRoot root;

    public CommitResourceUtil(HgRoot root) {
        this.root = root;
    }

    public CommitResource[] getCommitResources(IResource[] inResources) {
        StatusContainerAction statusAction = new StatusContainerAction(null,
                inResources);
        File workingDir = statusAction.getWorkingDir();
        try {
            statusAction.run();
            String result = statusAction.getResult();
            return spliceList(result, workingDir, inResources);
        } catch (Exception e) {
            String msg = "HgRoot: " + root.getAbsolutePath()
                    + ": unable to get status " + e.getMessage();
            MercurialEclipsePlugin.logError(msg, e);
            return null;
        }
    }

   

    /**
     * 
     * @param string
     * @param workingDir
     *            Use this to try to match the outpack to the IResource in the
     *            inResources array
     * @return
     */
    private CommitResource[] spliceList(String string, File workingDir,
            IResource[] inResources) {

        ArrayList<CommitResource> list = new ArrayList<CommitResource>();
        StringTokenizer st = new StringTokenizer(string);
        String status;
        String fileName;
        IResource statusResource;        
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        // Tokens are always in pairs as lines are in the form "A
        // TEST_FOLDER\test_file2.c"
        // where the first token is the status and the 2nd is the path relative
        // to the project.
        while (st.hasMoreTokens()) {
            status = st.nextToken(" ").trim();
            fileName = st.nextToken("\n").trim();
            statusResource = null;

            for (int res = 0; res < inResources.length; res++) {
                // Mercurial doesn't control directories or projects and so will
                // just return that they're untracked.

                try {
                    statusResource = MercurialUtilities.convert(root
                            .getCanonicalPath()
                            + File.separator + fileName);
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                }
                if (statusResource == null) {
                    continue; // Found a resource
                }
            }

            if (statusResource == null) {
                // Create a resource could be a deleted file we want to commit
                IPath rootPath;
                try {
                    rootPath = new Path(root.getCanonicalPath());
                    Path filePath = new Path(fileName);                    
                    IFolder folder = workspaceRoot.getFolder(rootPath);
                    statusResource = folder.getFile(filePath);
                } catch (IOException e) {
                    MercurialEclipsePlugin.logError(e);
                }

            }
            
            if (!status.startsWith("?") || !Team.isIgnoredHint(statusResource)) {
                // file is allready managed
                // or file is not in "ignore list"
                list.add(new CommitResource(status, statusResource, new File(
                        fileName)));
            }
        }

        return list.toArray(new CommitResource[0]);
    }    
}
