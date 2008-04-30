/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.Properties;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

/**
 * Wizard to add a new location. Uses ConfigurationWizardMainPage for entering
 * informations about SVN repository location
 */
public class NewLocationWizard extends Wizard {
    private ConfigurationWizardMainPage mainPage;

    private Properties properties = null;

    public NewLocationWizard() {
        IDialogSettings workbenchSettings = MercurialEclipsePlugin.getDefault()
                .getDialogSettings();
        IDialogSettings section = workbenchSettings
                .getSection("NewLocationWizard");
        if (section == null) {
            section = workbenchSettings.addNewSection("NewLocationWizard");
        }
        setDialogSettings(section);
        setWindowTitle("Create new repository location");
    }

    public NewLocationWizard(Properties initialProperties) {
        this();
        this.properties = initialProperties;
    }

    /**
     * Creates the wizard pages
     */
    @Override
    public void addPages() {
        mainPage = new ConfigurationWizardMainPage("repositoryPage1",
                "Create new repository", MercurialEclipsePlugin
                        .getImageDescriptor("wizards/share_wizban.png"));
        if (properties != null) {
            mainPage.setProperties(properties);
        }
        mainPage.setDescription("Here you can create a new repository location.");
        mainPage.setDialogSettings(getDialogSettings());
        addPage(mainPage);
    }

    /*
     * @see IWizard#performFinish
     */
    @Override
    public boolean performFinish() {
        mainPage.finish(new NullProgressMonitor());
        Properties props = mainPage.getProperties();
        final HgRepositoryLocation[] root = new HgRepositoryLocation[1];
        HgRepositoryLocationManager provider = MercurialEclipsePlugin.getRepoManager();
        try {
            root[0] = provider.createRepository(props);
            provider.addRepoLocation(root[0]);
            return true;
        } catch (TeamException e) {
            MercurialEclipsePlugin.logError(e);
        }
        return false;
    }
}
