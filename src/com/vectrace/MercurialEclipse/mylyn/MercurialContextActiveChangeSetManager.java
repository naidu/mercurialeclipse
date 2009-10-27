/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * zluspai	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.mylyn;

import org.eclipse.core.resources.IResource;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.team.ui.ContextActiveChangeSetManager;
import org.eclipse.mylyn.tasks.core.ITask;

/**
 * Change-set management for Mylyn comments.
 * See:
 * http://wiki.eclipse.org/Mylyn_Integrator_Reference#Change_set_management
 *
 * @author zluspai
 *
 */
@SuppressWarnings("restriction")
public class MercurialContextActiveChangeSetManager extends ContextActiveChangeSetManager {

    // the current task
    private static ITask currentTask;

    @Override
    protected void updateChangeSetLabel(ITask task) {
        super.updateChangeSetLabel(task);
    }

    @Override
    protected void initContextChangeSets() {
        super.initContextChangeSets();
    }

    @Override
    public void clearActiveChangeSets() {
        super.clearActiveChangeSets();
    }

    @Override
    public IResource[] getResources(ITask task) {
        return super.getResources(task);
    }

    @Override
    public void contextActivated(IInteractionContext context) {
        currentTask = MylynFacadeImpl.getCurrentTask();
        super.contextActivated(context);
    }

    @Override
    public void contextDeactivated(IInteractionContext context) {
        try {
            super.contextDeactivated(context);
        } finally {
            currentTask = null;
        }
    }


}
