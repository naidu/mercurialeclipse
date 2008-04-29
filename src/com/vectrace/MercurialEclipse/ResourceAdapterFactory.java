/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import java.util.BitSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.model.FlaggedResource;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class ResourceAdapterFactory implements IAdapterFactory {

    @SuppressWarnings("unchecked")
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == FlaggedResource.class) {
            try {
                IResource resource = (IResource) adaptableObject;
                IProject project = resource.getProject();

                //abort if not in hg
                if (RepositoryProvider.getProvider(project, MercurialTeamProvider.ID) == null) {
                    return null;
                }

//                FlagManager manager = MercurialEclipsePlugin.getDefault().getFlagManager();
//                FlaggedProject fproject = manager.getFlaggedProject(project);
//                if (fproject == null) {
//                    fproject = manager.refresh(project);
//                }
//                return fproject.getFlaggedResource(resource);
                BitSet status = MercurialStatusCache.getInstance().getStatus(resource);
                FlaggedResource fResource = new FlaggedResource(resource,status);
                return fResource;
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
