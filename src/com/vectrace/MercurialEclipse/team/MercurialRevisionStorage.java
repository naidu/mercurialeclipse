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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

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
public class MercurialRevisionStorage implements IStorage {
    private int revision;
    private String global;
    private final IResource resource;
    protected ChangeSet changeSet;
    protected byte [] bytes;
    private Exception error;
    private File parent;

    /**
     * The recommended constructor to use is MercurialRevisionStorage(IResource res, String rev, String global,
     * ChangeSet cs)
     *
     */
    public MercurialRevisionStorage(IResource res, String changeset) {
        super();
        resource = res;
        try {
            this.changeSet = LocalChangesetCache.getInstance().getLocalChangeSet(res, changeset, false);
            if(changeSet != null){
                this.revision = changeSet.getChangesetIndex();
                this.global = changeSet.getChangeset();
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    /**
     * Constructs a new MercurialRevisionStorage with the given params.
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
    public MercurialRevisionStorage(IResource res, int rev, String global, ChangeSet cs) {
        super();
        this.revision = rev;
        this.global = global;
        this.resource = res;
        this.changeSet = cs;
    }

    /**
     * Constructs an {@link MercurialRevisionStorage} with the newest local changeset available.
     *
     * @param res
     *            the resource
     */
    public MercurialRevisionStorage(IResource res) {
        super();
        this.resource = res;
        ChangeSet cs = null;
        try {
            cs = LocalChangesetCache.getInstance().getCurrentWorkDirChangeset(res);
            this.revision = cs.getChangesetIndex(); // should be fetched
            // from id
            this.global = cs.getChangeset();
            this.changeSet = cs;
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    /**
     * @param parent the parent (ancestor file name before rename/copy), might be null
     */
    public void setParent(File parent) {
        this.parent = parent;
    }

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
        String result;
        try {
            IFile file = resource.getProject().getFile(resource.getProjectRelativePath());
            result = fetchStringContent(file);
        } catch (CoreException e) {

            if(parent != null){
                IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
                        new Path(parent.getAbsolutePath()));
                try {
                    result = fetchStringContent(file);
                    return createStreamContent(result);
                } catch (CoreException e2) {
                    e = e2;
                }
            }
            error = e;
            bytes = new byte[0];
            // core API ignores exceptions from this method, so we need to log them here
            MercurialEclipsePlugin.logWarning("Failed to get revision content for " + toString(), e);
            throw e;
        }

        return createStreamContent(result);
    }

    private InputStream createStreamContent(String result) throws HgException {
        try {
            HgRoot root = MercurialTeamProvider.getHgRoot(resource);
            bytes = result.getBytes(root.getEncoding().name());
            return new ByteArrayInputStream(bytes);
        } catch (UnsupportedEncodingException e) {
            error = e;
            bytes = new byte[0];
            // core API ignores exceptions from this method, so we need to log them here
            MercurialEclipsePlugin.logWarning("Failed to get revision content for " + toString(), e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    private String fetchStringContent(IFile file) throws CoreException {
        if (changeSet == null) {
            // no changeset known
            return HgCatClient.getContent(file, null);
        }
        String result;
        // Setup and run command
        if (changeSet.getDirection() == Direction.INCOMING && changeSet.getBundleFile() != null) {
            // incoming: overlay repository with bundle and extract then via cat
            try {
                result = HgCatClient.getContentFromBundle(file,
                        Integer.valueOf(changeSet.getChangesetIndex()).toString(),
                        changeSet.getBundleFile().getCanonicalPath());
            } catch (IOException e) {
                throw new HgException("Unable to determine canonical path for " + changeSet.getBundleFile(), e);
            }
        } else if (changeSet.getDirection() == Direction.OUTGOING) {
            bytes = PatchUtils.getPatchedContentsAsBytes(file, changeSet.getPatches(), true);
            result = new ByteArrayInputStream(bytes).toString();
        } else {
            // local: get the contents via cat
            result = HgCatClient.getContent(file, Integer.valueOf(changeSet.getChangesetIndex()).toString());
        }
        return result;
    }

    public IPath getFullPath() {
        return resource.getFullPath().append(revision != 0 ? (" [" + revision + "]") //$NON-NLS-1$ //$NON-NLS-2$
                : Messages.getString("MercurialRevisionStorage.parentChangeset")); //$NON-NLS-1$
    }

    public String getName() {
        // the getContents() call below is a workaround for the fact that the core API
        // seems to ignore the failures in getContents() and do not update the storage name
        // in this case. So we just call getContents (which result is buffered in any case)
        // here, and remember the possible error.
        try {
            getContents();
        } catch (CoreException e) {
            error = e;
            bytes = new byte[0];
        }
        String name;
        if (changeSet != null) {
            name = resource.getName() + " [" + changeSet.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        } else {
            name = resource.getName();
        }
        if(error != null){
            String message = error.getMessage();
            if (message.indexOf('\n') > 0) {
                // name = message + ", " + name;
                name = message.substring(0, message.indexOf('\n'));
            } else {
                name = message;
            }
        }
        return name;
    }

    public int getRevision() {
        return revision;
    }

    /**
     * You can't write to other revisions then the current selected e.g. ReadOnly
     */
    public boolean isReadOnly() {
        if (revision != 0) {
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
    public MercurialRevisionStorage(IResource res, int rev) {
        this(res, String.valueOf(rev));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Hg revision [");
        if (changeSet != null) {
            builder.append("changeSet=");
            builder.append(changeSet);
            builder.append(", ");
        }
        if (revision != 0) {
            builder.append("revision=");
            builder.append(revision);
            builder.append(", ");
        }
        if (global != null) {
            builder.append("global=");
            builder.append(global);
            builder.append(", ");
        }
        if (resource != null) {
            builder.append("resource=");
            builder.append(resource);
            builder.append(", ");
        }
        if (parent != null) {
            builder.append("parent=");
            builder.append(parent);
            builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }
}
