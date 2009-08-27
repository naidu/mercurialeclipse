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
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.Team;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.StatusContainerAction;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public final class CommitResourceUtil {

    private HgRoot root;

    public CommitResourceUtil() {

    }

    public CommitResource[] getCommitResources(IResource[] inResources) throws HgException {
        StatusContainerAction statusAction = new StatusContainerAction(null, inResources);
        root = statusAction.getHgWorkingDir();
        try {
            statusAction.run();
            String result = statusAction.getResult();
            return spliceList(result, inResources);
        } catch (Exception e) {
            String msg = "HgRoot: " + root.getAbsolutePath() //$NON-NLS-1$
                    + Messages.getString("CommitResourceUtil.error.unableToGetStatus") + e.getMessage(); //$NON-NLS-1$
            MercurialEclipsePlugin.logError(msg, e);
            return null;
        }
    }


    private CommitResource[] spliceList(String string, IResource[] inResources) {

        ArrayList<CommitResource> list = new ArrayList<CommitResource>();
        StringTokenizer st = new StringTokenizer(string);
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        // Tokens are always in pairs as lines are in the form "A
        // TEST_FOLDER\test_file2.c"
        // where the first token is the status and the 2nd is the path relative
        // to the root.
        while (st.hasMoreTokens()) {
            String status = st.nextToken(" ").trim();
            String fileName = st.nextToken("\n").trim();
            if(status.startsWith("?")){
                continue;
            }
            Path path = new Path(new File(root, fileName).getAbsolutePath());
            IResource statusResource = workspaceRoot.getFileForLocation(path);
            if (!Team.isIgnoredHint(statusResource)) {
                // file is allready managed or file is not in "ignore list"
                list.add(new CommitResource(status, statusResource, new File(fileName)));
            }
        }

        return list.toArray(new CommitResource[0]);
    }
}
