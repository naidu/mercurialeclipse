/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views.console;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * Console that shows the output of Hg commands. It is shown as a page in the
 * generic console view. It supports coloring for message, command, and error
 * lines in addition the font can be configured.
 * 
 * @since 3.0
 */
public class HgConsole extends MessageConsole implements IConsoleListener,
        IPropertyChangeListener {

    // created colors for each line type - must be disposed at shutdown
    private Color commandColor;
    private Color messageColor;
    private Color errorColor;

    // used to time the commands
    private long commandStarted = 0;

    // streams for each command type - each stream has its own color
    private MessageConsoleStream commandStream;
    private MessageConsoleStream messageStream;
    private MessageConsoleStream errorStream;

    // preferences for showing the Hg console when Hg output is provided
    private boolean showOnMessage = false;

    private ConsoleDocument document;
    private IConsoleManager consoleManager;

    // format for timings printed to console
    private static final DateFormat TIME_FORMAT;

    static {
        DateFormat format;
        format = new SimpleDateFormat("m:ss.SSS");
        TIME_FORMAT = format;
    }

    // Indicates whether the console is visible in the Console view
    private boolean visible = false;
    // Indicates whether the console's streams have been initialized
    private boolean initialized = false;

    /*
     * Constant used for indenting error status printing
     */
    private static final String NESTING = "   "; //$NON-NLS-1$

    /**
     * Used to notify this console of lifecycle methods <code>init()</code> and
     * <code>dispose()</code>.
     */
    public class MyLifecycle implements org.eclipse.ui.console.IConsoleListener {
        public void consolesAdded(IConsole[] consoles) {
            for (int i = 0; i < consoles.length; i++) {
                IConsole console = consoles[i];
                if (console == HgConsole.this) {
                    init();
                }
            }

        }

        public void consolesRemoved(IConsole[] consoles) {
            for (int i = 0; i < consoles.length; i++) {
                IConsole console = consoles[i];
                if (console == HgConsole.this) {
                    ConsolePlugin.getDefault().getConsoleManager()
                            .removeConsoleListener(this);
                    dispose();
                }
            }
        }
    }

    /**
     * Constructor initializes preferences and colors but doesn't create the
     * console page yet.
     */
    public HgConsole() {
        super(
                "Mercurial Console", MercurialEclipsePlugin.getImageDescriptor("icons/mercurialeclipse.png")); //$NON-NLS-1$ //$NON-NLS-2$
        showOnMessage = Boolean
                .valueOf(
                        MercurialUtilities
                                .getPreference(
                                        MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE,
                                        "true")).booleanValue();
        document = new ConsoleDocument();
        consoleManager = ConsolePlugin.getDefault().getConsoleManager();
        MercurialEclipsePlugin.getDefault().getPreferenceStore()
                .addPropertyChangeListener(HgConsole.this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.console.AbstractConsole#init()
     */
    @Override
    protected void init() {
        // Called when console is added to the console view
        super.init();

        initLimitOutput();
        initWrapSetting();

        // Ensure that initialization occurs in the ui thread
        new SafeUiJob("Initializing console...") {

            @Override
            public IStatus runSafe(IProgressMonitor monitor) {
                JFaceResources.getFontRegistry().addListener(HgConsole.this);
                initializeStreams();
                dump();
                return super.runSafe(monitor);
            }
        }.schedule();
    }

    private void initWrapSetting() {
        IPreferenceStore store = MercurialEclipsePlugin.getDefault()
                .getPreferenceStore();
        if (store.getBoolean(MercurialPreferenceConstants.PREF_CONSOLE_WRAP)) {
            setConsoleWidth(store
                    .getInt(MercurialPreferenceConstants.PREF_CONSOLE_WIDTH));
        } else {
            setConsoleWidth(-1);
        }
    }

    private void initLimitOutput() {
        IPreferenceStore store = MercurialEclipsePlugin.getDefault()
                .getPreferenceStore();
        if (store
                .getBoolean(MercurialPreferenceConstants.PREF_CONSOLE_LIMIT_OUTPUT)) {
            int highWaterMark = store
                    .getInt(MercurialPreferenceConstants.PREF_CONSOLE_HIGH_WATER_MARK);
            if (highWaterMark < 1000) {
                highWaterMark = 1000;
            }
            setWaterMarks(
                    0,
                    highWaterMark);
        } else {
            setWaterMarks(0, 1000);
        }
    }

    /*
     * Initialize thre streams of the console. Must be called from the UI
     * thread.
     */
    private void initializeStreams() {
        synchronized (document) {
            if (!initialized) {
                commandStream = newMessageStream();
                errorStream = newMessageStream();
                messageStream = newMessageStream();
                // install colors
                commandColor = createColor(MercurialEclipsePlugin
                        .getStandardDisplay(),
                        MercurialPreferenceConstants.PREF_CONSOLE_COMMAND_COLOR);
                commandStream.setColor(commandColor);
                messageColor = createColor(MercurialEclipsePlugin
                        .getStandardDisplay(),
                        MercurialPreferenceConstants.PREF_CONSOLE_MESSAGE_COLOR);
                messageStream.setColor(messageColor);
                errorColor = createColor(MercurialEclipsePlugin
                        .getStandardDisplay(),
                        MercurialPreferenceConstants.PREF_CONSOLE_ERROR_COLOR);
                errorStream.setColor(errorColor);
                // install font
                Font f = PlatformUI.getWorkbench().getThemeManager()
                        .getCurrentTheme().getFontRegistry().get(
                                MercurialPreferenceConstants.PREF_CONSOLE_FONT);
                setFont(f);
                initialized = true;
            }
        }
    }

    private void dump() {
        synchronized (document) {
            visible = true;
            ConsoleDocument.ConsoleLine[] lines = document.getLines();
            for (int i = 0; i < lines.length; i++) {
                ConsoleDocument.ConsoleLine line = lines[i];
                appendLine(line.type, line.line);
            }
            document.clear();
        }
    }

    private void appendLine(int type, String line) {
        showConsole();
        synchronized (document) {
            if (visible) {
                switch (type) {
                case ConsoleDocument.COMMAND:
                    commandStream.println(line);
                    break;
                case ConsoleDocument.MESSAGE:
                    messageStream.println("  " + line); //$NON-NLS-1$
                    break;
                case ConsoleDocument.ERROR:
                    errorStream.println("  " + line); //$NON-NLS-1$
                    break;
                }
            } else {
                document.appendConsoleLine(type, line);
            }
        }
    }

    private void showConsole() {
        show(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.console.MessageConsole#dispose()
     */
    @Override
    protected void dispose() {
        // Here we can't call super.dispose() because we actually want the
        // partitioner to remain
        // connected, but we won't show lines until the console is added to the
        // console manager
        // again.

        // Called when console is removed from the console view
        synchronized (document) {
            visible = false;
            JFaceResources.getFontRegistry().removeListener(this);
        }
    }

    /**
     * Clean-up created fonts.
     */
    public void shutdown() {
        // Call super dispose because we want the partitioner to be
        // disconnected.
        super.dispose();
        if (commandColor != null) {
            commandColor.dispose();
        }
        if (messageColor != null) {
            messageColor.dispose();
        }
        if (errorColor != null) {
            errorColor.dispose();
        }
        MercurialEclipsePlugin.getDefault().getPreferenceStore()
                .removePropertyChangeListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.internal.cHg.core.client.listeners.IConsoleListener#
     * commandInvoked(java.lang.String)
     */
    public void commandInvoked(String line) {
        commandStarted = System.currentTimeMillis();
        appendLine(ConsoleDocument.COMMAND, line);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.internal.cHg.core.client.listeners.IConsoleListener#
     * messageLineReceived(java.lang.String)
     */
    public void messageLineReceived(String line, IStatus status) {
        appendLine(ConsoleDocument.MESSAGE, "  " + line); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.internal.cHg.core.client.listeners.IConsoleListener#
     * errorLineReceived(java.lang.String)
     */
    public void errorLineReceived(String line, IStatus status) {
        appendLine(ConsoleDocument.ERROR, "  " + line); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.internal.cHg.core.client.listeners.IConsoleListener#
     * commandCompleted(org.eclipse.core.runtime.IStatus, java.lang.Exception)
     */
    public void commandCompleted(IStatus status, Throwable exception) {
        long commandRuntime = System.currentTimeMillis() - commandStarted;
        String time;
        try {
            time = "Done in " + TIME_FORMAT.format(new Date(commandRuntime))
                    + " min.";
        } catch (RuntimeException e) {
            MercurialEclipsePlugin.logError(e);
            time = "UNKNOWN";
        }
        String statusText;
        if (status != null) {
            boolean includeRoot = true;
            if (status.getSeverity() == IStatus.ERROR) {
                statusText = status.getMessage() + "(" + time + ")";
                appendLine(ConsoleDocument.ERROR, statusText);
                includeRoot = false;
            } else {
                statusText = time;
                appendLine(ConsoleDocument.MESSAGE, statusText);
            }

            outputStatus(status, includeRoot, includeRoot ? 0 : 1);
        } else if (exception != null) {
            if (exception instanceof OperationCanceledException) {
                statusText = "Aborted. (" + time + ")";
            } else {
                statusText = time;
            }
            appendLine(ConsoleDocument.COMMAND, statusText);
            if (exception instanceof CoreException) {
                outputStatus(((CoreException) exception).getStatus(), true, 1);
            }
        } else {
            appendLine(ConsoleDocument.COMMAND, time);
        }
        appendLine(ConsoleDocument.COMMAND, "");
    }

    private void outputStatus(IStatus status, boolean includeParent,
            int nestingLevel) {
        int myNestingLevel = nestingLevel;
        if (includeParent && !status.isOK()) {
            outputStatusMessage(status, nestingLevel);
            myNestingLevel++;
        }

        // Include a CoreException in the status
        Throwable t = status.getException();
        if (t instanceof CoreException) {
            outputStatus(((CoreException) t).getStatus(), true, myNestingLevel);
        }

        // Include child status
        IStatus[] children = status.getChildren();
        for (int i = 0; i < children.length; i++) {
            outputStatus(children[i], true, myNestingLevel);
        }
    }

    private void outputStatusMessage(IStatus status, int nesting) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < nesting; i++) {
            buffer.append(NESTING);
        }
        buffer.append(messageLineForStatus(status));
        appendLine(ConsoleDocument.COMMAND, buffer.toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse
     * .jface.util.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        // colors
        if (visible) {
            if (property
                    .equals(MercurialPreferenceConstants.PREF_CONSOLE_COMMAND_COLOR)) {
                Color newColor = createColor(MercurialEclipsePlugin
                        .getStandardDisplay(),
                        MercurialPreferenceConstants.PREF_CONSOLE_COMMAND_COLOR);
                commandStream.setColor(newColor);
                commandColor.dispose();
                commandColor = newColor;
            } else if (property
                    .equals(MercurialPreferenceConstants.PREF_CONSOLE_MESSAGE_COLOR)) {
                Color newColor = createColor(MercurialEclipsePlugin
                        .getStandardDisplay(),
                        MercurialPreferenceConstants.PREF_CONSOLE_MESSAGE_COLOR);
                messageStream.setColor(newColor);
                messageColor.dispose();
                messageColor = newColor;
            } else if (property
                    .equals(MercurialPreferenceConstants.PREF_CONSOLE_ERROR_COLOR)) {
                Color newColor = createColor(MercurialEclipsePlugin
                        .getStandardDisplay(),
                        MercurialPreferenceConstants.PREF_CONSOLE_ERROR_COLOR);
                errorStream.setColor(newColor);
                errorColor.dispose();
                errorColor = newColor;
                // font
            } else if (property
                    .equals(MercurialPreferenceConstants.PREF_CONSOLE_FONT)) {
                setFont(((FontRegistry) event.getSource())
                        .get(MercurialPreferenceConstants.PREF_CONSOLE_FONT));
            }
        }
        if (property
                .equals(MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE)) {
            Object value = event.getNewValue();
            if (value instanceof String) {
                showOnMessage = Boolean.valueOf((String) value).booleanValue();
            } else {
                showOnMessage = ((Boolean) value).booleanValue();
            }
        } else if (property
                .equals(MercurialPreferenceConstants.PREF_CONSOLE_LIMIT_OUTPUT)) {
            initLimitOutput();
        } else if (property
                .equals(MercurialPreferenceConstants.PREF_CONSOLE_WRAP)) {
            initWrapSetting();
        }
    }

    /**
     * Returns the NLSd message based on the status returned from the Hg
     * command.
     * 
     * @param status
     *            an NLSd message based on the status returned from the Hg
     *            command.
     */
    private String messageLineForStatus(IStatus status) {
        if (status.getSeverity() == IStatus.ERROR) {
            return "Error: " + status.getMessage();
        } else if (status.getSeverity() == IStatus.WARNING) {
            return "Warning: " + status.getMessage();
        } else if (status.getSeverity() == IStatus.INFO) {
            return "Info: " + status.getMessage();
        }
        return status.getMessage();
    }

    /**
     * Returns a color instance based on data from a preference field.
     */
    private Color createColor(Display display, String preference) {
        RGB rgb = PreferenceConverter.getColor(MercurialEclipsePlugin
                .getDefault().getPreferenceStore(), preference);
        return new Color(display, rgb);
    }

    /**
     * Show the console.
     * 
     * @param showNoMatterWhat
     *            ignore preferences if <code>true</code>
     */
    public void show(boolean showNoMatterWhat) {
        showOnMessage = Boolean
                .valueOf(
                        MercurialUtilities
                                .getPreference(
                                        MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE,
                                        "false")).booleanValue();
        if (showNoMatterWhat || showOnMessage) {
            if (!visible) {
                HgConsoleFactory.showConsole();
            } else {
                consoleManager.showConsoleView(this);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.console.IConsoleListener#consolesAdded(org.eclipse.ui.
     * console.IConsole[])
     */
    public void consolesAdded(IConsole[] consoles) {
        for (IConsole console : consoles) {
            if (console.equals(this)) {
                show(true);
                break;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.console.IConsoleListener#consolesRemoved(org.eclipse.ui
     * .console.IConsole[])
     */
    public void consolesRemoved(IConsole[] consoles) {

    }
}
