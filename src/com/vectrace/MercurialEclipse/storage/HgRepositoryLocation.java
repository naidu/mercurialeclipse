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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.repository.model.AllRootsElement;

/*
 * A class abstracting a Mercurial repository location which may be either local
 * or remote.
 */
public class HgRepositoryLocation extends AllRootsElement implements
        Comparable<HgRepositoryLocation> {
    private String location;
    private String user;
    private String password;
    private URI uri;
    private static final String SPLIT_TOKEN = "@@@"; //$NON-NLS-1$

    HgRepositoryLocation(String uri) throws URISyntaxException {
        this(uri, null, null);
    }

    HgRepositoryLocation(String uri, String user, String password)
            throws URISyntaxException {
        this.location = uri;
        String[] repoInfo = uri.split(SPLIT_TOKEN);
        
        this.user = user;
        this.password = password;
        
        if ((this.user == null || this.user.length() == 0)
                && repoInfo.length > 1) {
            this.user = repoInfo[1];
            this.location = repoInfo[0];
        }
        
        URI myUri = null;
        try {
            myUri = new URI(location);
        } catch (URISyntaxException e) {

            // do nothing. workaround below doesn't work :-(

            // this creates an URI like file:/c:/hurz
            // myUri = new File(uri).toURI();
            // normalize
            // myUri = myUri.normalize();
            // adding two slashes for Mercurial => file:///c:/hurz
            // see http://www.selenic.com/mercurial/bts/issue1153
            // myUri = new URI(myUri.toASCIIString().substring(0, 5) + "//"
            // + myUri.toASCIIString().substring(5));
        }        
        if (myUri != null) {
            if (myUri.getScheme() != null
                    && !myUri.getScheme().equalsIgnoreCase("file")) { //$NON-NLS-1$
                String userInfo = null;
                if (myUri.getUserInfo() == null) {
                    userInfo = createUserinfo(this.user, this.password);
                } else {
                    // extract user and password from given URI
                    String[] authorization = myUri.getUserInfo().split(":"); //$NON-NLS-1$
                    this.user = authorization[0];
                    if (authorization.length > 1) {
                        this.password = authorization[1];
                    }
                    userInfo = createUserinfo(this.user, this.password);
                }
                this.uri = new URI(myUri.getScheme(), userInfo,
                        myUri.getHost(), myUri.getPort(), myUri.getPath(),
                        myUri.getQuery(), myUri.getFragment());
            }
            this.location = new URI(myUri.getScheme(), myUri.getHost(), myUri
                    .getPath(), myUri.getFragment()).toASCIIString();
        }
    }

    /**
     * @param user
     * @param password
     * @return
     */
    private String createUserinfo(String user, String password) {
        String userInfo = null;
        if (user != null && user.length() > 0) {
            // pass gotta be separated by a colon
            userInfo = user;
            if (password != null && password.length() != 0) {
                userInfo = userInfo.concat(":").concat(password); //$NON-NLS-1$
            }
        }
        return userInfo;
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
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

    /**
     * Create a repository location instance from the given properties. The
     * supported properties are: user The username for the connection (optional)
     * password The password used for the connection (optional) url The url
     * where the repository resides rootUrl The repository root url
     * 
     * @throws MalformedURLException
     */
    public static HgRepositoryLocation fromProperties(Properties configuration)
            throws HgException, URISyntaxException {

        String user = configuration.getProperty("user"); //$NON-NLS-1$
        if ((user == null) || (user.length() == 0)) {
            user = null;
        }
        String password = configuration.getProperty("password"); //$NON-NLS-1$
        if (user == null) {
            password = null;
        }
        String rootUrl = configuration.getProperty("rootUrl"); //$NON-NLS-1$
        if ((rootUrl == null) || (rootUrl.length() == 0)) {
            rootUrl = null;
        }
        String url = configuration.getProperty("url"); //$NON-NLS-1$
        if (url == null) {
            throw new HgException(Messages.getString("HgRepositoryLocation.urlMustNotBeNull")); //$NON-NLS-1$
        }
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

    public String getSaveString() {
        String r = location;
        if (uri != null && uri.getUserInfo() != null) {
            r += SPLIT_TOKEN + uri.getUserInfo();
        }
        return r;
    }

    @Override
    public Object[] internalGetChildren(Object o, IProgressMonitor monitor) {
        return new HgRepositoryLocation[0];
    }

    @Override
    public ImageDescriptor getImageDescriptor(Object object) {
        return super.getImageDescriptor(object);
    }

    /**
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

}
