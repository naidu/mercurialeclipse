/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov          - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetContentProvider;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetModelProvider;
import com.vectrace.MercurialEclipse.team.CompareAction;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * @author Andrei
 */
public class OpenAction extends Action {

	private final Action defaultOpen;
	private final ISynchronizePageConfiguration configuration;

	public OpenAction(Action defaultOpen, ISynchronizePageConfiguration configuration) {
		super();
		this.defaultOpen = defaultOpen;
		this.configuration = configuration;
	}

	@Override
	public void run() {
		Object input = configuration.getPage().getViewer().getInput();
		if(!(input instanceof HgChangeSetModelProvider)) {
			// this is the "default" workspace resources mode
			if(defaultOpen != null){
				defaultOpen.run();
			}
			return;
		}
		ISelection selection = configuration.getSite().getSelectionProvider().getSelection();
		if(!(selection instanceof IStructuredSelection)){
			return;
		}
		IStructuredSelection selection2 = (IStructuredSelection) selection;
		if(selection2.size() != 1){
			return;
		}
		Object object = selection2.getFirstElement();
		if(!(object instanceof FileFromChangeSet)){
			if(object instanceof WorkingChangeSet) {
				tryToEditChangeset(selection2);
			}
			return;
		}
		FileFromChangeSet fcs = (FileFromChangeSet) object;
		Viewer viewer = configuration.getPage().getViewer();
		if(!(viewer instanceof ContentViewer)){
			return;
		}
		CommonViewer commonViewer = (CommonViewer) viewer;
		final HgChangeSetContentProvider csProvider = getProvider(commonViewer.getNavigatorContentService());
		final ChangeSet cs = csProvider.getParentOfSelection(fcs);
		if(cs == null){
			return;
		}

		final IFile file = fcs.getFile();
		if(file == null){
			// TODO this can happen, if the file was modified but is OUTSIDE Eclipse workspace
			MessageDialog.openInformation(null, "Compare",
					"Diff for files external to Eclipse workspace is not supported yet!");
			return;
		}
		IFile sourceFile = fcs.getCopySourceFile();
		final IFile parentFile = sourceFile != null? sourceFile : file;

		if(cs instanceof WorkingChangeSet){
			// default: compare local file against parent changeset
			CompareAction compareAction = new CompareAction(file);
			compareAction.setUncommittedCompare(true);
			compareAction.setSynchronizePageConfiguration(configuration);
			compareAction.run(this);
			return;
		}

		Job job = new Job("Diff for " + file.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					CompareUtils.openCompareWithParentEditor(cs, file, false, configuration);
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
					return e.getStatus();
				}

				return Status.OK_STATUS;
			}

		};
		job.schedule();
	}

	protected void tryToEditChangeset(IStructuredSelection selection) {
		Object property = configuration.getProperty(MercurialSynchronizePageActionGroup.EDIT_CHANGESET_ACTION);
		if(property instanceof EditChangesetSynchronizeAction) {
			EditChangesetSynchronizeAction editAction = (EditChangesetSynchronizeAction) property;
			editAction.selectionChanged(selection);
			if(editAction.isEnabled()) {
				editAction.run();
			}
		}
	}

	public static HgChangeSetContentProvider getProvider(INavigatorContentService service) {
		INavigatorContentExtension extensionById = service
				.getContentExtensionById(HgChangeSetContentProvider.ID);
		IContentProvider provider = extensionById.getContentProvider();
		if (provider instanceof HgChangeSetContentProvider) {
			return (HgChangeSetContentProvider) provider;
		}
		return null;
	}

}
