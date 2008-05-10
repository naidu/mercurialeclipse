/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Ahlberg - implementation
 *     Jérôme Nègre   - constants are now, well, constant
 *     Bastian Doetsch
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public class MercurialPreferenceConstants {
    // executables
    public static final String MERCURIAL_EXECUTABLE = "hg";
    public static final String GPG_EXECUTABLE = "hg.gpg.executable";
    
    // user name should be per project in the future, different repositories
    // could have different names (sub optimal I know but it really could)
    public static final String MERCURIAL_USERNAME = "user.name";
    
    // label decorator
    public static final String LABELDECORATOR_LOGIC = "hg.labeldecorator.logic";
    public static final String LABELDECORATOR_LOGIC_2MM = "2-means-modified";
    public static final String LABELDECORATOR_LOGIC_HB = "high-bit";
    
    // Timeouts
    public static final String DefaultTimeout = "hg.timeout.default";
    public static final String CloneTimeout = "hg.timeout.clone";
    public static final String PushTimeout = "hg.timeout.push";
    public static final String PullTimeout = "hg.timeout.pull";
    public static final String UpdateTimeout = "hg.timeout.update";
    public static final String CommitTimeout = "hg.timeout.commit";
    public static final String ImergeTimeout = "hg.timeout.imerge";
    public static final String LogTimeout = "hg.timeout.log";
    public static final String StatusTimeout = "hg.timeout.status";
    public static final String AddTimeout = "hg.timeout.add";
    public static final String RemoveTimeout = "hg.timeout.remove";
    
    // batch sizes
    public static final String LogBatchSize = "hg.batchsize.log";
    public static final String StatusBatchSize = "hg.batchsize.status";
}
