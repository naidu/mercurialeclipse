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
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author bastian
 * 
 */
public class MercurialParticipantSynchronizeWizard extends
        SubscriberParticipantWizard implements IWizard {
    private final IWizard importWizard = new CloneRepoWizard();
    private HgWizardPage page;
    private Properties pageProperties = null;

    public MercurialParticipantSynchronizeWizard() {
        IDialogSettings workbenchSettings = MercurialEclipsePlugin.getDefault()
                .getDialogSettings();
        IDialogSettings section = workbenchSettings
                .getSection("MercurialParticipantSynchronizeWizard"); //$NON-NLS-1$
        if (section == null) {
            section = workbenchSettings
                    .addNewSection("MercurialParticipantSynchronizeWizard"); //$NON-NLS-1$
        }
        setDialogSettings(section);
    }

    @Override
    protected SubscriberParticipant createParticipant(ISynchronizeScope scope) {
        return new MercurialSynchronizeParticipant(scope, pageProperties
                .getProperty("url")); //$NON-NLS-1$
    }

    @Override
    protected IWizard getImportWizard() {
        return importWizard;
    }

    @Override
    protected String getPageTitle() {
        return Messages.getString("MercurialParticipantSynchronizeWizard.pageTitle"); //$NON-NLS-1$
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
            page = new ConfigurationWizardMainPage(Messages.getString("MercurialParticipantSynchronizeWizard.repositoryPage.name"), //$NON-NLS-1$
                    Messages.getString("MercurialParticipantSynchronizeWizard.repositoryPage.title"), MercurialEclipsePlugin //$NON-NLS-1$
                            .getImageDescriptor(Messages.getString("MercurialParticipantSynchronizeWizard.repositoryPage.image"))); //$NON-NLS-1$

            page
                    .setDescription(Messages.getString("MercurialParticipantSynchronizeWizard.repositoryPage.description")); //$NON-NLS-1$
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
