/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Jérôme Nègre              - adding label decorator section
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		store.setDefault(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, "hg"); //$NON-NLS-1$
		store.setDefault(MercurialPreferenceConstants.MERCURIAL_USERNAME, System.getProperty ( "user.name" )); //$NON-NLS-1$

		// Andrei: not really sure why it was ever set to "modified" as default.
		// "Highest" importance should be default, like "merge conflict"
		// when having 2 different statuses in a folder it should have the more important one
		store.setDefault(MercurialPreferenceConstants.LABELDECORATOR_LOGIC, MercurialPreferenceConstants.LABELDECORATOR_LOGIC_HB);

		store.setDefault(MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPLETE_STATUS, false);
		store.setDefault(MercurialPreferenceConstants.RESOURCE_DECORATOR_COMPUTE_DEEP_STATUS, true);
		store.setDefault(MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET, false);
		store.setDefault(MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET, false);

		store.setDefault(MercurialPreferenceConstants.LOG_BATCH_SIZE, 200);
		store.setDefault(MercurialPreferenceConstants.STATUS_BATCH_SIZE, 10);
		store.setDefault(MercurialPreferenceConstants.COMMIT_MESSAGE_BATCH_SIZE, 10);

		// blue
		store.setDefault(MercurialPreferenceConstants.PREF_CONSOLE_COMMAND_COLOR, "0,0,255");
		// black
		store.setDefault(MercurialPreferenceConstants.PREF_CONSOLE_MESSAGE_COLOR, "0,0,0");
		// red
		store.setDefault(MercurialPreferenceConstants.PREF_CONSOLE_ERROR_COLOR, "255,0,0");

		store.setDefault(MercurialPreferenceConstants.PREF_DECORATE_WITH_COLORS, true);

		/*
		store.setDefault(PreferenceConstants.P_CHOICE, "choice2");
		store.setDefault(PreferenceConstants.P_STRING,"Default value");
		 */
	}

}
