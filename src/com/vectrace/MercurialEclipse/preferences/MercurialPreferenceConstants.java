/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Ahlberg - implementation
 *     Jerome Negre   - constants are now, well, constant
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public class MercurialPreferenceConstants
{
  public static final String MERCURIAL_EXECUTABLE = "hg";
  //user name should be per project in the future, different repositories could have different names (sub optimal I know but it really could)
  public static final String MERCURIAL_USERNAME = "user.name";
}


