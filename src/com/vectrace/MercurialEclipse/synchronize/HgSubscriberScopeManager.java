/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.ISubscriberChangeEvent;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * @author Andrei
 */
public class HgSubscriberScopeManager extends SubscriberScopeManager implements Observer {

    public static final int INCOMING = -1;
    public static final int OUTGOING = -2;
    public static final int LOCAL = -3;


    public HgSubscriberScopeManager(ResourceMapping[] inputMappings, MercurialSynchronizeSubscriber subscriber) {
        super(HgSubscriberScopeManager.class.getSimpleName(), inputMappings, subscriber, false);
        MercurialStatusCache.getInstance().addObserver(this);
        IncomingChangesetCache.getInstance().addObserver(this);
        OutgoingChangesetCache.getInstance().addObserver(this);
        LocalChangesetCache.getInstance().addObserver(this);
    }

    public void update(Observable o, Object arg) {
        if(!(arg instanceof Set<?>)){
            return;
        }
        List<ISubscriberChangeEvent> changeEvents = new ArrayList<ISubscriberChangeEvent>();
        Set<?> resources = (Set<?>) arg;
        IResource[] roots = getSubscriber().roots();
        boolean projectRefresh = false;
        int flags = ISubscriberChangeEvent.SYNC_CHANGED;
        for (Object res : resources) {
            if(!(res instanceof IResource)) {
                continue;
            }
            IResource resource = (IResource)res;
            for (IResource root : roots) {
                if(root.contains(resource)) {
                    if(resource.contains(root)){
                        projectRefresh = true;
                    }
                    changeEvents.add(new SubscriberChangeEvent(getSubscriber(), flags, resource));
                    break;
                }
            }
        }

        if (changeEvents.size() == 0) {
            return;
        }

        if(resources.size() == 1 && projectRefresh){
            if(MercurialEclipsePlugin.getDefault().isDebugging()) {
                System.out.println("\n! Refresh from: " + o + " : " + resources.size() + "\n");
            }
            int flag = 0;
            if (o instanceof IncomingChangesetCache) {
                flag = INCOMING;
            } else if (o instanceof OutgoingChangesetCache || o instanceof LocalChangesetCache ) {
                flag = OUTGOING;
            } else if (o instanceof MercurialStatusCache) {
                flag = LOCAL;
            }
            try {
                ((MercurialSynchronizeSubscriber)getSubscriber()).refresh(roots, flag, new NullProgressMonitor());
            } catch (TeamException e) {
                MercurialEclipsePlugin.logError(e);
            }
        } else {
            if(MercurialEclipsePlugin.getDefault().isDebugging()) {
                System.out.println("\nRefresh from: " + o + " : " + resources.size() + "\n");
            }
            ISubscriberChangeEvent[] deltas = changeEvents.toArray(new ISubscriberChangeEvent[changeEvents.size()]);
            ((MercurialSynchronizeSubscriber)getSubscriber()).fireTeamResourceChange(deltas);
        }
    }


    @Override
    public void dispose() {
        MercurialStatusCache.getInstance().deleteObserver(this);
        IncomingChangesetCache.getInstance().deleteObserver(this);
        OutgoingChangesetCache.getInstance().deleteObserver(this);
        LocalChangesetCache.getInstance().deleteObserver(this);
        super.dispose();
    }

}
