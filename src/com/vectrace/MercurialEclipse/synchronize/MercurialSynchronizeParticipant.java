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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class MercurialSynchronizeParticipant extends
        ModelSynchronizeParticipant {
    private static final String REPOSITORY_LOCATION = "REPOSITORY_LOCATION"; //$NON-NLS-1$
    private String secondaryId;
    private HgRepositoryLocation repositoryLocation;

    public MercurialSynchronizeParticipant(
            MergeContext ctx,
            HgRepositoryLocation repositoryLocation) {
        super(ctx);
        this.repositoryLocation = repositoryLocation;        
        this.secondaryId = new Date().toString();
    }

    @Override
    public void init(String secId, IMemento memento) throws PartInitException {
        super.init(secondaryId, memento);
        try {
            ISynchronizeParticipantDescriptor descriptor = TeamUI
                    .getSynchronizeManager().getParticipantDescriptor(getId());
            setInitializationData(descriptor);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
        }

        IMemento myMemento = memento
                .getChild(MercurialSynchronizeParticipant.class.getName());

        this.secondaryId = secId;
        String uri = myMemento.getString(REPOSITORY_LOCATION);

        try {
            this.repositoryLocation = MercurialEclipsePlugin.getRepoManager()
                    .getRepoLocation(uri, null, null);
        } catch (URISyntaxException e) {
            throw new PartInitException(e.getLocalizedMessage(), e);
        }
    }
        
    public MercurialSynchronizeParticipant() {
    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
        IMemento myMemento = memento
                .createChild(MercurialSynchronizeParticipant.class.getName());
        myMemento.putString(REPOSITORY_LOCATION, repositoryLocation.getUri()
                .toString());
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant#initializeContext
     * (org.eclipse.team.core.mapping.provider.SynchronizationContext)
     */
    @Override
    protected void initializeContext(SynchronizationContext context) {
        super.initializeContext(context);
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
        return Messages.getString("MercurialSynchronizeParticipant.syncOnRepo").concat("" //$NON-NLS-1$ //$NON-NLS-2$
                + repositoryLocation);
    }    

    /**
     * @return the repositoryLocation
     */
    public HgRepositoryLocation getRepositoryLocation() {
        return repositoryLocation;
    }

}
