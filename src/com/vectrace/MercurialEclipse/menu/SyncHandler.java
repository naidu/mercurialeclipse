/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

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
		if(!wizard.isComplete()){
			return true;
		}
		ExecutionEvent executionEvent = getEvent();
		String id = executionEvent.getCommand().getId();
		if(id != null && id.equals("com.vectrace.MercurialEclipse.menu.SyncHandler2")){
			return true;
		}
		return false;
	}
}
