/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ijuma                 - implementation
 *     Andrei Loskutov       - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views.console;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.themes.ITheme;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * This class should only be called from the UI thread as it is not thread-safe.
 */
public final class HgConsoleHolder implements IConsoleListener, IPropertyChangeListener {
	private static final HgConsoleHolder INSTANCE = new HgConsoleHolder();

	/**
	 * Constant used for indenting error status printing
	 */
	private static final String NESTING = "   "; //$NON-NLS-1$

	/**
	 * Initially null
	 */
	private volatile HgConsole console;

	/**
	 * The document to use for {@link #console} when it is instantiated.
	 */
	private final ConsoleDocument consoleDocument = new ConsoleDocument();

	private boolean showOnMessage;
	private boolean registered;

	private boolean debugTimeEnabled;
	private boolean debugEnabled;

	/**
	 * Retained so the logger is not garbage collected before JavaHg itself loads.
	 */
	private Logger javaHgLogger;

	private boolean bInitialized;

	private HgConsoleHolder() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		debugTimeEnabled = store.getBoolean(PREF_CONSOLE_DEBUG_TIME);
		debugEnabled = store.getBoolean(PREF_CONSOLE_DEBUG);
	}

	public static HgConsoleHolder getInstance() {
		return INSTANCE;
	}

	/**
	 * If force then instantiates {@link #console} if it is not already instantiated. Note: may be asynchronous.
	 *
	 * @param force Whether to instantiate {@link #console} if it is not already
	 */
	private void init(boolean force) {
		if (console != null || (bInitialized && !force)) {
			return;
		}

		synchronized(this){
			if (console != null || (bInitialized && !force)) {
				return;
			}

			if (!bInitialized) {
				IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();

				store.addPropertyChangeListener(this);

				showOnMessage = store.getBoolean(PREF_CONSOLE_SHOW_ON_MESSAGE);
				handleJavaHgLogging();

				if (!PlatformUI.isWorkbenchRunning()) {
					showOnMessage = false;
				}
				bInitialized = true;
			}
			if (force && console == null) {
				// install font
				// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=298795
				// we must run this stupid code in the UI thread
				if (Display.getCurrent() != null) {
					initConsole();
				} else {
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
						public void run() {
							initConsole();
						}
					});
				}
			}
		}
	}

	protected void initConsole() {
		console = new HgConsole(consoleDocument);
		getConsoleManager().addConsoleListener(this);

		ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
		theme.addPropertyChangeListener(this);
		JFaceResources.getFontRegistry().addListener(this);
		console.setConsoleFont();
	}

	/**
	 * Install a handler so JavaHg's logging through the java.util.logging API will be shown in the
	 * console view.
	 */
	private void handleJavaHgLogging() {
		javaHgLogger = Logger.getLogger("com.aragost.javahg");
		javaHgLogger.setLevel(Level.INFO);

		Handler h = new Handler() {

			@Override
			public void publish(LogRecord record) {
				log(record);
			}

			@Override
			public void flush() {
			}

			@Override
			public void close() throws SecurityException {
			}
		};

		javaHgLogger.addHandler(h);
	}

	public HgConsole showConsole(boolean force) {
		force |= showOnMessage;

		init(force);

		if (force) {
			// register console
			if(Display.getCurrent() == null){
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
					public void run() {
						registerConsole();
						getConsoleManager().showConsoleView(console);
					}
				});
			} else {
				registerConsole();
				getConsoleManager().showConsoleView(console);
			}
		}

		return console;
	}

	private void registerConsole() {
		boolean exists = isConsoleRegistered();
		if (!exists) {
			getConsoleManager().addConsoles(new IConsole[] { console });
		}
	}

	private boolean isConsoleRegistered() {
		if(registered){
			return true;
		}
		IConsole[] existing = getConsoleManager().getConsoles();
		for (int i = 0; i < existing.length; i++) {
			if (console == existing[i]) {
				registered = true;
			}
		}
		return registered;
	}

	private IHgConsole getConsole() {
		init(false);

		return (console == null) ? consoleDocument : console;
	}

	public void consolesAdded(IConsole[] consoles) {
		// noop
	}

	public void consolesRemoved(IConsole[] consoles) {
		for (int i = 0; i < consoles.length; i++) {
			IConsole c = consoles[i];
			if (c == console) {
				registered = false;
				console.dispose();
				console = null;
				JFaceResources.getFontRegistry().removeListener(this);
				MercurialEclipsePlugin.getDefault().getPreferenceStore()
						.removePropertyChangeListener(this);
				ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				theme.removePropertyChangeListener(this);
				break;
			}
		}

	}

	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();

		if(PREF_CONSOLE_SHOW_ON_MESSAGE.equals(event.getProperty())){
			showOnMessage = store.getBoolean(PREF_CONSOLE_SHOW_ON_MESSAGE);
		} else if (property.equals(PREF_CONSOLE_DEBUG_TIME)) {
			debugTimeEnabled = store.getBoolean(PREF_CONSOLE_DEBUG_TIME);
		} else if (property.equals(PREF_CONSOLE_DEBUG)) {
			debugEnabled = store.getBoolean(PREF_CONSOLE_DEBUG);
		} else if (console != null) {
			console.propertyChange(event);
		}
	}

	private static IConsoleManager getConsoleManager() {
		return ConsolePlugin.getDefault().getConsoleManager();
	}

	public void commandInvoked(String line) {
		getConsole().appendLine(ConsoleDocument.COMMAND, line);
	}

	public void messageLineReceived(String line) {
		getConsole().appendLine(ConsoleDocument.MESSAGE, line);
	}

	public void errorLineReceived(String line) {
		getConsole().appendLine(ConsoleDocument.ERROR, line);
	}

	protected void log(LogRecord record) {
		int type = (Level.INFO.intValue() < record.getLevel().intValue()) ? ConsoleDocument.ERROR
				: ConsoleDocument.MESSAGE;
		String loggerName = record.getLoggerName();
		int index;

		if (loggerName != null && (index =  loggerName.lastIndexOf('.')) >= 0) {
			loggerName = loggerName.substring(index + 1);
		}

		getConsole().appendLine(type, loggerName + ": " + record.getMessage());
	}

	public void commandCompleted(long timeInMillis, IStatus status, Throwable exception) {

		String time = getTimeString(timeInMillis);
		if (status != null) {
			if(status.getSeverity() == IStatus.ERROR) {
				printStatus(status, time, false);
			} else if(debugEnabled){
				printStatus(status, time, true);
			} else if(debugTimeEnabled){
				getConsole().appendLine(ConsoleDocument.MESSAGE, time);
			}
		} else if (exception != null) {
			String statusText;
			if (exception instanceof OperationCanceledException) {
				statusText = Messages.getString("HgConsole.aborted1") + time + Messages.getString("HgConsole.aborted2"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				statusText = time;
			}
			getConsole().appendLine(ConsoleDocument.COMMAND, statusText);
			if (exception instanceof CoreException) {
				outputStatus(((CoreException) exception).getStatus(), true, 1);
			}
		} else if(debugTimeEnabled){
			getConsole().appendLine(ConsoleDocument.MESSAGE, time);
		}
	}

	private void printStatus(IStatus status, String time, boolean includeRoot) {
		String statusText = status.getMessage();
		if(time.length() > 0){
			statusText += "(" + time.trim() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		int kind = status.getSeverity() == IStatus.ERROR? ConsoleDocument.ERROR : ConsoleDocument.MESSAGE;
		getConsole().appendLine(kind, statusText);
		outputStatus(status, includeRoot, includeRoot ? 0 : 1);
	}

	/**
	 *
	 * @param timeInMillis
	 * @return empty string if time measurement was not enabled or we are failed to measure it
	 */
	private String getTimeString(long timeInMillis) {
		if(!debugTimeEnabled){
			return "";
		}
		String time;
		try {
			time = String.format("  Done in %1$tM:%1$tS:%1$tL", Long.valueOf(timeInMillis));
		} catch (RuntimeException e) {
			MercurialEclipsePlugin.logError(e);
			time = "";
		}
		return time;
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
		getConsole().appendLine(ConsoleDocument.COMMAND, buffer.toString());
	}

	/**
	 * Returns the NLSd message based on the status returned from the Hg
	 * command.
	 *
	 * @param status
	 *            an NLSd message based on the status returned from the Hg
	 *            command.
	 */
	private static String messageLineForStatus(IStatus status) {
		if (status.getSeverity() == IStatus.ERROR) {
			return Messages.getString("HgConsole.error") + status.getMessage(); //$NON-NLS-1$
		} else if (status.getSeverity() == IStatus.WARNING) {
			return Messages.getString("HgConsole.warning") + status.getMessage(); //$NON-NLS-1$
		} else if (status.getSeverity() == IStatus.INFO) {
			return Messages.getString("HgConsole.info") + status.getMessage(); //$NON-NLS-1$
		}
		return status.getMessage();
	}

	public static interface IHgConsole {

		/**
		 * Appends a line of the specified type to the end of the console.
		 */
		void appendLine(int type, String string);
	}
}
