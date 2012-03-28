/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - removeResources()
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.aragost.javahg.commands.flags.RemoveCommandFlags;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgRemoveClient extends AbstractClient {

	/**
	 * Remove a resource with --force flag
	 * @return True if the resource was removed
	 */
	public static boolean forceRemoveResource(IResource resource, IProgressMonitor monitor) {
		if (monitor != null) {
			monitor.subTask(Messages.getString("HgRemoveClient.removeResource.1") + resource.getName() //$NON-NLS-1$
					+ Messages.getString("HgRemoveClient.removeResource.2")); //$NON-NLS-1$
		}
		HgRoot hgRoot = MercurialRootCache.getInstance().getHgRoot(resource);

		List<File> removedFiles = RemoveCommandFlags.on(hgRoot.getRepository()).force().execute(ResourceUtils.getFileHandle(resource));

		return !removedFiles.isEmpty();
	}

	public static void removeResources(List<IResource> resources) {
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);

		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot hgRoot = mapEntry.getKey();
			List<IResource> res = mapEntry.getValue();
			File[] files = new File[res.size()];

			for (int i = 0; i < files.length; i++) {
				files[i] = ResourceUtils.getFileHandle(res.get(i));
			}

			RemoveCommandFlags.on(hgRoot.getRepository()).execute(files);
		}
	}

	/**
	 * Remove resources with --force and --after flags
	 */
	public static void removeResourcesLater(Map<HgRoot, List<IResource>> resourcesByRoot) {
		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot hgRoot = mapEntry.getKey();
			List<IResource> res = mapEntry.getValue();
			File[] files = new File[res.size()];

			for (int i = 0; i < files.length; i++) {
				files[i] = ResourceUtils.getFileHandle(res.get(i));
			}

			RemoveCommandFlags.on(hgRoot.getRepository()).after().force().execute(files);
		}
	}
}
