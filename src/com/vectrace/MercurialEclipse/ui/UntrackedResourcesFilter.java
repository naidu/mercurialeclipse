package com.vectrace.MercurialEclipse.ui;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class UntrackedResourcesFilter extends ViewerFilter {

	private final Map<IProject, Set<IPath>> untrackedFiles;
	private final Map<IProject, Set<IPath>> untrackedFolders;
	
	public UntrackedResourcesFilter(Map<IProject, Set<IPath>> untrackedFiles,
			Map<IProject, Set<IPath>> untrackedFolders) {
		super();
		this.untrackedFiles = untrackedFiles;
		this.untrackedFolders = untrackedFolders;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement,
			Object element) {
		
		IResource resource = (IResource) element;
		IProject project = resource.getProject();
		IPath path = resource.getProjectRelativePath();
		if(resource.getType() == IResource.FILE) {
			return untrackedFiles.get(project).contains(path);
		} else if(resource.getType() == IResource.FOLDER){
			return untrackedFolders.get(project).contains(path);
		} else {
			return true;
		}
	}

}
