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
package com.vectrace.MercurialEclipse.extensionpoint.definition;


public interface IMercurialEclipseActionListener {
	void onAmend(String obsoleteChangeSet, String newChangeSet);
	void onBeforeRebase(String obsoleteSourceChangeSet, String obsoleteDestinationChangeSet);
	void onRebase(String newChangeSet);
	void onStrip(String obsoleteChangeSet);
}
