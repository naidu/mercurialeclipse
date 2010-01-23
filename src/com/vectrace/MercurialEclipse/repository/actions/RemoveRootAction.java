/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 *     Andrei Loskutov (Intland) - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository.actions;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * RemoveRootAction removes a repository
 */
public class RemoveRootAction extends SelectionListenerAction {
	private IStructuredSelection selection;

	public RemoveRootAction(Shell shell) {
		super("Remove Repository");
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("rem_co.gif"));
	}

	/**
	 * Returns the selected remote files
	 */
	@SuppressWarnings("unchecked")
	protected HgRepositoryLocation[] getSelectedRemoteRoots() {
		ArrayList<HgRepositoryLocation> resources = null;
		if (selection != null && !selection.isEmpty()) {
			resources = new ArrayList<HgRepositoryLocation>();
			Iterator<HgRepositoryLocation> elements = selection.iterator();
			while (elements.hasNext()) {
				Object next = HgAction.getAdapter(elements.next(),
						HgRepositoryLocation.class);
				if (next instanceof HgRepositoryLocation) {
					resources.add((HgRepositoryLocation) next);
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			HgRepositoryLocation[] result = new HgRepositoryLocation[resources
					.size()];
			resources.toArray(result);
			return result;
		}
		return new HgRepositoryLocation[0];
	}

	protected String getErrorTitle() {
		return "Error";
	}

	@Override
	public void run() {
		HgRepositoryLocation[] roots = getSelectedRemoteRoots();
		if (roots.length == 0) {
			return;
		}
		boolean confirm = MessageDialog.openConfirm(null, "Mercurial Repositories",
				"Remove repository (all authentication data will be lost)?");
		if(!confirm){
			return;
		}
		for (int i = 0; i < roots.length; i++) {
			MercurialEclipsePlugin.getRepoManager().disposeRepository(
						roots[i]);
		}
	}

	/**
	 * updates the selection. this selection will be used during run returns
	 * true if action can be enabled
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection sel) {
		this.selection = sel;

		HgRepositoryLocation[] roots = getSelectedRemoteRoots();
		return roots.length > 0;
	}

}
