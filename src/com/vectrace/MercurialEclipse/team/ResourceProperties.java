/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Contains the name of the properties set on IResources.
 * 
 * @see IResource#setPersistentProperty(QualifiedName, String)
 * @see IResource#setSessionProperty(QualifiedName, Object)
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class ResourceProperties {

    private ResourceProperties(){
    }
    
    /**
     * IProject PersistentProperty
     */
    public final static QualifiedName MERGING = new QualifiedName(
            MercurialEclipsePlugin.ID,
            "merging");
    
}
