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

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;

import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * Console factory is used to show the console from the Console view "Open Console"
 * drop-down action. This factory is registered via the org.eclipse.ui.console.consoleFactory 
 * extension point. 
 * 
 * @since 3.1
 */
public class HgConsoleFactory implements IConsoleFactory {

	public HgConsoleFactory() {
	}
	
	public void openConsole() {
		showConsole();
	}
	
	public static void showConsole() {
		HgConsole console = MercurialUtilities.getMercurialConsole();
		if (console != null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] existing = manager.getConsoles();
			boolean exists = false;
			for (int i = 0; i < existing.length; i++) {
				if(console == existing[i]) {
                    exists = true;
                }
			}
			if(! exists) {
                manager.addConsoles(new IConsole[] {console});
            }
			manager.showConsoleView(console);
		}
	}
	
	public static void closeConsole() {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		HgConsole console = MercurialUtilities.getMercurialConsole();
		if (console != null) {
			manager.removeConsoles(new IConsole[] {console});
			ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(console.new MyLifecycle());
		}
	}
}
