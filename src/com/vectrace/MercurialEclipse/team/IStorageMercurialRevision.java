/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch           - additions for sync
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCatClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.PatchUtils;

/**
 * @author zingo
 *
 *         This is a IStorage subclass that can handle file revision
 *
 */
public class IStorageMercurialRevision implements IStorage {
    private String revision;
    private String global;
    private final IResource resource;
    private ChangeSet changeSet;
    private HgRoot root;
    private byte [] bytes;

    /**
     * The recommended constructor to use is IStorageMercurialRevision(IResource res, String rev, String global,
     * ChangeSet cs)
     *
     */
    public IStorageMercurialRevision(IResource res, String changeset) {
        super();
        resource = res;
        try {
            ChangeSet cs = LocalChangesetCache.getInstance().getLocalChangeSet(res, changeset);
            this.changeSet = cs;
            this.revision = String.valueOf(cs.getChangesetIndex());
            this.global = cs.getChangeset();
        } catch (NumberFormatException e) {
            MercurialEclipsePlugin.logError(e);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    /**
     * Constructs a new IStorageMercurialRevision with the given params.
     *
     * @param res
     *            the resource for which we want an IStorage revision
     * @param rev
     *            the changeset index as string
     * @param global
     *            the global hash identifier
     * @param cs
     *            the changeset object
     */
    public IStorageMercurialRevision(IResource res, String rev, String global, ChangeSet cs) {
        super();
        this.revision = rev;
        this.global = global;
        this.resource = res;
        this.changeSet = cs;
    }

    /**
     * Constructs an {@link IStorageMercurialRevision} with the newest local changeset available.
     *
     * @param res
     *            the resource
     */
    public IStorageMercurialRevision(IResource res) {
        super();
        this.resource = res;
        ChangeSet cs = null;
        try {
            cs = LocalChangesetCache.getInstance().getCurrentWorkDirChangeset(res);
            this.revision = cs.getChangesetIndex() + ""; // should be fetched //$NON-NLS-1$
            // from id
            this.global = cs.getChangeset();
            this.changeSet = cs;
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        if (adapter.equals(IResource.class)) {
            return resource;
        }
        return null;
    }

    /**
     * Generate data content of the so called "file" in this case a revision, e.g. a hg cat --rev "rev" %file%
     * <p>
     * {@inheritDoc}
     */
    public InputStream getContents() throws CoreException {
        if(bytes != null){
            return new ByteArrayInputStream(bytes);
        }
        // Setup and run command
        String result = ""; //$NON-NLS-1$
        root = MercurialTeamProvider.getHgRoot(resource);
        IFile file = resource.getProject().getFile(resource.getProjectRelativePath());
        if (changeSet != null) {

            // incoming: overlay repository with bundle and extract then via cat
            if (changeSet.getDirection() == Direction.INCOMING && changeSet.getBundleFile() != null) {

                String bundleFile = null;
                try {
                    bundleFile = changeSet.getBundleFile().getCanonicalFile().getCanonicalPath();
                } catch (IOException e) {
                    throw new CoreException(new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, e.getMessage(), e));
                }
                if (bundleFile != null) {
                    result = HgCatClient.getContentFromBundle(file, changeSet.getChangesetIndex() + "", bundleFile); //$NON-NLS-1$
                }

            } else if (changeSet.getDirection() == Direction.OUTGOING) {
                bytes = PatchUtils.getPatchedContentsAsBytes(file, changeSet.getPatches(), true);
                return new ByteArrayInputStream(bytes);
            } else {
                // local: get the contents via cat
                result = HgCatClient.getContent(file, changeSet.getChangesetIndex() + ""); //$NON-NLS-1$
            }
        } else {
            // no changeset known
            result = HgCatClient.getContent(file, null);
        }

        try {
            bytes = result.getBytes(root.getEncoding().name());
            return new ByteArrayInputStream(bytes);
        } catch (UnsupportedEncodingException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /*
     * (non-Javadoc)setContents(
     *
     * @see org.eclipse.core.resources.IStorage#getFullPath()
     */
    public IPath getFullPath() {
        return resource.getFullPath().append(revision != null ? (" [" + revision + "]") //$NON-NLS-1$ //$NON-NLS-2$
                : Messages.getString("IStorageMercurialRevision.parentChangeset")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IStorage#getName()
     */
    public String getName() {
        String name;
        if (changeSet != null) {
            name = resource.getName() + " [" + changeSet.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            name = resource.getName();
        }
        return name;
    }

    public String getRevision() {
        return revision;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IStorage#isReadOnly()
     *
     * You can't write to other revisions then the current selected e.g. ReadOnly
     */
    public boolean isReadOnly() {
        if (revision != null) {
            return true;
        }
        // if no revision resource is the current one e.g. editable :)
        ResourceAttributes attributes = resource.getResourceAttributes();
        if (attributes != null) {
            return attributes.isReadOnly();
        }
        return true; /* unknown state marked as read only for safety */
    }

    public IResource getResource() {
        return resource;
    }

    public String getGlobal() {
        return global;
    }

    /**
     * @return the changeSet
     */
    public ChangeSet getChangeSet() {
        return changeSet;
    }

    /**
     * This constructor is not recommended, as the revision index is not unique when working with other than the local
     * repository.
     *
     * @param res
     * @param rev
     */
    public IStorageMercurialRevision(IResource res, int rev) {
        this(res, String.valueOf(rev));
    }
}
