package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
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
public abstract class SingleResourceAction implements IActionDelegate {

	private IResource selection;

	public SingleResourceAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			//the xml enables this action only for a selection of a single resource
			this.selection = (IResource)((IStructuredSelection) selection).getFirstElement();
		}
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	protected IResource getSelectedResource() {
		return selection;
	}

	public void run(IAction action) {
		try {
			run(getSelectedResource());
		} catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
		    MessageDialog.openError(getShell(), "Hg says...", e.getMessage()+"\nSee Error Log for more details.");
		}
	}
	
	protected abstract void run(IResource resource) throws Exception ;
}