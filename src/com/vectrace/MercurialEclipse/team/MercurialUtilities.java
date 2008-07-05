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
 *******************************************************************************/

package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.views.console.HgConsole;

/**
 * @author zingo
 * 
 */
public class MercurialUtilities {

    static HgConsole console;
    static IOConsoleInputStream console_in;
    static IOConsoleOutputStream console_out;
    static PrintStream console_out_printstream; // migth be used by threads

    // GetMercurialConsole should be
    // used to get this, even
    // internally
    // GetMercurialConsole() in
    // synchronized

    /**
     * This class is full of utilities metods, useful allover the place
     */
    public MercurialUtilities() {

    }

    /**
     * mercurial command
     */
    public static boolean isExecutableConfigured() {
        try {
            Runtime.getRuntime().exec(getHGExecutable());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Returns the executable for hg. If it's not defined, false is returned
     * 
     * @return false if no hg is defined. True if hg executable is defined
     */
    public static String getHGExecutable() {
        return getPreference(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE,
                "");
    }

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

    public static String getHGExecutable(boolean configureIfMissing) {
        if (isExecutableConfigured()) {
            return getHGExecutable();
        }
        if (configureIfMissing) {
            configureHgExecutable();
            return getHGExecutable();
        }
        return "hg";
    }

    public static String getGpgExecutable(boolean configureIfMissing) {
        if (isGpgExecutableConfigured()) {
            return getGpgExecutable();
        }
        if (configureIfMissing) {
            configureGpgExecutable();
            return getGpgExecutable();
        }
        return "gpg";
    }

    public static void configureGpgExecutable() {
        configureHgExecutable();
    }

    private static boolean isGpgExecutableConfigured() {
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
     * @return false if no hg is defined. True if hg executable is defined
     */
    public static String getGpgExecutable() {
        return getPreference(MercurialPreferenceConstants.GPG_EXECUTABLE, "gpg");
    }

    public static void configureHgExecutable() {
        Shell shell = Display.getCurrent().getActiveShell();
        String pageId = "com.vectrace.MercurialEclipse.prefspage";
        String[] dsplIds = null;
        Object data = null;
        PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(shell,
                pageId, dsplIds, data);
        dlg.setErrorMessage("Mercurial is not configured correctly."
                + "Run 'hg debuginstall' to analyse.");
        dlg.open();
    }

    /**
     * Should we handle resource
     * 
     * @param dialog
     *            TODO
     * 
     *            Return true if the resource is handled by mercurial. is it a
     *            link we do not folow the link (not now anyway mabe later
     *            versions will)
     * 
     */
    public static boolean isResourceInReposetory(IResource resource,
            boolean dialog) {
        // check, if we're team provider
        if (resource == null || resource.getProject() == null
                || RepositoryProvider.getProvider(resource.getProject(),MercurialTeamProvider.ID) == null) {
            return false;
        }
        
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
                        .openInformation(shell, "Resource in link URI",
                                "The Selected resource is in a link and can't be handled by this plugin sorry!");
            }
        }

        // TODO Follow links and see if they point to another reposetory

        return !isLinked;
    }

    /** ************************* Username *********************** */

    /**
     * Returns the Username for hg. If it's not defined, false is returned
     * 
     * @return false if no hg is defined. True if hg executable is defined
     */
    public static String getHGUsername() {
        IPreferenceStore preferenceStore = MercurialEclipsePlugin.getDefault()
                .getPreferenceStore();
        // This returns "" if not defined
        String executable = preferenceStore
                .getString(MercurialPreferenceConstants.MERCURIAL_USERNAME);
        return executable;
    }

    public static String getHGUsername(boolean configureIfMissing) {
        String uname = getHGUsername();

        if (uname != null) {
            return uname;
        }
        if (configureIfMissing) {
            configureUsername();
            return getHGUsername();
        }
        return System.getProperty("user.name");
    }

    public static void configureUsername() {
        Shell shell = Display.getCurrent().getActiveShell();
        String pageId = "com.vectrace.MercurialEclipse.prefspage";
        String[] dsplIds = null;
        Object data = null;
        PreferenceDialog dlg = PreferencesUtil.createPreferenceDialogOn(shell,
                pageId, dsplIds, data);
        dlg.open();
    }

    /**
     * ************************* search for a mercurial repository
     * ***********************
     */

    static String search4MercurialRoot(final IProject project) {
        if (project != null) {
            if (project.getLocation() != null) {
                return MercurialUtilities.search4MercurialRoot(project
                        .getLocation().toFile());
            }
        }
        return null;
    }

    static String search4MercurialRoot(final File file) {
        String path = null;
        File parent = file;
        File hgFolder = new File(parent, ".hg");
        // System.out.println("pathcheck:" + parent.toString());
        while ((parent != null)
                && !(hgFolder.exists() && hgFolder.isDirectory())) {
            parent = parent.getParentFile();
            if (parent != null) {
                // System.out.println("pathcheck:" + parent.toString());
                hgFolder = new File(parent, ".hg");
            }
        }
        if (parent != null) {
            path = hgFolder.getParentFile().toString();
        }

        // System.out.println("pathcheck: >" + path + "<");
        return path;
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

    /*
     * Convenience method to return the OS specific path to the repository.
     */
    static public String getRepositoryPath(IProject project) {
        return project.getLocation().toOSString();
    }

    /*
     * Execute a command via the shell. Can throw HgException if the command
     * does not execute correctly. Exception will contain the error stream from
     * the command execution. @returns String containing the successful
     * 
     * TODO: Should log failure. TODO: Should not return null for failure.
     */
    public static String ExecuteCommand(String cmd[], File workingDir,
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
     * @param obj
     * @return Workingdir of object
     * 
     */
    static public File getWorkingDir(IResource obj) {

        File workingDir;
        if (obj.getType() == IResource.PROJECT) {
            workingDir = (obj.getLocation()).toFile();
        } else if (obj.getType() == IResource.FOLDER) {
            // workingDir = (obj.getLocation()).toFile();
            workingDir = (obj.getLocation()).removeLastSegments(1).toFile();
        } else if (obj.getType() == IResource.FILE) {
            workingDir = (obj.getLocation()).removeLastSegments(1).toFile();
        } else {
            workingDir = null;
        }

        return workingDir;
    }

    /**
     * @param obj
     * @return Workingdir of object
     * 
     */
    static public File getWorkingDir(File obj) {
        // System.out.println("getWorkingDir( " + obj.toString() + ") = " +
        // obj.getAbsolutePath());
        return new File(obj.getPath());
    }

    static public String getResourceName(IResource obj) {
        return (obj.getLocation()).lastSegment();
    }

    static public String getResourceName(IResource obj, File workingDir) {

        String st = obj.getLocation().toOSString();
        // System.out.println("getResourceName(<" + st + ">,<"+
        // workingDir.getAbsolutePath() + ">) =<" +
        // st.substring(workingDir.getAbsolutePath().length()) + ">");
        if (st.startsWith(workingDir.getAbsolutePath())) {
            st = st.substring(workingDir.getAbsolutePath().length()); // Cut
            // of
            // working dir
            // it might start with a path separator char that we want to
            // remove...
            if (st.startsWith(File.separator)) {
                st = st.substring(File.separator.length());
            }
        }
        return st;
    }

    public static synchronized HgConsole getMercurialConsole() {
        if (console != null) {
            return console;
        }

        console = new HgConsole();
        console.initialize();
        
//        if (console_in == null) {
//            console_in = console.getInputStream();
//        }
//        if (console_out == null) {
//            console_out = console.newOutputStream();
//            // console_out_printstream.println("Hello word!");
//        }
        return console;
    }

//    static synchronized PrintStream getMercurialConsoleOutPrintStream() {
//        if (console_out_printstream != null) {
//            return console_out_printstream;
//        }
//        if (console == null) {
//            console = getMercurialConsole();
//        }
//        if (console_out != null) {
//            console_out_printstream = new PrintStream(console_out);
//            // console_out_printstream.setColor(Display.getDefault().
//            // getSystemColor(SWT.COLOR_GREEN));
//            return console_out_printstream;
//        }
//        return null;
//    }

    /*
     * TODO public static synchronized IOConsole getBazaarConsole() {
     * 
     * if (console == null) { console = new
     * IOConsole(UITexts.BazaarConsole_name, null); IConsoleManager manager =
     * ConsolePlugin.getDefault().getConsoleManager(); manager.addConsoles(new
     * IConsole[] { console }); } else { return console; } if (console_in ==
     * null) { console_in = console.getInputStream(); } if (console_out == null)
     * { console_out = console.newOutputStream(); //
     * console_out_printstream.println("Hello word!"); } return console; //
     * Error }
     * 
     * static synchronized PrintStream getBazaarConsoleOutPrintStream(){
     * if(console == null) { console = getBazaarConsole(); } if (console_out !=
     * null) { console_out_printstream = new PrintStream(console_out); return
     * console_out_printstream; } return null; }
     */

    /*
     * public void runTest(IOConsole console) { final Display display =
     * Display.getDefault();
     * 
     * final IOConsoleInputStream in = console.getInputStream();
     * display.asyncExec(new Runnable() { public void run() {
     * in.setColor(display.getSystemColor(SWT.COLOR_BLUE)); } });
     * IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
     * manager.addConsoles(new IConsole[] { console });
     * 
     * final IOConsoleOutputStream out = console.newOutputStream();
     * //$NON-NLS-1$ Display.getDefault().asyncExec(new Runnable() { public void
     * run() {
     * out.setColor(Display.getDefault().getSystemColor(SWT.COLOR_GREEN));
     * out.setFontStyle(SWT.ITALIC); } });
     * 
     * PrintStream ps = new PrintStream(out); ps.println("Any text entered
     * should be echoed back"); //$NON-NLS-1$ for(;;) { byte[] b = new
     * byte[1024]; int bRead = 0; try { bRead = in.read(b); } catch (IOException
     * io) { io.printStackTrace(); }
     * 
     * try { out.write(b, 0, bRead); ps.println(); } catch (IOException e) {
     * e.printStackTrace(); } } }
     */

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
     * This methods extracts the IResource from the given file path taking into
     * account, that the repository root might be above project level.
     * 
     * @param proj
     * @param file
     * @param res
     * @return
     */
    public static IResource getIResource(IProject proj, String path) {
        int projectPathStartIndex = 0;
        try {
            if (!MercurialTeamProvider.isRepositoryRootInProject(proj)) {
                projectPathStartIndex = path.indexOf(proj.getName());
            }
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
        }
        if (projectPathStartIndex >= 0) {
            return proj.getFile(path.substring(projectPathStartIndex));
        }
        return null;
    }
}
