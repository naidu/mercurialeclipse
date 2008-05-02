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

import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.SubscriberParticipantWizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;

/**
 * @author bastian
 * 
 */
public class MercurialParticipantSynchronizeWizard extends
        SubscriberParticipantWizard implements IWizard {
    private final IWizard importWizard = new CloneRepoWizard();
    private ConfigurationWizardMainPage page;
    private Properties pageProperties = null;

    public MercurialParticipantSynchronizeWizard() {
        IDialogSettings workbenchSettings = MercurialEclipsePlugin.getDefault()
                .getDialogSettings();
        IDialogSettings section = workbenchSettings
                .getSection("MercurialParticipantSynchronizeWizard");
        if (section == null) {
            section = workbenchSettings
                    .addNewSection("MercurialParticipantSynchronizeWizard");
        }
        setDialogSettings(section);
    }

    @Override
    protected SubscriberParticipant createParticipant(ISynchronizeScope scope) {
        return new MercurialSynchronizeParticipant(scope, pageProperties
                .getProperty("url"));
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
        IProject[] projects = MercurialStatusCache.getInstance()
                .getAllManagedProjects();
        if (projects != null) {
            page = new ConfigurationWizardMainPage("repositoryPage1",
                    "Choose repository", MercurialEclipsePlugin
                            .getImageDescriptor("wizards/share_wizban.png"));

            page
                    .setDescription("Please choose the repository location to monitor.");
            page.setDialogSettings(getDialogSettings());
            addPage(page);
        }

    }

    @Override
    public boolean performFinish() {
        page.finish(new NullProgressMonitor());
        this.pageProperties = page.getProperties();
        return super.performFinish();
    }

}
