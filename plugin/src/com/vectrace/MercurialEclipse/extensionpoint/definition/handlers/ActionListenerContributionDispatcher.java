/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * svetlana.daragatch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.extensionpoint.definition.handlers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.extensionpoint.definition.IMercurialEclipseActionListener;

public class ActionListenerContributionDispatcher {
	private static final String EXTENSION_POINT_ID = "com.vectrace.MercurialEclipse.actionListener";

	public static void onAmend(final String obsoleteChangeSet, final String newChangeSet) {
		dispatch(new Action() {
			public void run() throws Exception {
				actionListener.onAmend(obsoleteChangeSet, newChangeSet);
			}
		});
	}

	public static void onBeforeRebase(final String obsoleteSourceChangeSet, final String obsoleteDestinationChangeSet) {
		dispatch(new Action() {
			public void run() throws Exception {
				actionListener.onBeforeRebase(obsoleteSourceChangeSet, obsoleteDestinationChangeSet);
			}
		});
	}

	public static void onRebase(final String newChangeSet) {
		dispatch(new Action() {
			public void run() throws Exception {
				actionListener.onRebase(newChangeSet);
			}
		});
	}

	public static void onStrip(final String obsoleteChangeSet) {
		dispatch(new Action() {
			public void run() throws Exception {
				actionListener.onStrip(obsoleteChangeSet);
			}
		});
	}

	private static void dispatch(Action runnable) {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] config = registry.getConfigurationElementsFor(EXTENSION_POINT_ID);

		try {
			for (IConfigurationElement e : config) {
				final Object o = e.createExecutableExtension("class");

				if (o instanceof IMercurialEclipseActionListener) {
					runnable.setActionListener((IMercurialEclipseActionListener) o);
					SafeRunner.run(runnable);
				}
			}
		} catch (CoreException ex) {
			MercurialEclipsePlugin.logError("ActionListenerContributionHandler:executeExtension Exception", ex);
		}
	}

	private static abstract class Action implements ISafeRunnable {
		IMercurialEclipseActionListener actionListener;

		public void setActionListener(IMercurialEclipseActionListener actionListener) {
			this.actionListener = actionListener;
		}

		public void handleException(Throwable ex) {
			MercurialEclipsePlugin.logError("ActionListenerContributionHandler:executeExtension Exception", ex);
		}
	}
}
