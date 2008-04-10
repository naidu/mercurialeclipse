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

import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

public class MercurialResourceVariantComparator implements
		IResourceVariantComparator {
	private static MercurialResourceVariantComparator instance;

	private MercurialResourceVariantComparator() {
	}

	public static MercurialResourceVariantComparator getInstance() {
		if (instance == null) {
			instance = new MercurialResourceVariantComparator();
		}
		return instance;
	}

	public boolean compare(IResource local, IResourceVariant remote) {
		return new IStorageMercurialRevision(local).getName().equals(remote
				.getContentIdentifier());
	}

	public boolean compare(IResourceVariant base, IResourceVariant remote) {
		return base.getContentIdentifier()
				.equals(remote.getContentIdentifier());
	}

	public boolean isThreeWay() {
		return false;
	}

}
