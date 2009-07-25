/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;

public class MercurialSynchronizePageActionGroup extends ModelSynchronizeParticipantActionGroup {

    @Override
    public void initialize(ISynchronizePageConfiguration configuration) {
        super.initialize(configuration);

        super.appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
                ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
                new CommitSynchronizeAction("Commit",
                        configuration, getVisibleRootsSelectionProvider()));

        super.appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
                ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
                new PullSynchronizeAction("Pull",
                        configuration, getVisibleRootsSelectionProvider(), false));

        super.appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
                ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
                new PullSynchronizeAction("Pull and Update",
                        configuration, getVisibleRootsSelectionProvider(), true));

        super.appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
                ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
                new ShowHistorySynchronizeAction("Show History",
                        configuration, getVisibleRootsSelectionProvider()));
    }


}
