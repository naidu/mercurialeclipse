/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * ijuma	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views.console;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * This class should only be called from the UI thread as it is not thread-safe.
 */
public class HgConsoleHolder implements IConsoleListener, IPropertyChangeListener {

    private static final HgConsoleHolder instance = new HgConsoleHolder();

    private volatile HgConsole console;

    private boolean showOnMessage;

    private HgConsoleHolder() {
    }

    public static HgConsoleHolder getInstance() {
        return instance;
    }

    private void init() {
        if (isInitialized()) {
            return;
        }
        synchronized(this){
            if (isInitialized()) {
                return;
            }
            console = new HgConsole();
            // install font
            Font f = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry().get(
                    MercurialPreferenceConstants.PREF_CONSOLE_FONT);
            console.setFont(f);
            showOnMessage = Boolean.parseBoolean(MercurialUtilities.getPreference(
                    MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE, "false"));
            JFaceResources.getFontRegistry().addListener(this);
            MercurialEclipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
        }
    }

    private boolean isInitialized() {
        return console != null;
    }

    public HgConsole showConsole() {
        init();

        if (showOnMessage) {
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
            getConsoleManager().showConsoleView(console);
        }

        return console;
    }

    public void closeConsole() {
        IConsoleManager manager = ConsolePlugin.getDefault()
                .getConsoleManager();
        if (console != null) {
            manager.removeConsoles(new IConsole[] { console });
        }
    }

    public HgConsole getConsole() {
        init();
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
                console = null;
                JFaceResources.getFontRegistry().removeListener(this);
                MercurialEclipsePlugin.getDefault().getPreferenceStore()
                        .removePropertyChangeListener(this);
                break;
            }
        }

    }

    public void propertyChange(PropertyChangeEvent event) {
        if(MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE.equals(event.getProperty())){
            showOnMessage = Boolean.parseBoolean(MercurialUtilities.getPreference(
                    MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE, "false"));
        } else {
            console.propertyChange(event);
        }
    }

    private IConsoleManager getConsoleManager() {
        return ConsolePlugin.getDefault().getConsoleManager();
    }
}
