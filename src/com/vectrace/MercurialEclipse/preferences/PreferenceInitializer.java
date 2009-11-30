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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
		detectAndSetHgExecutable(store);
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

	private void detectAndSetHgExecutable(IPreferenceStore store) {
		// Currently only tested on Windows. The binary is expected to be found
		// at "os\win32\x86\hg.exe" (relative to the plugin/fragment directory)
		boolean isWindows = File.separatorChar == '\\';
		IPath path = isWindows ? new Path("$os$/hg.exe") : new Path("$os$/hg");
		URL url = FileLocator.find(MercurialEclipsePlugin.getDefault().getBundle(), path, null);
		if(url != null){
			try {
				url = FileLocator.toFileURL(url);
				store.setDefault(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, new File(url.toURI()).getAbsolutePath());
			} catch (IOException e1) {
				MercurialEclipsePlugin.logError(e1);
				store.setDefault(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, "hg");
			} catch (URISyntaxException e) {
				MercurialEclipsePlugin.logError(e);
				store.setDefault(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, "hg");
			}
		} else {
			store.setDefault(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, "hg");
		}
	}

}
