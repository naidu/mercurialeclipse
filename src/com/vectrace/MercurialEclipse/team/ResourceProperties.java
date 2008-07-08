/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Bastian Doetsch - javadocs and new qualified name MERGE_COMMIT_OFFERED
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.menu.MergeHandler;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.views.MergeView;

/**
 * Contains the name of the properties set on IResources.
 * 
 * @see IResource#setPersistentProperty(QualifiedName, String)
 * @see IResource#setSessionProperty(QualifiedName, Object)
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class ResourceProperties {

    /**
     * Qualified name for a persistent property that signifies that a project is
     * in merge state.
     * 
     * @see MergeHandler
     * @see MercurialStatusCache
     * @see CommitMergeHandler
     * @see ResourceDecorator
     */
    public final static QualifiedName MERGING = new QualifiedName(
            MercurialEclipsePlugin.ID, "merging");

    /**
     * Qualified name for a session property on a project that signifies that
     * the commit dialog has already been shown, so the dialog doesn't pop up
     * automatically anymore.
     * 
     * @see MergeView
     * @see CommitMergeHandler
     */
    public static final QualifiedName MERGE_COMMIT_OFFERED = new QualifiedName(
            MercurialEclipsePlugin.ID, MergeView.ID + ".commitOffered");
    
    /**
     * Qualified name for a workspace session property that signifies whether
     * to use Imerge extension or hg resolve for merging. Makes MercurialEclipse
     * check exactly once for hg resolve and remember it until Eclipse is restarted.
     */
    public static final QualifiedName MERGE_USE_RESOLVE = new QualifiedName(
            MercurialEclipsePlugin.ID, MergeView.ID + ".useResolve");

    private ResourceProperties() {
    }

}
