/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - some updates
 *     Stefan Groschupf          - logError
 *     Bastian Doetsch           - updates, cleanup and documentation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.Team;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgConfigClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.utils.IniFile;

/**
 * Class that offers Utility methods for working with the plug-in.
 *
 * @author zingo
 *
 */
public class MercurialUtilities {
	private final static boolean isWindows = File.separatorChar == '\\';

	/**
	 * This class is full of utilities metods, useful allover the place
	 */
	private MercurialUtilities() {
		// don't call me
	}

	public static boolean isWindows() {
		return isWindows;
	}

	/**
	 * Determines if the configured Mercurial executable can be called.
	 *
	 * @return true if no error occurred while calling the executable, false otherwise
	 */
	public static boolean isHgExecutableCallable() {
		try {
			Runtime.getRuntime().exec(getHGExecutable());
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns the hg executable stored in the plug-in preferences. If it's not defined, "hg" is
	 * returned as default.
	 *
	 * @return the path to the executable or, if not defined "hg"
	 */
	public static String getHGExecutable() {
		return HgClients.getPreference(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, "hg"); //$NON-NLS-1$
	}

	/**
	 * Fetches a preference from the plug-in's preference store. If no preference could be found in
	 * the store, the given default is returned.
	 *
	 * @param preferenceConstant
	 *            the string identifier for the constant.
	 * @param defaultIfNotSet
	 *            the default to return if no preference was found.
	 * @return the preference or the default
	 */
	public static String getPreference(String preferenceConstant, String defaultIfNotSet) {
		IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		// This returns "" if not defined
		String pref = preferenceStore.getString(preferenceConstant);

		if (pref.length() > 0) {
			return pref;
		}
		return defaultIfNotSet;
	}

	/**
	 * Gets the configured executable if it's callable (@see
	 * {@link MercurialUtilities#isHgExecutableCallable()}.
	 *
	 * If configureIfMissing is set to true, the configuration will be started and afterwards the
	 * executable stored in the preferences will be checked if it is callable. If true, it is
	 * returned, else "hg" will be returned. If the parameter is set to false, it will returns "hg"
	 * if no preference is set.
	 *
	 * @param configureIfMissing
	 *            flag if configuration should be started if hg is not callable.
	 * @return the hg executable path
	 */
	public static String getHGExecutable(boolean configureIfMissing) {
		if (isHgExecutableCallable()) {
			return getHGExecutable();
		}
		if (configureIfMissing) {
			configureHgExecutable();
			return getHGExecutable();
		}
		return "hg"; //$NON-NLS-1$
	}

	/**
	 * Checks the GPG Executable is callable and returns it if it is.
	 *
	 * Otherwise, if configureIfMissing is set to true, configuration will be started and the new
	 * command is tested for callability. If there's no preference found after configuration, "gpg"
	 * will be returned as default.
	 *
	 * @param configureIfMissing
	 *            flag, if configuration should be started if gpg is not callable.
	 * @return the gpg executable path
	 */
	public static String getGpgExecutable(boolean configureIfMissing) {
		if (isGpgExecutableCallable()) {
			return getGpgExecutable();
		}
		if (configureIfMissing) {
			configureGpgExecutable();
			return getGpgExecutable();
		}
		return "gpg"; //$NON-NLS-1$
	}

	/**
	 * Starts configuration for Gpg executable by opening the preference page.
	 */
	public static void configureGpgExecutable() {
		configureHgExecutable();
	}

	private static boolean isGpgExecutableCallable() {
		try {
			Runtime.getRuntime().exec(getGpgExecutable());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Returns the executable for gpg. If it's not defined, false is returned
	 *
	 * @return gpg executable path or "gpg", if it's not set.
	 */
	public static String getGpgExecutable() {
		String executable = HgClients.getPreference(MercurialPreferenceConstants.GPG_EXECUTABLE,
				"gpg"); //$NON-NLS-1$
		if (executable == null || executable.length() == 0) {
			return "false"; //$NON-NLS-1$
		}
		return executable;
	}

	/**
	 * Starts the configuration for Mercurial executable by opening the preference page.
	 */
	public static void configureHgExecutable() {
		final String jobName = Messages
				.getString("MercurialUtilities.openingPreferencesForConfiguringMercurialEclipse");
		SafeUiJob job = new SafeUiJob(jobName) {

			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				String pageId = "com.vectrace.MercurialEclipse.prefspage"; //$NON-NLS-1$
				PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(getDisplay()
						.getActiveShell(), pageId, null, null);
				dlg.setErrorMessage(Messages
						.getString("MercurialUtilities.errorNotConfiguredCorrectly") //$NON-NLS-1$
						+ Messages.getString("MercurialUtilities.runDebugInstall")); //$NON-NLS-1$
				dlg.open();
				return super.runSafe(monitor);
			}

			@Override
			public boolean belongsTo(Object family) {
				return jobName.equals(family);
			}
		};
		IJobManager jobManager = Job.getJobManager();
		jobManager.cancel(jobName);
		Job[] jobs = jobManager.find(jobName);
		if (jobs.length == 0) {
			job.schedule(50);
		}
	}

	public static boolean isPossiblySupervised(IResource resource) {
		if (resource == null) {
			return false;
		}
		// check, if we're team provider
		IProject project = resource.getProject();
		if (!MercurialTeamProvider.isHgTeamProviderFor(project)) {
			return false;
		}
		return !(Team.isIgnoredHint(resource) || resource.isTeamPrivateMember());
	}

	/**
	 * Checks if the given resource is controlled by MercurialEclipse. If the given resource is
	 * linked, it is not controlled by MercurialEclipse and therefore false is returned. A linked
	 * file is not followed, so even if there might be a hg repository in the linked files original
	 * location, we won't handle such a resource as supervised.
	 *
	 * @param dialog
	 *            flag to signify that an error message should be displayed if the resource is a
	 *            linked resource Return true if the resource
	 * @return true, if MercurialEclipse provides team functions to this resource, false otherwise.
	 */
	public static boolean hgIsTeamProviderFor(IResource resource, boolean dialog) {
		// check, if we're team provider
		if (resource == null) {
			return false;
		}
		IProject project = resource.getProject();
		if (project == null || !MercurialTeamProvider.isHgTeamProviderFor(project)) {
			return false;
		}

		// if we are team provider, this project can't be linked :-).
		if (resource instanceof IProject) {
			return true;
		}

		// Check to se if resource is not in a link
		boolean isLinked = resource.isLinked(IResource.CHECK_ANCESTORS);

		// open dialog if resource is linked and flag is set to true
		if (dialog && isLinked) {
			Shell shell = MercurialEclipsePlugin.getActiveShell();
			MessageDialog.openInformation(shell, Messages
					.getString("MercurialUtilities.linkWarningShort"), //$NON-NLS-1$
					Messages.getString("MercurialUtilities.linkWarningLong")); //$NON-NLS-1$
		}

		// TODO Follow links and see if they point to another reposetory

		return !isLinked;
	}

	/**
	 * Returns the username for hg as configured in preferences. If it's not defined in the
	 * preference store, null is returned.
	 * <p>
	 * <b>Note:</b> Preferred way to access user commit name is to use
	 * {@link HgCommitMessageManager#getDefaultCommitName(HgRoot)}
	 *
	 * @return hg username or empty string, never null
	 */
	public static String getDefaultUserName() {
		IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		// This returns "" if not defined
		String username = preferenceStore
				.getString(MercurialPreferenceConstants.MERCURIAL_USERNAME);

		// try to read username via hg showconfig
		if (username == null || username.equals("")) {
			try {
				username = HgConfigClient.getHgConfigLine(ResourcesPlugin.getWorkspace().getRoot()
						.getLocation().toFile(), "ui.username");
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		// try to read mercurial hgrc in default locations
		String home = System.getProperty("user.home");
		if (username == null || username.equals("")) {
			username = readUsernameFromIni(home + "/.hgrc");
		}

		if (isWindows()) {
			if (username == null || username.equals("")) {
				username = readUsernameFromIni(home + "/Mercurial.ini");
			}

			if (username == null || username.equals("")) {
				username = readUsernameFromIni("C:/Mercurial/Mercurial.ini");
			}
		}

		if (username == null || username.equals("")) {
			// use system username
			username = System.getProperty("user.name");
		}

		// never return null!
		if (username == null) {
			username = "";
		}
		return username;
	}

	private static String readUsernameFromIni(String filename) {
		String username;
		try {
			IniFile iniFile = new IniFile(filename);
			username = iniFile.getKeyValue("ui", "username");
		} catch (FileNotFoundException e) {
			username = null;
		}
		return username;
	}

	/**
	 * Execute a command via the shell. Can throw HgException if the command does not execute
	 * correctly. Exception will contain the error stream from the command execution.
	 *
	 * @returns String containing the successful output
	 *
	 *          TODO: Should log failure. TODO: Should not return null for failure.
	 */
	public static String executeCommand(String cmd[], File workingDir, boolean consoleOutput)
			throws HgException {
		return execute(cmd, workingDir).executeToString();
	}

	private static LegacyAdaptor execute(String cmd[], File workingDir) {
		String[] copy = new String[cmd.length - 2];
		System.arraycopy(cmd, 2, copy, 0, cmd.length - 2);
		LegacyAdaptor legacyAdaptor = new LegacyAdaptor(cmd[1], workingDir, true);
		legacyAdaptor.args(copy);
		return legacyAdaptor;
	}

	private static class LegacyAdaptor extends HgCommand {

		protected LegacyAdaptor(String command, File workingDir, boolean escapeFiles) {
			super(command, workingDir, escapeFiles);
		}

		LegacyAdaptor args(String... arguments) {
			this.addOptions(arguments);
			return this;
		}

		@Override
		public String executeToString() throws HgException {
			return super.executeToString();
		}

		@Override
		public byte[] executeToBytes() throws HgException {
			return super.executeToBytes();
		}
	}

	public static boolean isCommandAvailable(String command, QualifiedName sessionPropertyName,
			String extensionEnabler) throws HgException {
		try {
			boolean returnValue;
			IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
			Object prop = workspaceRoot.getSessionProperty(sessionPropertyName);
			if (prop != null) {
				returnValue = ((Boolean) prop).booleanValue();
			} else {
				returnValue = AbstractClient.isCommandAvailable(command, extensionEnabler);
				workspaceRoot.setSessionProperty(sessionPropertyName, Boolean.valueOf(returnValue));
			}
			return returnValue;
		} catch (CoreException e) {
			throw new HgException(e);
		}

	}

	/**
	 * @param prefHistoryMergeChangesetBackground
	 * @return
	 */
	public static Color getColorPreference(String pref) {
		RGB rgb = PreferenceConverter.getColor(MercurialEclipsePlugin.getDefault().getPreferenceStore(), pref);
		return new Color(MercurialEclipsePlugin.getStandardDisplay(), rgb);
	}

	/**
	 * @param prefHistoryMergeChangesetBackground
	 * @return
	 */
	public static Font getFontPreference(String pref) {
		FontData data = PreferenceConverter.getFontData(MercurialEclipsePlugin.getDefault().getPreferenceStore(), pref);
		return new Font(MercurialEclipsePlugin.getStandardDisplay(), data);
	}

}
