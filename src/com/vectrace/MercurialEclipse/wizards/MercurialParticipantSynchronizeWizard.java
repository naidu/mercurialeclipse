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

import java.net.URISyntaxException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelParticipantWizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author bastian
 * 
 */
public class MercurialParticipantSynchronizeWizard extends
        ModelParticipantWizard implements IWizard {
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
    protected IWizard getImportWizard() {
        return importWizard;
    }

    @Override
    protected String getPageTitle() {
        return Messages
                .getString("MercurialParticipantSynchronizeWizard.pageTitle"); //$NON-NLS-1$
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
            ConfigurationWizardMainPage mainPage = new ConfigurationWizardMainPage(
                    Messages
                            .getString("MercurialParticipantSynchronizeWizard.repositoryPage.name"), //$NON-NLS-1$
                    Messages
                            .getString("MercurialParticipantSynchronizeWizard.repositoryPage.title"), MercurialEclipsePlugin //$NON-NLS-1$
                            .getImageDescriptor(Messages
                                    .getString("MercurialParticipantSynchronizeWizard.repositoryPage.image"))); //$NON-NLS-1$

            mainPage.setShowBundleButton(false);
            mainPage.setShowCredentials(true);
            page = mainPage;
            page
                    .setDescription(Messages
                            .getString("MercurialParticipantSynchronizeWizard.repositoryPage.description")); //$NON-NLS-1$
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.ui.synchronize.ModelParticipantWizard#createParticipant
     * (org.eclipse.core.resources.mapping.ResourceMapping[])
     */
    @Override
    protected ISynchronizeParticipant createParticipant(
            ResourceMapping[] selectedMappings) {
        String url = pageProperties.getProperty("url"); //$NON-NLS-1$
        String user = pageProperties.getProperty("user"); //$NON-NLS-1$
        String pass = pageProperties.getProperty("password"); //$NON-NLS-1$
        HgRepositoryLocation repo;
        try {            
            repo = MercurialEclipsePlugin.getRepoManager().getRepoLocation(url,
                    user, pass);
            ISynchronizationScope scope = null; // TODO

            MercurialSynchronizeSubscriber subscriber = new MercurialSynchronizeSubscriber(
                    scope, repo);

            SubscriberScopeManager manager = new SubscriberScopeManager(
                    "HgSubscriberScopeManager", selectedMappings, subscriber,
                    false);

            MergeContext ctx = new HgSubscriberMergeContext(subscriber, manager);           
            
            return new MercurialSynchronizeParticipant(ctx, repo);
        } catch (URISyntaxException e) {
            MercurialEclipsePlugin.logError(e);
            page.setErrorMessage(e.getLocalizedMessage());
        }
        return null;
    }

}
