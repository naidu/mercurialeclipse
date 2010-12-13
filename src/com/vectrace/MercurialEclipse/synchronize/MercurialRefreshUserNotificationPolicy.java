/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Soren Mathiasen	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.team.internal.ui.synchronize.IRefreshEvent;
import org.eclipse.team.internal.ui.synchronize.RefreshUserNotificationPolicy;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * @author Soren Mathiasen
 *
 */
@SuppressWarnings("restriction")
public class MercurialRefreshUserNotificationPolicy extends RefreshUserNotificationPolicy {

	/**
	 * @param participant
	 */
	public MercurialRefreshUserNotificationPolicy(ISynchronizeParticipant participant) {
		super(participant);
	}
	/**
	 * @see org.eclipse.team.internal.ui.synchronize.RefreshUserNotificationPolicy#handleRefreshDone(org.eclipse.team.internal.ui.synchronize.IRefreshEvent, boolean)
	 */
	@Override
	protected boolean handleRefreshDone(IRefreshEvent event, boolean prompt) {
		// set prompt to false in order to disable on changes found dialog.
		// TODO There must be a better way of doing this !
		return super.handleRefreshDone(event, false);
	}

}
