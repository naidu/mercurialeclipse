/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views.console;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * Console factory is used to show the console from the Console view
 * "Open Console" drop-down action. This factory is registered via the
 * org.eclipse.ui.console.consoleFactory extension point.
 * 
 * @since 3.1
 */
public class HgConsoleFactory implements IConsoleFactory, IConsoleListener,
        IPropertyChangeListener {
    private static HgConsole console;
    private static boolean showOnMessage;
    private static IConsoleManager consoleManager;
    private static boolean initialized = false;
    private static HgConsoleFactory instance = null;
    
    public HgConsoleFactory() {
        instance = this;
        console = new HgConsole();
    }
    
    public static HgConsoleFactory getInstance() {
        if (instance == null) {
            instance = new HgConsoleFactory();
        }
        return instance;
    }

    public void openConsole() {
        showConsole();
    }

    private void init() {
        if (!initialized) {
            Runnable r = new Runnable() { 
                /*
                 * (non-Javadoc)
                 * 
                 * @see
                 * com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org
                 * .eclipse .core.runtime.IProgressMonitor)
                 */
                
                public void run() {
                    // install font
                    Font f = PlatformUI
                            .getWorkbench()
                            .getThemeManager()
                            .getCurrentTheme()
                            .getFontRegistry()
                            .get(MercurialPreferenceConstants.PREF_CONSOLE_FONT);
                    if (console == null) {
                        console = new HgConsole();
                    }
                    console.setFont(f);
                    console.initialize();
                    JFaceResources.getFontRegistry().addListener(
                            HgConsoleFactory.this);
                }
            };
            MercurialEclipsePlugin.getStandardDisplay().asyncExec(r);
            MercurialEclipsePlugin.getDefault().getPreferenceStore()
                    .addPropertyChangeListener(this);
        }
        initialized = true;
    }

    public void showConsole() {
        init();

        // register console
        IConsole[] existing = getConsoleManager().getConsoles();
        boolean exists = false;
        for (int i = 0; i < existing.length; i++) {
            if (console == existing[i]) {
                exists = true;
            }
        }
        if (!exists) {
            getConsoleManager().addConsoles(new IConsole[] { console });
        }

        showOnMessage = Boolean
                .valueOf(
                        MercurialUtilities
                                .getPreference(
                                        MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE,
                                        "false")).booleanValue(); //$NON-NLS-1$

        if (showOnMessage) {
            getConsoleManager().showConsoleView(console);
        }
    }

    public static void closeConsole() {
        IConsoleManager manager = ConsolePlugin.getDefault()
                .getConsoleManager();
        if (console != null) {
            manager.removeConsoles(new IConsole[] { console });
        }
    }

    /**
     * @return
     */
    public HgConsole getConsole() {
        return console;
    }

    public void consolesAdded(IConsole[] consoles) {
        for (int i = 0; i < consoles.length; i++) {
            IConsole c = consoles[i];
            if (console == c) {
                console.init();
                showConsole();
                break;
            }
        }
    }

    public void consolesRemoved(IConsole[] consoles) {
        for (int i = 0; i < consoles.length; i++) {
            IConsole c = consoles[i];
            if (c == console) {
                console.dispose();
                JFaceResources.getFontRegistry().removeListener(this);
                console = null;
                MercurialEclipsePlugin.getDefault().getPreferenceStore()
                        .removePropertyChangeListener(this);
                initialized = false;
                break;
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange
     * (org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        console.propertyChange(event);
    }

    /**
     * @param consoleManager the consoleManager to set
     */
    private void setConsoleManager(IConsoleManager consoleManager) {
        HgConsoleFactory.consoleManager = consoleManager;
    }

    /**
     * @return the consoleManager
     */
    private IConsoleManager getConsoleManager() {
        if (consoleManager == null) {
            setConsoleManager(ConsolePlugin.getDefault().getConsoleManager());
        }
        return consoleManager;
    }
}
