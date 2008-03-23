package com.vectrace.MercurialEclipse.ui;

import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class ResourcesTreeContentProvider implements ITreeContentProvider {

	//the pseudo-root
	public final static Object ROOT = new Object();
	//the real roots
	private final Set<? extends IResource> roots;
	
	public ResourcesTreeContentProvider(Set<? extends IResource> roots) {
		super();
		this.roots = roots;
	}

	public Object[] getChildren(Object parentElement) {
		if(parentElement == ROOT) {
			return roots.toArray(new IResource[0]);
		} else if(parentElement instanceof IContainer){
			try {
				return  ((IContainer)parentElement).members();
			} catch (CoreException e) {
				MercurialEclipsePlugin.logWarning("Failed to get children of "+parentElement, e);
				return null;
			}
		} else {
			return null;
		}
	}

	public Object getParent(Object element) {
		return ((IResource)element).getParent();
	}

	public boolean hasChildren(Object element) {
		return element == ROOT || element instanceof IContainer;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
	}

}
