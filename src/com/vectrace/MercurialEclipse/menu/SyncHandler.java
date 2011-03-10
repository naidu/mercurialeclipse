/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.MercurialParticipantSynchronizeWizard;

public class SyncHandler extends MultipleResourcesHandler {

	@Override
	protected void run(List<IResource> resources) throws Exception {
		MercurialParticipantSynchronizeWizard wizard = new MercurialParticipantSynchronizeWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(resources));
		if(shouldShowWizard(wizard)) {
			WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
			wizardDialog.open();
		} else {
			wizard.performFinish();
		}
	}

	private boolean shouldShowWizard(MercurialParticipantSynchronizeWizard wizard){
//		if(wizard.prepareSettings() == null){
//			return true;
//		}
		ExecutionEvent executionEvent = getEvent();
		String id = executionEvent.getCommand().getId();
		return "com.vectrace.MercurialEclipse.menu.SyncHandler2".equals(id);
	}

	@Override
	protected List<IResource> getSelectedResources() {
		List<IResource> resources = super.getSelectedResources();
		if(MercurialEclipsePlugin.getDefault().getPreferenceStore().getBoolean(MercurialPreferenceConstants.PREF_SYNC_ALL_PROJECTS_IN_REPO)) {
			Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(resources);
			resources.clear();
			Set<HgRoot> roots = byRoot.keySet();
			for (HgRoot root : roots) {
				Set<IProject> projects = ResourceUtils.getProjects(root);
				resources.addAll(projects);
			}
		}
		return resources;
	}
}
