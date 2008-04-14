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
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgUpdateClient;

public class UpdateHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        HgUpdateClient.update(project, null, false);
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        // will trigger a FlagManager refresh
    }

}
