package com.vectrace.MercurialEclipse.team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public abstract class MultipleFilesAction implements IActionDelegate {

	private List<IFile> selection;

	public MultipleFilesAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = new ArrayList<IFile>();
			extractFiles(
					this.selection,
					Arrays.asList(((IStructuredSelection)selection).toArray()).toArray(new IResource[0]));
		}
	}
	
	private void extractFiles(List<IFile> list, IResource[] resources) {
		for(IResource resource : resources) {
			if(resource instanceof IFile) {
				list.add((IFile)resource);
			} else if(resource instanceof IContainer) {
				try {
					extractFiles(list, ((IContainer)resource).members());
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}
			} else {
				MercurialEclipsePlugin.logWarning("Unexpected resource type: "+resource.getClass(), null);
			}
		}
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	protected List<IFile> getSelectedFiles() {
		return selection;
	}

	public void run(IAction action) {
		try {
			run(getSelectedFiles());
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
		}
	}
		
	protected abstract void run(List<IFile> files) throws Exception ;
}