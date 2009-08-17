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

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

/**
 * @author zingo
 *
 */
public class MercurialRevision extends FileRevision {

    private final IResource resource;
    private final ChangeSet changeSet;

    /** Cached data */
    private MercurialRevisionStorage mercurialRevisionStorage;
    private final GChangeSet gChangeSet;
    private final int revision;
    private String hash;
    private final Signature signature;
    private File parent;

    public MercurialRevision(ChangeSet changeSet, GChangeSet gChangeSet,
            IResource resource, Signature sig) {
        super();
        this.changeSet = changeSet;
        this.gChangeSet = gChangeSet;
        this.revision = changeSet.getChangesetIndex();
        this.hash = changeSet.getChangeset();
        this.resource = resource;
        this.signature = sig;
    }

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



    public ChangeSet getChangeSet() {
        return changeSet;
    }

    public GChangeSet getGChangeSet() {
        return gChangeSet;
    }

    public String getName() {
        return resource.getName();
    }

    @Override
    public String getContentIdentifier() {
        return changeSet.getChangeset();
    }

    public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
        if (mercurialRevisionStorage == null) {
            if((resource == null || !resource.exists()) && parent != null){
                IResource parentRes =  ResourcesPlugin.getWorkspace().getRoot()
                    .getFileForLocation(new Path(parent.getAbsolutePath()));
                mercurialRevisionStorage = new MercurialRevisionStorage(parentRes,
                        revision, hash, changeSet);
            } else {
                mercurialRevisionStorage = new MercurialRevisionStorage(resource,
                        revision, hash, changeSet);
            }
        }
        return mercurialRevisionStorage;
    }

    public boolean isPropertyMissing() {
        return false;
    }

    public IFileRevision withAllProperties(IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    /**
     * @return the revision
     */
    public int getRevision() {
        return revision;
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

    /**
     * @return the possible parent (after the copy or rename operation), may be null
     */
    public File getParent() {
        return parent;
    }

    /**
     * @param parent the possible parent (after the copy or rename operation)
     */
    public void setParent(File parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("hg rev [");
        if (revision > 0) {
            builder.append("revision=");
            builder.append(revision);
            builder.append(", ");
        }
        if (changeSet != null) {
            builder.append("changeSet=");
            builder.append(changeSet);
            builder.append(", ");
        }
        if (resource != null) {
            builder.append("resource=");
            builder.append(resource);
            builder.append(", ");
        }
        if (signature != null) {
            builder.append("signature=");
            builder.append(signature);
            builder.append(", ");
        }
        if (gChangeSet != null) {
            builder.append("gChangeSet=");
            builder.append(gChangeSet);
        }
        if (parent != null) {
            builder.append("parent=");
            builder.append(parent);
        }
        builder.append("]");
        return builder.toString();
    }


}
