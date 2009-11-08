package com.vectrace.MercurialEclipse.team;

import java.util.ArrayList;
import java.util.List;

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
public abstract class MultipleResourcesAction implements IActionDelegate {

	private List<IResource> selection;

	public MultipleResourcesAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			this.selection = new ArrayList<IResource>();
			for(Object o : ((IStructuredSelection)sel).toArray()) {
				this.selection.add((IResource)o);
			}
		}
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	protected List<IResource> getSelectedResources() {
		return selection;
	}

	public void run(IAction action) {
		try {
			run(getSelectedResources());
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			MessageDialog.openError(getShell(), Messages.getString("MultipleResourcesAction.hgSays"), e.getMessage()+Messages.getString("MultipleResourcesAction.seeErrorLog")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected abstract void run(List<IResource> resources) throws Exception ;
}