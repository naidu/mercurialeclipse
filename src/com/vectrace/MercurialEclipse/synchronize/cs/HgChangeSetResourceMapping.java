/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *	   IBM Corporation - initial API and implementation
 *     Andrei Loskutov (Intland) - adopting to hg
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.model.ChangeSet;

public class HgChangeSetResourceMapping extends ResourceMapping {

	private final ChangeSet changeSet;

	public HgChangeSetResourceMapping(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}

	@Override
	public Object getModelObject() {
		return changeSet;
	}

	@Override
	public String getModelProviderId() {
		return HgChangeSetModelProvider.ID;
	}

	@Override
	public IProject[] getProjects() {
		Set<IProject> result = new HashSet<IProject>();
		Set<IFile> files = changeSet.getFiles();
		for (IFile file : files) {
			result.add(file.getProject());
		}
		return result.toArray(new IProject[result.size()]);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		IResource[] resources = changeSet.getResources();
		if (resources.length == 0) {
			return new ResourceTraversal[0];
		}
		return new ResourceTraversal[] {
				new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE)
		};
	}

}
