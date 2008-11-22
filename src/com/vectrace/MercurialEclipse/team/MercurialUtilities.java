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
 *******************************************************************************/

package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgConfigClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.views.console.HgConsole;
import com.vectrace.MercurialEclipse.views.console.HgConsoleFactory;

/**
 * Class that offers Utility methods for working with the plug-in.
 * 
 * @author zingo
 * 
 */
public class MercurialUtilities {

    private static HgConsole console;

    /**
     * This class is full of utilities metods, useful allover the place
     */
    public MercurialUtilities() {

    }

    /**
     * Determines if the configured Mercurial executable can be called.
     * 
     * @return true if no error occurred while calling the executable, false
     *         otherwise
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
     * Returns the hg executable stored in the plug-in preferences. If it's not
     * defined, "hg" is returned as default.
     * 
     * @return the path to the executable or, if not defined "hg"
     */
    public static String getHGExecutable() {
        return HgClients.getPreference(
                MercurialPreferenceConstants.MERCURIAL_EXECUTABLE, "hg"); //$NON-NLS-1$
    }

    /**
     * Fetches a preference from the plug-in's preference store. If no
     * preference could be found in the store, the given default is returned.
     * 
     * @param preferenceConstant
     *            the string identifier for the constant.
     * @param defaultIfNotSet
     *            the default to return if no preference was found.
     * @return the preference or the default
     */
    public static String getPreference(String preferenceConstant,
            String defaultIfNotSet) {
        IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault()
                .getPreferenceStore();
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
     * If configureIfMissing is set to true, the configuration will be started
     * and afterwards the executable stored in the preferences will be checked
     * if it is callable. If true, it is returned, else "hg" will be returned.
     * If the parameter is set to false, it will returns "hg" if no preference
     * is set.
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
     * Otherwise, if configureIfMissing is set to true, configuration will be
     * started and the new command is tested for callability. If there's no
     * preference found after configuration, "gpg" will be returned as default.
     * 
     * @param configureIfMissing
     *            flag, if configuration should be started if gpg is not
     *            callable.
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
        String executable = HgClients.getPreference(
                MercurialPreferenceConstants.GPG_EXECUTABLE, "gpg"); //$NON-NLS-1$
        if (executable == null || executable.length() == 0) {
            return "false"; //$NON-NLS-1$
        }
        return executable;
    }

    /**
     * Starts the configuration for Mercurial executable by opening the
     * preference page.
     */
    public static void configureHgExecutable() {
        Shell shell = Display.getCurrent().getActiveShell();
        String pageId = "com.vectrace.MercurialEclipse.prefspage"; //$NON-NLS-1$
        String[] dsplIds = null;
        Object data = null;
        PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(shell,
                pageId, dsplIds, data);
        dlg.setErrorMessage(Messages.getString("MercurialUtilities.errorNotConfiguredCorrectly") //$NON-NLS-1$
                + Messages.getString("MercurialUtilities.runDebugInstall")); //$NON-NLS-1$
        dlg.open();
    }

    /**
     * Checks if the given resource is controlled by MercurialEclipse. If the
     * given resource is linked, it is not controlled by MercurialEclipse and
     * therefore false is returned. A linked file is not followed, so even if
     * there might be a hg repository in the linked files original location, we
     * won't handle such a resource as supervised.
     * 
     * @param dialog
     *            flag to signify that an error message should be displayed if
     *            the resource is a linked resource Return true if the resource
     * @return true, if MercurialEclipse provides team functions to this
     *         resource, false otherwise.
     */
    public static boolean hgIsTeamProviderFor(IResource resource, boolean dialog) {
        // check, if we're team provider
        if (resource == null
                || resource.getProject() == null
                || RepositoryProvider.getProvider(resource.getProject(),
                        MercurialTeamProvider.ID) == null) {
            return false;
        }

        // if we are team provider, this project can't be linked :-).
        if (resource instanceof IProject) {
            return true;
        }

        // Check to se if resource is not in a link
        String linkedParentName = resource.getProjectRelativePath().segment(0);
        if (linkedParentName == null) {
            return false;
        }

        IFolder linkedParent = resource.getProject()
                .getFolder(linkedParentName);
        boolean isLinked = linkedParent.isLinked();

        // open dialog if resource is linked and flag is set to true
        if (dialog && isLinked) {
            Shell shell = null;
            IWorkbench workbench = null;

            workbench = PlatformUI.getWorkbench();
            if (workbench != null
                    && workbench.getActiveWorkbenchWindow() != null) {
                shell = workbench.getActiveWorkbenchWindow().getShell();
            }
            if (shell != null) {
                MessageDialog
                        .openInformation(shell, Messages.getString("MercurialUtilities.linkWarningShort"), //$NON-NLS-1$
                                Messages.getString("MercurialUtilities.linkWarningLong")); //$NON-NLS-1$
            }
        }

        // TODO Follow links and see if they point to another reposetory

        return !isLinked;
    }

    /**
     * Returns the username for hg as configured in preferences. If it's not
     * defined in the preference store, null is returned.
     * 
     * @return hg username or null
     */
    public static String getHGUsername() {
        IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault()
                .getPreferenceStore();
        // This returns "" if not defined
        String executable = preferenceStore
                .getString(MercurialPreferenceConstants.MERCURIAL_USERNAME);
        return executable;
    }

    /**
     * Gets the username for hg as configured in preferences. If there is no
     * preference set and configureIfMissing is true, start configuration of the
     * username and afterwards return the new preference. If nothing was
     * configured this could still be null!
     * 
     * If configureIfMissing is false and no preference is set, the systems
     * property "user.name" is returned (@see {@link System#getProperty(String)}
     * 
     * @param configureIfMissing
     *            true if configuration should be started, otherwise false
     * @return the username
     */
    public static String getHGUsername(boolean configureIfMissing) {
        String uname = getHGUsername();

        if (uname != null && uname.length() != 0) {
            return uname;
        }
        if (configureIfMissing) {
            configureUsername();
            return getHGUsername();
        }
        try {
            return HgConfigClient.getHgConfigLine(ResourcesPlugin.getWorkspace()
                    .getRoot().getLocation().toFile(), "ui.username"); //$NON-NLS-1$
        } catch (HgException e) {
            return System.getProperty("user.name"); //$NON-NLS-1$
        }
    }

    /**
     * Starts configuration of the hg username by opening preference page.
     */
    public static void configureUsername() {
        Shell shell = Display.getCurrent().getActiveShell();
        String pageId = "com.vectrace.MercurialEclipse.prefspage"; //$NON-NLS-1$
        String[] dsplIds = null;
        Object data = null;
        PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(shell,
                pageId, dsplIds, data);
        dlg.open();
    }

    /**
     * Returns hg root if project != null. Delegates to
     * {@link MercurialTeamProvider#getHgRoot(IResource)}.
     * 
     * @param project
     *            the project to get root for
     * @return the canonical file system path of the hg root or null
     * @throws HgException
     *             if error occurred
     */
    public static String search4MercurialRoot(final IProject project)
            throws HgException {
        if (project != null) {
            try {
                File file = MercurialTeamProvider.getHgRoot(project);
                if (file != null) {
                    return file.getCanonicalPath();
                }
            } catch (HgException e) {
                // do nothing - no root available
                MercurialEclipsePlugin.logInfo(e.getLocalizedMessage(), e);
            } catch (IOException e) {
                MercurialEclipsePlugin.logError(e);
                throw new HgException(e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns hg root if file != null. Delegates to
     * {@link MercurialTeamProvider#getHgRoot(File)}.
     * 
     * @param file
     *            file to get root for
     * @return the canonical file system path of the hg root or null
     * @throws HgException
     */
    public static String search4MercurialRoot(final File file)
            throws HgException {
        if (file != null) {
            try {
                return MercurialTeamProvider.getHgRoot(file).getCanonicalPath();
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
                throw new HgException(e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

    /**
     * Get the project for the selection (it use the first element)
     * 
     * @param selection
     * @return
     */
    public static IProject getProject(IStructuredSelection selection) {
        Object obj;
        obj = selection.getFirstElement();
        if ((obj != null) && (obj instanceof IResource)) {
            return ((IResource) obj).getProject();
        }
        return null;
    }

    /**
     * Convenience method to return the OS specific path to the repository.
     */
    static public String getRepositoryPath(IProject project) {
        return project.getLocation().toOSString();
    }

    /**
     * Execute a command via the shell. Can throw HgException if the command
     * does not execute correctly. Exception will contain the error stream from
     * the command execution.
     * 
     * @returns String containing the successful output
     * 
     *          TODO: Should log failure. TODO: Should not return null for
     *          failure.
     */
    public static String executeCommand(String cmd[], File workingDir,
            boolean consoleOutput) throws HgException {
        return execute(cmd, workingDir).executeToString();
    }

    private static LegacyAdaptor execute(String cmd[], File workingDir) {
        String[] copy = new String[cmd.length - 2];
        System.arraycopy(cmd, 2, copy, 0, cmd.length - 2);
        LegacyAdaptor legacyAdaptor = new LegacyAdaptor(cmd[1], workingDir,
                true);
        legacyAdaptor.args(copy);
        return legacyAdaptor;
    }

    /**
     * Gets the working directory for an IResource
     * 
     * @param obj
     *            the resource we need the working directory for
     * @return Workingdir of object or null if resource neither project, folder
     *         or file
     */
    public static File getWorkingDir(IResource obj) {

        File workingDir;
        if (obj.getType() == IResource.PROJECT) {
            workingDir = (obj.getLocation()).toFile();
        } else if (obj.getType() == IResource.FOLDER) {
            workingDir = (obj.getLocation()).removeLastSegments(1).toFile();
        } else if (obj.getType() == IResource.FILE) {
            workingDir = (obj.getLocation()).removeLastSegments(1).toFile();
        } else {
            workingDir = null;
        }
        return workingDir;
    }

    /**
     * Gets the Mercurial console. If it's not already created, we create it and
     * register it with the ConsoleManager.
     * 
     * @return the MercurialConsole.
     */
    public static synchronized HgConsole getMercurialConsole() {
        if (console != null) {
            return console;
        }

        console = new HgConsole();
        console.initialize();
        HgConsoleFactory.showConsole();
        return console;
    }

    private static class LegacyAdaptor extends HgCommand {

        protected LegacyAdaptor(String command, File workingDir,
                boolean escapeFiles) {
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

    /**
     * Gets a resources name.
     * 
     * @param obj
     *            an IResource
     * @return the name
     */
    public static String getResourceName(IResource obj) {
        return (obj.getLocation()).lastSegment();
    }

    /**
     * Converts a {@link java.io.File} to a workspace resource
     * 
     * @param file
     * @return
     * @throws HgException
     */
    public static IResource convert(File file) throws HgException {
        try {
            IResource resource;
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            if (file.isDirectory()) {
                resource = root.getContainerForLocation(new Path(file
                        .getCanonicalPath()));
            } else {
                resource = root.getFileForLocation(new Path(file
                        .getCanonicalPath()));
            }
            return resource;
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Converts a path to a workspace resource
     * 
     * @param file
     * @return the resource or null
     * @throws HgException
     */
    public static IResource convert(String path) throws HgException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.getFileForLocation(new Path(path));
        if (resource == null) {
            resource = root.getContainerForLocation(new Path(path));
        }
        return resource;
    }
    
    public static boolean isCommandAvailable(String command,
            QualifiedName sessionPropertyName, String extensionEnabler)
            throws HgException {
        try {
            boolean returnValue;
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
                    .getRoot();
            Object prop = workspaceRoot
                    .getSessionProperty(sessionPropertyName);
            if (prop != null) {
                returnValue = ((Boolean) prop).booleanValue();
            } else {
                returnValue = AbstractClient.isCommandAvailable(command,
                        extensionEnabler);
                workspaceRoot.setSessionProperty(sessionPropertyName, Boolean
                        .valueOf(returnValue));
            }
            return returnValue;
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e);
        }

    }

}
