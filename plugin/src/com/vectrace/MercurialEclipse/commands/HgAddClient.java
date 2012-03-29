/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.aragost.javahg.commands.flags.AddCommandFlags;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgAddClient extends AbstractClient {

	public static void addResources(List<IResource> resources,
			IProgressMonitor monitor) {
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);
		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot hgRoot = mapEntry.getKey();

			if (monitor != null) {
				monitor.subTask(Messages.getString("HgAddClient.addingResourcesFrom") + hgRoot.getName()); //$NON-NLS-1$
			}

			AddCommandFlags.on(hgRoot.getRepository()).execute(toFileArray(mapEntry.getValue()));
		}
	}
}
