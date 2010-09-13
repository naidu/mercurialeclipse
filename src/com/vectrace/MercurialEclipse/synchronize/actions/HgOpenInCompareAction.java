/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ilya Ivanov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.Iterator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ModelCompareEditorInput;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;

import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant.MercurialCompareInput;

/**
 * @author Ilya Ivanov (Intland) - implementation
 *
 * @see OpenInCompareAction
 */

@SuppressWarnings("restriction")
public class HgOpenInCompareAction extends Action {

	private final ISynchronizePageConfiguration configuration;

	public HgOpenInCompareAction(Action defaultOpen, ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void run() {
		ISelection selection = configuration.getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection) {
			if (!isOkToRun(selection)) {
				return;
			}

			boolean reuseEditorIfPossible = ((IStructuredSelection) selection).size() == 1;
			for (Iterator<?> iterator = ((IStructuredSelection) selection).iterator(); iterator.hasNext();) {
				Object obj = iterator.next();
				if (obj instanceof FileFromChangeSet) {
					FileFromChangeSet fcs = (FileFromChangeSet) obj;
					IWorkbenchSite ws = configuration.getSite().getWorkbenchSite();
					if (ws instanceof IWorkbenchPartSite) {
						IEditorPart part = findOpenCompareEditor((IWorkbenchPartSite) ws, fcs, configuration);
						if (part != null) {
							// editor found
							ws.getPage().activate(part);
							return;
						}
					}
				}
				OpenInCompareAction.openCompareEditor(configuration, obj, !OpenStrategy.activateOnOpen(), reuseEditorIfPossible);
			}
		}
	}

	/**
	 * Returns an editor handle if a MercurialCompareInput compare editor is opened on
	 * the given IResource.
	 *
	 * @param site the view site in which to search for editors
	 * @param configuration2
	 * @param resource the resource to use to find the compare editor
	 * @return an editor handle if found and <code>null</code> otherwise
	 *
	 * @see OpenInCompareAction#findOpenCompareEditor(IWorkbenchPartSite, IResource)
	 */
	public static IEditorPart findOpenCompareEditor(IWorkbenchPartSite site, FileFromChangeSet fcs, ISynchronizePageConfiguration configuration2) {
		IWorkbenchPage page = site.getPage();
		IEditorReference[] editorRefs = page.getEditorReferences();
		for (int i = 0; i < editorRefs.length; i++) {
			final IEditorPart part = editorRefs[i].getEditor(false /* don't restore editor */);
			if (part != null) {
				IEditorInput input = part.getEditorInput();
				if (input instanceof ModelCompareEditorInput) {
					ModelCompareEditorInput mInput = (ModelCompareEditorInput) input;
					Object obj = mInput.getCompareResult();
					if (obj instanceof MercurialCompareInput) {
						MercurialCompareInput mci = (MercurialCompareInput) obj;
						if (mci.isInputFor(fcs)) {
							return part;
						}
					}
				}
			}
		}
		return null;
	}

	private boolean isOkToRun(ISelection selection) {
		// do not open Compare Editor unless all elements have input
		Object[] elements = ((IStructuredSelection) selection).toArray();
		ISynchronizeParticipant participant = configuration
				.getParticipant();
		// model synchronize
		if (participant instanceof ModelSynchronizeParticipant) {
			ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
			for (int i = 0; i < elements.length; i++) {
				// TODO: This is inefficient
				if (!msp.hasCompareInputFor(elements[i])) {
					return false;
				}
			}
		} else {
			// all files
			IResource resources[] = Utils.getResources(elements);
			for (int i = 0; i < resources.length; i++) {
	            if (resources[i].getType() != IResource.FILE) {
	                // Only supported if all the items are files.
	                return false;
	            }
	        }
		}
		return true;
	}
}
