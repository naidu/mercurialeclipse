package com.vectrace.MercurialEclipse.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

public class ResourceLabelProvider extends LabelProvider {

    ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();

    @Override
    public Image getImage(Object element) {

	if (element instanceof IAdaptable) {
	    IResource res = (IResource) ((IAdaptable) element)
		    .getAdapter(IResource.class);
	    if (res != null) {
		switch (res.getType()) {
		case IResource.FILE:
		    return sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
		case IResource.FOLDER:
		    return sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		case IResource.PROJECT:
		    return sharedImages.getImage(SharedImages.IMG_OBJ_PROJECT);
		}
	    }
	}
	return null;
    }

    @Override
    public String getText(Object element) {
	if (element instanceof IAdaptable) {
	    IResource res = (IResource) ((IAdaptable) element)
		    .getAdapter(IResource.class);
	    if (res != null) {
		return res.getName();
	    }
	}
	return null;
    }
}
