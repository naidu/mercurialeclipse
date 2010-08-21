/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author johnpeb
 */
public enum PresentationMode {
	FLAT(Messages.getString("PresentationMode.Flat")), //
	TREE(Messages.getString("PresentationMode.Tree")), //
	COMPRESSED_TREE(Messages.getString("PresentationMode.CompressedTree"));

	public static final String CONFIG_KEY = MercurialEclipsePlugin.ID + ".syncPresentationMode";

	private final String localized;

	PresentationMode(String localized) {
		if (localized == null) {
			throw new IllegalStateException();
		}
		this.localized = localized;
	}

	@Override
	public String toString() {
		return localized;
	}

	public static PresentationMode get(ISynchronizePageConfiguration configuration) {
		String name = (String) configuration.getProperty(CONFIG_KEY);

		if (name != null) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException t) {
			}
		}

		return FLAT;
	}

	public void set(ISynchronizePageConfiguration configuration) {
		configuration.setProperty(CONFIG_KEY, name());
	}

	public boolean isSet(ISynchronizePageConfiguration configuration) {
		return get(configuration) == this;
	}
}
