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
package com.vectrace.MercurialEclipse.synchronize;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.synchronize.actions.MercurialSynchronizePageActionGroup;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class MercurialSynchronizeParticipant extends
ModelSynchronizeParticipant {
    private static final String REPOSITORY_LOCATION = "REPOSITORY_LOCATION"; //$NON-NLS-1$
    private String secondaryId;
    private HgRepositoryLocation repositoryLocation;

    public MercurialSynchronizeParticipant() {
    }

    public MercurialSynchronizeParticipant(
            MergeContext ctx,
            HgRepositoryLocation repositoryLocation) {
        super(ctx);
        this.repositoryLocation = repositoryLocation;
        this.secondaryId = new Date().toString();
        try {
            ISynchronizeParticipantDescriptor descriptor = TeamUI
            .getSynchronizeManager().getParticipantDescriptor(getId());
            setInitializationData(descriptor);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    @Override
    public void init(String secId, IMemento memento) throws PartInitException {
        IMemento myMemento = memento
        .getChild(MercurialSynchronizeParticipant.class.getName());

        this.secondaryId = secId;
        String uri = myMemento.getString(REPOSITORY_LOCATION);

        try {
            this.repositoryLocation = MercurialEclipsePlugin
            .getRepoManager().getRepoLocation(uri);
        } catch (URISyntaxException e) {
            throw new PartInitException(e.getLocalizedMessage(), e);
        }

        super.init(secondaryId, memento);
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        IMemento myMemento = memento
        .createChild(MercurialSynchronizeParticipant.class.getName());
        myMemento.putString(REPOSITORY_LOCATION, repositoryLocation.getSaveString());
    }

    @Override
    protected MergeContext restoreContext(ISynchronizationScopeManager manager) throws CoreException {
        // TODO probably it should do something more helpful
        ISynchronizationScope scope;
        if(manager instanceof SynchronizationScopeManager){
            SynchronizationScopeManager manager2 = (SynchronizationScopeManager) manager;
            scope = manager2.getScope();
            ResourceMapping[] mappings = scope.getMappings();
            Set<IProject> projectSet = new HashSet<IProject>();
            for (ResourceMapping mapping : mappings) {
                IProject[] projects = mapping.getProjects();
                for (IProject iProject : projects) {
                    projectSet.add(iProject);
                }
            }
            if(!projectSet.isEmpty()) {
                scope = new RepositorySynchronizationScope(projectSet.toArray(new IProject[0]));
            } else {
                scope = new RepositorySynchronizationScope(MercurialStatusCache.getInstance()
                        .getAllManagedProjects());
            }
        } else {
            scope = new RepositorySynchronizationScope(MercurialStatusCache.getInstance()
                    .getAllManagedProjects());
        }
        MercurialSynchronizeSubscriber subscriber = new MercurialSynchronizeSubscriber(
                scope, repositoryLocation);
        return new HgSubscriberMergeContext(subscriber, manager);
    }

    @Override
    protected void initializeContext(SynchronizationContext context) {
        if(context != null) {
            super.initializeContext(context);
        }
    }

    @Override
    public void dispose() {
        if(getContext() != null) {
            super.dispose();
        }
    }

    @Override
    public String getId() {
        return getClass().getName();
    }

    @Override
    public String getSecondaryId() {
        return secondaryId;
    }

    @Override
    public String getName() {
        return Messages.getString("MercurialSynchronizeParticipant.syncOnRepo") //$NON-NLS-1$
        + repositoryLocation;
    }

    /**
     * @return the repositoryLocation
     */
    public HgRepositoryLocation getRepositoryLocation() {
        return repositoryLocation;
    }

    @Override
    protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
        // allows us to contribute our own actions to the synchronize view via java code
        return new MercurialSynchronizePageActionGroup();
    }

    @Override
    protected boolean isViewerContributionsSupported() {
        // allows us to contribute our own actions to the synchronize view via plugin.xml
        return true;
    }
}
