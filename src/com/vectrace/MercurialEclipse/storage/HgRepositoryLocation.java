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
 *     Adam Berkes (Intland)     - bug fixes
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.repository.model.AllRootsElement;

/**
 * A class abstracting a Mercurial repository location which may be either local
 * or remote.
 */
public class HgRepositoryLocation extends AllRootsElement implements Comparable<HgRepositoryLocation> {

	private static final String PASSWORD_MASK = "***";

	private String logicalName;
	protected String location;
	private String user;
	private String password;

	/**
	 * hg repository which is represented by a bundle file (on local disk)
	 */
	public static class BundleRepository extends HgRepositoryLocation {

		/**
		 * @param location canonical representation of a bundle file path, never null
		 */
		public BundleRepository(File location) {
			super(null, null, null);
			this.location = location.getAbsolutePath();
		}

		@Override
		public URI getUri(boolean isSafe) throws HgException {
			return null;
		}
	}

	HgRepositoryLocation(String logicalName, String user, String password){
		this.logicalName = logicalName;
		this.user = user;
		this.password = password;
	}

	HgRepositoryLocation(String logicalName, String location, String user, String password) throws HgException {
		this(logicalName, user, password);
		URI uri = HgRepositoryLocationParser.parseLocationToURI(location, user, password);
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
		} else {
			this.location = location;
		}
	}

	HgRepositoryLocation(String logicalName, URI uri) throws HgException {
		this(logicalName, HgRepositoryLocationParser.getUserNameFromURI(uri),
				HgRepositoryLocationParser.getPasswordFromURI(uri));
		if (uri == null) {
			throw new HgException("Given URI cannot be null");
		}
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

	static public boolean validateLocation(String validate) {
		try {
			HgRepositoryLocation location2 = HgRepositoryLocationParser.parseLocation(validate, null, null);
			if(location2 == null){
				return false;
			}
			return location2.getUri() != null || (location2.getLocation() != null &&
					new File(location2.getLocation()).isDirectory());
		} catch (HgException ex) {
			MercurialEclipsePlugin.logError(ex);
			return false;
		}
	}

	public int compareTo(HgRepositoryLocation loc) {
		if(getLocation() == null) {
			return -1;
		}
		if(loc.getLocation() == null){
			return 1;
		}
		return getLocation().compareTo(loc.getLocation());
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

	/**
	 * Return unsafe (with password) URI for repository location if possible
	 * @return a valid URI of the repository or null if repository is local directory
	 * @throws HgException unable to parse to URI or location is invalid.
	 */
	public URI getUri() throws HgException {
		return getUri(false);
	}

	/**
	 * Return URI for repository location if possible
	 * @param isSafe add password to userinfo if false or add a mask instead
	 * @return a valid URI of the repository or null if repository is local directory
	 * @throws HgException unable to parse to URI or location is invalid.
	 */
	public URI getUri(boolean isSafe) throws HgException {
		return HgRepositoryLocationParser.parseLocationToURI(getLocation(), getUser(), isSafe ? PASSWORD_MASK : getPassword());
	}

	@Override
	public String toString() {
		if (logicalName!= null && logicalName.length()>0) {
			return logicalName + " (" + location + ")";
		}
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

	public String getLocation() {
		return location;
	}

	/**
	 * @return a location with password removed that is safe to display on screen
	 */
	public String getDisplayLocation() {
		try {
			URI uri = getUri(true);
			if (uri != null) {
				return uri.toString();
			}
		} catch (HgException ex) {
			// This shouldn't happen at this point
			throw new RuntimeException(ex);
		}
		return getLocation();
	}

	public String getLogicalName() {
		return logicalName;
	}

	public boolean isEmpty() {
		return (getLocation() == null || getLocation().length() == 0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == HgRepositoryLocation.class){
			return this;
		}
		return super.getAdapter(adapter);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setLogicalName(String logicalName) {
		this.logicalName = logicalName;
	}
}
