/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;

public class UpdateHandler extends SingleResourceHandler {

    private String revision;
    private boolean cleanEnabled;

    @Override
    public void run(IResource resource) throws Exception {
        final IProject project = resource.getProject();
        boolean dirty = HgStatusClient.isDirty(project);
        if (dirty) {
            final boolean[] result = new boolean[1];
            if(Display.getCurrent() == null){
                Display.getDefault().syncExec(new Runnable() {
                    public void run() {
                        result[0] = MessageDialog.openQuestion(getShell(), "Uncommited Changes",
                        "Your project has uncommited changes.\nDo you really want to continue?");
                    }
                });
            } else {
                result[0] = MessageDialog.openQuestion(getShell(), "Uncommited Changes",
                "Your project has uncommited changes.\nDo you really want to continue?");
            }
            if (!result[0]) {
                return;
            }
        }
        HgUpdateClient.update(project, revision, cleanEnabled);
    }

    /**
     * @param revision the revision to use for the '-r' option, can be null
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * @param cleanEnabled true to add '-C' option
     */
    public void setCleanEnabled(boolean cleanEnabled) {
        this.cleanEnabled = cleanEnabled;
    }
}
