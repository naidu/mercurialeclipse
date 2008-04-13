/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;

public class MercurialResourceVariantComparator implements
		IResourceVariantComparator {

	private static MercurialResourceVariantComparator instance;
	private static MercurialStatusCache statusCache = MercurialStatusCache
			.getInstance();

	private MercurialResourceVariantComparator() {
	}

	public static MercurialResourceVariantComparator getInstance() {
		if (instance == null) {
			instance = new MercurialResourceVariantComparator();
		}
		return instance;
	}

	public boolean compare(IResource local, IResourceVariant remote) {
		if (statusCache.getStatus(local) != null) {
			int status = statusCache.getStatus(local).length() - 1;
			if (status == MercurialStatusCache.BIT_CLEAN) {
				String localVersion = "0:unknown";
				String remoteVersion = "0:unknown";
				try {
					ChangeSet cs = statusCache.getNewestLocalChangeSet(local);

					if (cs == null) {
						return false;
					}
					localVersion = cs.getChangeset();
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
				}
				remoteVersion = remote.getContentIdentifier();
				boolean equal = localVersion.equals(remoteVersion);
				return equal;
			}
		}
//		try {
//			System.out.println("Local differs from remote:"
//					+ local.getName() +","
//					+ statusCache.getStatus(local) + ","
//					+ statusCache.getNewestLocalChangeSet(local).getChangeset() + ","
//					+ remote.getContentIdentifier());
//		} catch (HgException e) {
//			MercurialEclipsePlugin.logError(e);
//		}
		return false;

	}

	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		return base.getContentIdentifier()
				.equals(remote.getContentIdentifier());
	}

	public boolean isThreeWay() {
		return true;
	}

}
