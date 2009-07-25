/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class CommitResource {
    private final String statusMessage;
    private final char status;
    private final File path;
    private final IResource resource;

    private String convertStatus(char statusChar, String originalString) {
        switch (statusChar) {
        case MercurialStatusCache.CHAR_MODIFIED:
            return CommitDialog.FILE_MODIFIED;
        case MercurialStatusCache.CHAR_ADDED:
            return CommitDialog.FILE_ADDED;
        case MercurialStatusCache.CHAR_REMOVED:
            return CommitDialog.FILE_REMOVED;
        case MercurialStatusCache.CHAR_UNKNOWN:
            return CommitDialog.FILE_UNTRACKED;
        case MercurialStatusCache.CHAR_DELETED:
            return CommitDialog.FILE_DELETED;
        case MercurialStatusCache.CHAR_CLEAN:
            return CommitDialog.FILE_CLEAN;
        default:
            return "status error: " + originalString; //$NON-NLS-1$
        }
    }

    private char getStatusChar(String statusToken) {
        if(statusToken.length() == 0) {
            return 0;
        }
        return  statusToken.charAt(0);
    }

    public CommitResource(String statusString, IResource resource, File path) {
        this.status = getStatusChar(statusString);
        this.statusMessage = convertStatus(status, statusString);
        this.resource = resource;
        this.path = path;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * @return the status character, as defined by {@link MercurialStatusCache}, or
     * zero, if status was not the one we expected to see from hg
     */
    public char getStatus() {
        return status;
    }

    public IResource getResource() {
        return resource;
    }

    public File getPath() {
        return path;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CommitResource [status=");
        builder.append(statusMessage);
        builder.append(", path=");
        builder.append(path);
        builder.append(", resource=");
        builder.append(resource);
        builder.append("]");
        return builder.toString();
    }

}