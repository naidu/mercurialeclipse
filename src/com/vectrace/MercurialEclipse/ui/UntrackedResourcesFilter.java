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
		String path = resource.getProjectRelativePath().toOSString();
		
		if(resource.getType() == IResource.FILE) {
			Set<IPath> set = untrackedFiles.get(project);
			return isSubPath(path, set);
		} else if(resource.getType() == IResource.FOLDER){
		    Set<IPath> set = untrackedFolders.get(project);
			return isSubPath(path, set);
		} else {
			return true;
		}
	}

    /**
     * @param path
     * @param set
     * @return
     */
    private boolean isSubPath(String path, Set<IPath> set) {
        for (IPath setPath : set) {
            String setPathString = setPath.toOSString();
            if (setPathString.endsWith(path)) {
                return true;
            }
        }
        return false;
    }

}
