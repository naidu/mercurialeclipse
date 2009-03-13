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
    public static final String MERCURIAL_EXECUTABLE = "hg"; //$NON-NLS-1$
    public static final String GPG_EXECUTABLE = "hg.gpg.executable"; //$NON-NLS-1$

    // user name should be per project in the future, different repositories
    // could have different names (sub optimal I know but it really could)
    public static final String MERCURIAL_USERNAME = "user.name"; //$NON-NLS-1$
    public static final String PREF_DEFAULT_ENCODING = "UTF-8"; //$NON-NLS-1$

    // label decorator
    public static final String LABELDECORATOR_LOGIC = "hg.labeldecorator.logic"; //$NON-NLS-1$
    public static final String LABELDECORATOR_LOGIC_2MM = "2-means-modified"; //$NON-NLS-1$
    public static final String LABELDECORATOR_LOGIC_HB = "high-bit"; //$NON-NLS-1$
    public static final String RESOURCE_DECORATOR_COMPLETE_STATUS = "hg.performance.getStatusForCompleteRepository"; //$NON-NLS-1$
    public static final String RESOURCE_DECORATOR_COMPUTE_DEEP_STATUS = "hg.performance.computeDeepStatus"; //$NON-NLS-1$
    public static final String RESOURCE_DECORATOR_SHOW_CHANGESET = "hg.performance.fileShowsChangeset"; //$NON-NLS-1$
    public static final String PREF_DECORATE_WITH_COLORS = "hg.labeldecorator.colors"; //$NON-NLS-1$
    public static String PREF_AUTO_SHARE_PROJECTS = "hg.autoshare"; //$NON-NLS-1$
    
    // Timeouts
    public static final String DEFAULT_TIMEOUT = "hg.timeout.default"; //$NON-NLS-1$
    public static final String CLONE_TIMEOUT = "hg.timeout.clone"; //$NON-NLS-1$
    public static final String PUSH_TIMEOUT = "hg.timeout.push"; //$NON-NLS-1$
    public static final String PULL_TIMEOUT = "hg.timeout.pull"; //$NON-NLS-1$
    public static final String UPDATE_TIMEOUT = "hg.timeout.update"; //$NON-NLS-1$
    public static final String COMMIT_TIMEOUT = "hg.timeout.commit"; //$NON-NLS-1$
    public static final String IMERGE_TIMEOUT = "hg.timeout.imerge"; //$NON-NLS-1$
    public static final String LOG_TIMEOUT = "hg.timeout.log"; //$NON-NLS-1$
    public static final String STATUS_TIMEOUT = "hg.timeout.status"; //$NON-NLS-1$
    public static final String ADD_TIMEOUT = "hg.timeout.add"; //$NON-NLS-1$
    public static final String REMOVE_TIMEOUT = "hg.timeout.remove"; //$NON-NLS-1$

    // batch sizes
    public static final String LOG_BATCH_SIZE = "hg.batchsize.log"; //$NON-NLS-1$
    public static final String STATUS_BATCH_SIZE = "hg.batchsize.status"; //$NON-NLS-1$
    public static final String COMMIT_MESSAGE_BATCH_SIZE = "hg.batchsize.commitmessage"; //$NON-NLS-1$

    // synchronize
    public static final String SYNCHRONIZE_FILES = "hg.synchronize.synchronizeOnFileLevel"; //$NON-NLS-1$

    // console
    public static final String PREF_CONSOLE_SHOW_ON_MESSAGE = "hg.console.showOnMessage"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_WRAP = "hg.console.wrap"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_WIDTH = "hg.console.width"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_LIMIT_OUTPUT = "hg.console.limitOutput"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_HIGH_WATER_MARK = "hg.console.highWaterMark"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_COMMAND_COLOR = "hg.console.command_color"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_MESSAGE_COLOR = "hg.console.message_color"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_ERROR_COLOR = "hg.console.error_color"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_FONT = "hg.console.font"; //$NON-NLS-1$
    public static final String PREF_CONSOLE_DEBUG = "hg.console.debug"; //$NON-NLS-1$

    // merge
    public static final String PREF_USE_EXTERNAL_MERGE = "hg.merge.useExternal"; //$NON-NLS-1$
    
}
