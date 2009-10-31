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
package com.vectrace.MercurialEclipse.model;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class FlaggedProject {

    private Map<IResource, FlaggedResource> resources;
    private String version;

    public FlaggedProject(IProject project, Map<IResource, FlaggedResource> resources,
            String version) {
        this.resources = resources;
        this.version = version;
    }

    public FlaggedResource getFlaggedResource(IResource resource) {
        return resources.get(resource);
    }

    public String getVersion() {
        return version;
    }

}
