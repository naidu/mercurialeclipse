package com.vectrace.MercurialEclipse.ui;

import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class ResourcesTreeContentProvider extends BaseWorkbenchContentProvider implements ITreeContentProvider {

	//the pseudo-root
	public final static Object ROOT = new Object();
	//the real roots
	private final Set<? extends IResource> roots;
	
	public ResourcesTreeContentProvider(Set<? extends IResource> roots) {
		super();
		this.roots = roots;
	}

	@Override
    public Object[] getChildren(Object parentElement) {
		if(parentElement == ROOT) {
			return roots.toArray(new IResource[0]);
		} else if(parentElement instanceof IContainer){
			try {
				return  ((IContainer)parentElement).members();
			} catch (CoreException e) {
				MercurialEclipsePlugin.logWarning(Messages.getString("ResourcesTreeContentProvider.failedToGetChildrenOf")+parentElement, e); //$NON-NLS-1$
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
    public Object getParent(Object element) {
		return ((IResource)element).getParent();
	}

	@Override
    public boolean hasChildren(Object element) {
		return element == ROOT || element instanceof IContainer;
	}

	@Override
    public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
    public void dispose() {
	}

	@Override
    public void inputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
	}

}
