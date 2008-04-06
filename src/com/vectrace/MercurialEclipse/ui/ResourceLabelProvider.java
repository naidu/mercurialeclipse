package com.vectrace.MercurialEclipse.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

public class ResourceLabelProvider extends LabelProvider {
	
	ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
	
	@Override
	public Image getImage(Object element) {
		if(element instanceof IFile) {
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
		} else if(element instanceof IFolder) {
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		} else if(element instanceof IProject) {
			return sharedImages.getImage(SharedImages.IMG_OBJ_PROJECT);
		} else {
			return null;
		}
	}

	@Override
	public String getText(Object element) {
		return ((IResource)element).getName();
	}
}
