/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.BitSet;

import org.eclipse.core.resources.IResource;

public class FlaggedResource {

    //relative order for folders
    public final static int BIT_IGNORE = 0;
    public final static int BIT_CLEAN = 1;
    public final static int BIT_DELETED = 2;
    public final static int BIT_REMOVED = 3;
    public final static int BIT_UNKNOWN = 4;
    public final static int BIT_ADDED = 5;
    public final static int BIT_MODIFIED = 6;
    public final static int BIT_IMPOSSIBLE = 7;

    private final IResource resource;//FIXME needed?
    private BitSet status;
    private boolean conflict = false;

    public FlaggedResource(IResource resource, BitSet status) {
        this.resource = resource;
        this.status = status;
    }

    //FIXME adaptable?
    public IResource getResource() {
        return resource;
    }

    public BitSet getStatus() {
        return status;
    }

    public BitSet combineStatus(BitSet otherStatus) {
        status = (BitSet) status.clone();
        status.or(otherStatus);
        return status;
    }

    public boolean isConflict() {
        return conflict;
    }

    public void setConflict(boolean conflict) {
        this.conflict = conflict;
    }

}
