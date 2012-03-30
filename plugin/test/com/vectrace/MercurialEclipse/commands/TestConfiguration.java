/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Stefan	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author Stefan
 *
 */
public class TestConfiguration extends TestCase implements IConsole,
		IErrorHandler, IConfiguration {
	private final Map<String, String> preferences = new HashMap<String, String>() {
		{
			put(MercurialPreferenceConstants.PREF_CONSOLE_DEBUG, "true");
		}
	};

	public void test(){
		// just a dumy method for JUnit to avoid test failure because of missing test
	}

	public void logError(Throwable e) {
		fail(e.getMessage());
	}

	public void logWarning(String message, Throwable e) {
		fail(e.getMessage());
	}

	public String getExecutable() {
		String path = "hg";
		// path = "hg";
		return path;
	}

	public int getTimeOut(String commandId) {
		return 12000;
	}

	public void commandCompleted(int exitCode, long timeInMillis,  String message, Throwable error) {
		System.out.println(exitCode + " - " + message);
		if (error != null) {
			error.printStackTrace(System.err);
		}
	}

	public void commandInvoked(String command) {
		System.out.println(command);
	}

	public void printError(String message, Throwable root) {
		System.err.println(message);
		root.printStackTrace(System.err);
	}

	public void printMessage(String message, Throwable root) {
		System.out.println(message);
		if (root != null) {
			root.printStackTrace(System.out);
		}
	}

	public String getPreference(String preferenceConstant,
			String defaultIfNotSet) {
		String pref = preferences.get(preferenceConstant);
		if (pref != null) {
			return pref;
		}
		return defaultIfNotSet;
	}
}
