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

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

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
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		detectAndSetHgExecutable(store);

		// "Highest" importance should be default, like "merge conflict"
		// when having 2 different statuses in a folder it should have the more important one
		store.setDefault(LABELDECORATOR_LOGIC, LABELDECORATOR_LOGIC_HB);

		store.setDefault(RESOURCE_DECORATOR_COMPUTE_DEEP_STATUS, true);
		store.setDefault(RESOURCE_DECORATOR_SHOW_CHANGESET, false);
		store.setDefault(RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET, false);

		store.setDefault(LOG_BATCH_SIZE, 200);
		store.setDefault(STATUS_BATCH_SIZE, 10);
		store.setDefault(COMMIT_MESSAGE_BATCH_SIZE, 10);
		store.setDefault(ENABLE_FULL_GLOG, true);

		// blue
		store.setDefault(PREF_CONSOLE_COMMAND_COLOR, "0,0,255"); //$NON-NLS-1$
		// black
		store.setDefault(PREF_CONSOLE_MESSAGE_COLOR, "0,0,0"); //$NON-NLS-1$
		// red
		store.setDefault(PREF_CONSOLE_ERROR_COLOR, "255,0,0"); //$NON-NLS-1$

		store.setDefault(PREF_DECORATE_WITH_COLORS, true);
		store.setDefault(PREF_SHOW_COMMENTS, true);
		store.setDefault(PREF_SHOW_PATHS, true);
		store.setDefault(PREF_SHOW_ALL_TAGS, true);
		store.setDefault(PREF_AFFECTED_PATHS_LAYOUT, LAYOUT_HORIZONTAL);
		store.setDefault(PREF_SIGCHECK_IN_HISTORY, false);
		store.setDefault(PREF_AUTO_SHARE_PROJECTS, true);

		// must be last entry as it causes some hg activity and so reads from the preferences
		store.setDefault(MERCURIAL_USERNAME, MercurialUtilities.getHGUsername());
	}

	private void detectAndSetHgExecutable(IPreferenceStore store) {
		// Currently only tested on Windows. The binary is expected to be found
		// at "os\win32\x86\hg.exe" (relative to the plugin/fragment directory)
		boolean isWindows = File.separatorChar == '\\';
		IPath path = isWindows ? new Path("$os$/hg.exe") : new Path("$os$/hg"); //$NON-NLS-1$ //$NON-NLS-2$
		URL url = FileLocator.find(MercurialEclipsePlugin.getDefault().getBundle(), path, null);
		if(url != null){
			try {
				url = FileLocator.toFileURL(url);
				store.setDefault(MERCURIAL_EXECUTABLE, new File(url.toURI()).getAbsolutePath());
			} catch (IOException e1) {
				MercurialEclipsePlugin.logError(e1);
				store.setDefault(MERCURIAL_EXECUTABLE, "hg"); //$NON-NLS-1$
			} catch (URISyntaxException e) {
				MercurialEclipsePlugin.logError(e);
				store.setDefault(MERCURIAL_EXECUTABLE, "hg"); //$NON-NLS-1$
			}
		} else {
			store.setDefault(MERCURIAL_EXECUTABLE, "hg"); //$NON-NLS-1$
		}
	}

}
