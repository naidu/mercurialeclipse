/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - changes
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;

import com.vectrace.MercurialEclipse.model.FlaggedResource;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class ResourceAdapterFactory implements IAdapterFactory {

    @SuppressWarnings("unchecked")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IResource.class) {
            try {
                IResource resource = (IResource) adaptableObject;
                IProject project = resource.getProject();

                //abort if not in hg
                if (project == null || !MercurialTeamProvider.isHgTeamProviderFor(project)) {
                    return null;
                }
                return resource;
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
                return null;
            }

        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Class[] getAdapterList() {
        return new Class[] { FlaggedResource.class };
    }

}
