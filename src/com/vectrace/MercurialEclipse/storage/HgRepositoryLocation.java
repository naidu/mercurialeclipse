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
 *******************************************************************************/

package com.vectrace.MercurialEclipse.storage;

import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.repository.model.AllRootsElement;

/*
 * A class abstracting a Mercurial repository location which may be either local
 * or remote.
 */
public class HgRepositoryLocation extends AllRootsElement implements Comparable<HgRepositoryLocation> {
    private String location;
    private String user;
    private String password;

    public HgRepositoryLocation(String url) {
        this.location = url;
    }
    
    public HgRepositoryLocation(String url, String user, String password){
        this(url);
        this.user=user;
        this.password=password;
    }

    public String getUrl() {
        return this.location;
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
        return this.location.compareTo(loc.location);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((location == null) ? 0 : location.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof HgRepositoryLocation))
            return false;
        final HgRepositoryLocation other = (HgRepositoryLocation) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        return true;
    }
    
    /**
     * Create a repository location instance from the given properties.
     * The supported properties are:
     *   user The username for the connection (optional)
     *   password The password used for the connection (optional)
     *   url The url where the repository resides
     *   rootUrl The repository root url
     */
    public static HgRepositoryLocation fromProperties(Properties configuration)
        throws HgException {
        // We build a string to allow validation of the components that are provided to us
    
        String user = configuration.getProperty("user");  
        if ((user == null) || (user.length() == 0))
            user = null;
        String password = configuration.getProperty("password");  
        if (user == null)
            password = null;
        String rootUrl = configuration.getProperty("rootUrl");
        if ((rootUrl == null) || (rootUrl.length() == 0))
            rootUrl = null;
        String url = configuration.getProperty("url");  
        if (url == null)
            throw new HgException("URL must not be null.");  
            
        
        return new HgRepositoryLocation(url, user, password);
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return location;
    }
    
    @Override
    public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
        return new HgRepositoryLocation[0];
    }
    
    @Override
    public ImageDescriptor getImageDescriptor(Object object) {        
        return super.getImageDescriptor(object);
    }
    
}
