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

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;

public class HgSubscriberChangeEvent extends SubscriberChangeEvent {

    public HgSubscriberChangeEvent(Subscriber subscriber, int flags, IResource resource) {
        super(subscriber, flags, resource);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj){
            return true;
        }
        if(!(obj instanceof HgSubscriberChangeEvent)){
            return false;
        }
        HgSubscriberChangeEvent event = (HgSubscriberChangeEvent) obj;

        return getResource().equals(event.getResource());
    }

    @Override
    public int hashCode() {
        return getResource().hashCode();
    }
}