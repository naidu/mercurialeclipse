/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.SubscriberParticipantWizard;

import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;

/**
 * @author bastian
 * 
 */
public class MercurialParticipantSynchronizeWizard extends
        SubscriberParticipantWizard implements IWizard {
    private IWizard importWizard = new CloneRepoWizard();
    private SynchronizeWizardPage page;
    

    @Override
    protected SubscriberParticipant createParticipant(ISynchronizeScope scope) {
        return new MercurialSynchronizeParticipant(scope, page.getLocation());
    }

    @Override
    protected IWizard getImportWizard() {
        return importWizard;
    }

    @Override
    protected String getPageTitle() {
        return "Mercurial Synchronization Wizard";
    }

    @Override
    protected IResource[] getRootResources() {
        return ResourcesPlugin.getWorkspace().getRoot().getProjects();
    }

    @Override
    public void addPages() {
        super.addPages();
        ImageDescriptor image = ImageDescriptor.createFromFile(null,
                "icons/newconnect_wizban.gif");
        IProject[] projects = MercurialStatusCache.getInstance()
                .getAllManagedProjects();
        if (projects != null) {
            page = new SynchronizeWizardPage(
                    "SyncRepoSelection", "Repository Selection", image);
            page.setDescription("Please select the repository to use for synchronization.");
            page.setTitle(getPageTitle().concat(" - Repository Selection"));
            addPage(page);
        }

    }
    
    @Override
    public boolean performFinish() {        
        return super.performFinish();
    }

}
