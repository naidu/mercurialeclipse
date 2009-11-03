/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch           - additions for repository view
 *     Subclipse contributors    - fromProperties() initial source
 *     @author adam.berkes <adam.berkes@intland.com>
 *******************************************************************************/

package com.vectrace.MercurialEclipse.storage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.repository.model.AllRootsElement;

/**
 * A class abstracting a Mercurial repository location which may be either local
 * or remote.
 */
public class HgRepositoryLocation extends AllRootsElement implements  Comparable<HgRepositoryLocation> {

    private final String logicalName;
    private String projectName;
    private String location;
    private final URI uri;
    private final String user;
    private final String password;
    private final boolean isPush;
    private Date lastUsage;

    HgRepositoryLocation(String logicalName, boolean isPush, URI uri, String location, String user, String password) {
        this.logicalName = logicalName;
        this.isPush = isPush;
        this.uri = uri;
        this.user = user;
        this.password = password;
        this.location = location;
        if(uri != null) {
            try {
                this.location = new URI(uri.getScheme(),
                        null,
                        uri.getHost(),
                        uri.getPort(),
                        uri.getPath(),
                        null,
                        uri.getFragment()).toASCIIString();
            } catch (URISyntaxException ex) {
                MercurialEclipsePlugin.logError(ex);
            }
        }
    }

    static public boolean validateLocation(String validate) {
        return validate.trim().length() > 0;
        /*
         * TODO: Something like this would be nice, but it doesn't understand
         * ssh and allows several other protocols. try { URL url = new
         * URL(validate); } catch(MalformedURLException e) { return false; }
         * return true;
         */
    }

    public int compareTo(HgRepositoryLocation loc) {
        if(location == null){
            return -1;
        }
        if(loc.location == null){
            return 1;
        }
        return this.location.compareTo(loc.location);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((location == null) ? 0 : location.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HgRepositoryLocation)) {
            return false;
        }
        final HgRepositoryLocation other = (HgRepositoryLocation) obj;
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        return true;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        if (logicalName!= null && logicalName.length()>0) {
            return logicalName + " (" + location + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return location;
    }

    @Deprecated
    public String getSaveString() {
        return HgRepositoryLocationParser.createSaveString(this);
    }

    @Override
    public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
        return new HgRepositoryLocation[0];
    }

    @Override
    public ImageDescriptor getImageDescriptor(Object object) {
        return super.getImageDescriptor(object);
    }

    public URI getUri() {
        return uri;
    }

    public String getLocation() {
        return location;
    }

    /**
     * @return a location with password removed that is safe to display on screen
     */
    public String getDisplayLocation() {
        if (uri == null) {
            return this.location;
        }

        try {
            return (new URI(uri.getScheme(), user,
                    uri.getHost(), uri.getPort(), uri.getPath(),
                    uri.getQuery(), uri.getFragment())).toString();

        } catch (URISyntaxException e) {
            // This shouldn't happen at this point
            throw new RuntimeException(e);
        }
    }

    public String getLogicalName() {
        return logicalName;
    }

    /**
     * @return the lastUsage
     */
    public Date getLastUsage() {
        return lastUsage;
    }

    /**
     * @param lastUsage the lastUsage to set
     */
    public void setLastUsage(Date lastUsage) {
        this.lastUsage = lastUsage;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return the isPush
     */
    public boolean isPush() {
        return isPush;
    }
}
