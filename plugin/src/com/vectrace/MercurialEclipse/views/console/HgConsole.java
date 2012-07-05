/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Andrei Loskutov     - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views.console;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.themes.ITheme;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.views.console.HgConsoleHolder.IHgConsole;

/**
 * Console that shows the output of Hg commands. It is shown as a page in the
 * generic console view. It supports coloring for message, command, and error
 * lines in addition the font can be configured.
 *
 */
public class HgConsole extends MessageConsole implements IHgConsole {

	/**
	 * Used in plugin.xml
	 */
	private static final String HG_CONSOLE_TYPE = "hgConsole";

	private static final String CONSOLE_FONT = "com.vectrace.mercurialeclipse.ui.colorsandfonts.ConsoleFont"; //$NON-NLS-1$

	/** created colors for each line type - must be disposed at shutdown*/
	private Color commandColor;
	private Color messageColor;
	private Color errorColor;


	/** streams for each command type - each stream has its own color */
	private MessageConsoleStream commandStream;
	private MessageConsoleStream messageStream;
	private MessageConsoleStream errorStream;

	private final ConsoleDocument document;

	/** Indicates whether the console is visible in the Console view */
	private boolean visible;
	/** Indicates whether the console's streams have been initialized */
	private boolean initialized;

	/**
	 * Constructor initializes preferences and colors but doesn't create the
	 * console page yet.
	 */
	public HgConsole(ConsoleDocument doc) {
		super("Mercurial Console", HG_CONSOLE_TYPE, MercurialEclipsePlugin.getImageDescriptor("mercurialeclipse.png"), true); //$NON-NLS-1$ //$NON-NLS-2$
		document = doc;
	}

	@Override
	protected void init() {
		// Called when console is added to the console view
		super.init();
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		initLimitOutput(store);
		initWrapSetting(store);
		initializeStreams();
		dump();
	}

	private void initWrapSetting(IPreferenceStore store) {
		if (store.getBoolean(PREF_CONSOLE_WRAP)) {
			setConsoleWidth(store.getInt(PREF_CONSOLE_WIDTH));
		} else {
			setConsoleWidth(-1);
		}
	}

	private void initLimitOutput(IPreferenceStore store) {
		if (store.getBoolean(PREF_CONSOLE_LIMIT_OUTPUT)) {
			int highWaterMark = store.getInt(PREF_CONSOLE_HIGH_WATER_MARK);
			if (highWaterMark < 1000) {
				highWaterMark = 1000;
			}
			setWaterMarks(0, highWaterMark);
		} else {
			setWaterMarks(0, 1000);
		}
	}

	/**
	 * Initialize thre streams of the console. Must be called from the UI
	 * thread.
	 */
	private void initializeStreams() {
		synchronized (document) {
			if (initialized) {
				return;
			}
			commandStream = newMessageStream();
			errorStream = newMessageStream();
			messageStream = newMessageStream();
			IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
			// install colors
			commandColor = createColor(store, PREF_CONSOLE_COMMAND_COLOR);
			commandStream.setColor(commandColor);
			messageColor = createColor(store, PREF_CONSOLE_MESSAGE_COLOR);
			messageStream.setColor(messageColor);
			errorColor = createColor(store, PREF_CONSOLE_ERROR_COLOR);
			errorStream.setColor(errorColor);
			initialized = true;
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

	/**
	 * @see com.vectrace.MercurialEclipse.views.console.HgConsoleHolder.IHgConsole#appendLine(int, java.lang.String)
	 */
	public void appendLine(int type, String line) {
		HgConsoleHolder.getInstance().showConsole(false);
		String myLine = line == null? "" : line;
		synchronized (document) {
			if (visible) {
				switch (type) {
				case ConsoleDocument.COMMAND:
					commandStream.println(myLine);
					break;
				case ConsoleDocument.MESSAGE:
					messageStream.println(myLine);
					break;
				case ConsoleDocument.ERROR:
					errorStream.println(myLine);
					break;
				}
			} else {
				document.appendLine(type, myLine);
			}
		}
	}

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
	}

	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if(property == null || !property.startsWith("hg.console.")){
			return;
		}
		// colors
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		if (visible) {
			if (property.equals(PREF_CONSOLE_COMMAND_COLOR)) {
				Color newColor = createColor(store, PREF_CONSOLE_COMMAND_COLOR);
				commandStream.setColor(newColor);
				commandColor.dispose();
				commandColor = newColor;
			} else if (property.equals(PREF_CONSOLE_MESSAGE_COLOR)) {
				Color newColor = createColor(store, PREF_CONSOLE_MESSAGE_COLOR);
				messageStream.setColor(newColor);
				messageColor.dispose();
				messageColor = newColor;
			} else if (property.equals(PREF_CONSOLE_ERROR_COLOR)) {
				Color newColor = createColor(store, PREF_CONSOLE_ERROR_COLOR);
				errorStream.setColor(newColor);
				errorColor.dispose();
				errorColor = newColor;
				// font
			} else if (property.equals(PREF_CONSOLE_FONT)) {
				setFont(((FontRegistry) event.getSource()).get(PREF_CONSOLE_FONT));
			}
		}
		if (property.equals(PREF_CONSOLE_LIMIT_OUTPUT)) {
			initLimitOutput(store);
		} else if (property.equals(PREF_CONSOLE_WRAP)) {
			initWrapSetting(store);
		} else if (CONSOLE_FONT.equals(property)) {
			setConsoleFont();
		}
	}

	/**
	 * Returns a color instance based on data from a preference field.
	 */
	private static Color createColor(IPreferenceStore store, String preference) {
		Display display = MercurialEclipsePlugin.getStandardDisplay();
		RGB rgb = PreferenceConverter.getColor(store, preference);
		return new Color(display, rgb);
	}

	protected void setConsoleFont() {
		if (Display.getCurrent() == null) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
					Font font = theme.getFontRegistry().get(CONSOLE_FONT);
					setFont(font);
				}
			});
		} else {
			ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
			Font font = theme.getFontRegistry().get(CONSOLE_FONT);
			setFont(font);

		}
	}
}
