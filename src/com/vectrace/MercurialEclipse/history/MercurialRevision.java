/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError 
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

/**
 * @author zingo
 * 
 */
public class MercurialRevision extends FileRevision {
    

    public Signature getSignature() {
        return signature;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((changeSet == null) ? 0 : changeSet.hashCode());
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MercurialRevision)) {
            return false;
        }
        MercurialRevision other = (MercurialRevision) obj;
        if (changeSet == null) {
            if (other.changeSet != null) {
                return false;
            }
        } else if (!changeSet.equals(other.changeSet)) {
            return false;
        }
        if (resource == null) {
            if (other.resource != null) {
                return false;
            }
        } else if (!resource.equals(other.resource)) {
            return false;
        }
        return true;
    }

    private IResource resource;
    private ChangeSet changeSet;
    private IStorageMercurialRevision iStorageMercurialRevision; // Cached data
    private final GChangeSet gChangeSet;
    private String revision;
    private String hash;
    private Signature signature;
    
    public MercurialRevision(ChangeSet changeSet, GChangeSet gChangeSet,
            IResource resource, Signature sig) {
        super();
        this.changeSet = changeSet;
        this.gChangeSet = gChangeSet;

        this.revision = changeSet.getChangesetIndex() + ""; //$NON-NLS-1$
        this.hash = changeSet.getChangeset();
        this.resource = resource;
        this.signature = sig;
    }

    public ChangeSet getChangeSet() {
        return changeSet;
    }

    public GChangeSet getGChangeSet() {
        return gChangeSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.history.IFileRevision#getName()
     */
    public String getName() {
        return resource.getName();
    }

    @Override
    public String getContentIdentifier() {
        return changeSet.getChangeset();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.history.IFileRevision#getStorage(org.eclipse.core
     * .runtime.IProgressMonitor)
     */
    public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
        if (iStorageMercurialRevision == null) {
            iStorageMercurialRevision = new IStorageMercurialRevision(resource,
                    revision, hash, changeSet);
        }
        return iStorageMercurialRevision;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.history.IFileRevision#isPropertyMissing()
     */
    public boolean isPropertyMissing() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.history.IFileRevision#withAllProperties(org.eclipse
     * .core.runtime.IProgressMonitor)
     */
    public IFileRevision withAllProperties(IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * @param revision
     *            the revision to set
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * @param hash
     *            the hash to set
     */
    public void setHash(String hash) {
        this.hash = hash;
    }

    public IResource getResource() {
        return resource;
    }

}
