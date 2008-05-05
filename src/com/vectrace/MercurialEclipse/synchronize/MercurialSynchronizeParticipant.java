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

import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.synchronize.actions.MercurialSynchronizePageActionGroup;

public class MercurialSynchronizeParticipant extends SubscriberParticipant {
    private static final String REPOSITORY_LOCATION = "REPOSITORY_LOCATION";
    private String secondaryId;
    private String repositoryLocation;
    private MercurialSynchronizeSubscriber subscriber = null;

    public MercurialSynchronizeParticipant(ISynchronizeScope scope,
            String repositoryLocation) {
        super(scope);
        this.repositoryLocation = repositoryLocation;
        subscriber = new MercurialSynchronizeSubscriber(scope,
                repositoryLocation);
        setSubscriber(subscriber);
        this.secondaryId = new Date().toString();        
    }

    @Override
    public void init(String secId, IMemento memento) throws PartInitException {
        super.init(secondaryId, memento);
        
        IMemento myMemento = memento.getChild(MercurialSynchronizeParticipant.class.getName());
        
        this.secondaryId = secId;
        this.repositoryLocation = myMemento.getString(REPOSITORY_LOCATION);
            
        subscriber = new MercurialSynchronizeSubscriber(getScope(), repositoryLocation);
        setSubscriber(subscriber);            
    }

    public MercurialSynchronizeParticipant() {
    }
    
    @Override
    public void saveState(IMemento memento) {        
        super.saveState(memento);
        IMemento myMemento = memento.createChild(MercurialSynchronizeParticipant.class.getName());
        myMemento.putString(REPOSITORY_LOCATION, repositoryLocation);
    }

    @Override
    protected void initializeConfiguration(
            final ISynchronizePageConfiguration configuration) {
        super.initializeConfiguration(configuration);

        MercurialSynchronizePageActionGroup syncPageActionGroup = new MercurialSynchronizePageActionGroup();
        configuration.addActionContribution(syncPageActionGroup);
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
    public IResource[] getResources() {
        return subscriber.roots();
    }

    @Override
    public String getName() {
        IResource[] resources = getScope().getRoots();
        String res = "";
        if (resources != null) {
            res.concat(" on resources: ");
            for (IResource resource : resources) {
                res.concat("\n\t" + resource.getName());
            }
        }
        return "Mercurial Synchronization".concat(res).concat("\nRepository: ")
                .concat(""+repositoryLocation);
    }

    @Override
    protected String getLongTaskName(IResource[] resources) {
        return "Mercurial: Refreshing resources for synchronization...";
    }

    /**
     * @return the repositoryLocation
     */
    public String getRepositoryLocation() {
        return repositoryLocation;
    }

}
