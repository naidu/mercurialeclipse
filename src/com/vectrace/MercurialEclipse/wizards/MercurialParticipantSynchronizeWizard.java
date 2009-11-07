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

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.team.ui.synchronize.ModelParticipantWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberScopeManager;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeSubscriber;
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author bastian
 *
 */
public class MercurialParticipantSynchronizeWizard extends ModelParticipantWizard implements IWorkbenchWizard {

    private static final String SECTION_NAME = "MercurialParticipantSynchronizeWizard";
    private static final String PROP_PASSWORD = "password";
    private static final String PROP_USER = "user";
    private static final String PROP_URL = "url";

    private final IWizard importWizard = new CloneRepoWizard();
    private HgWizardPage page;
    private Properties pageProperties;
    private IResource [] projects;

    public MercurialParticipantSynchronizeWizard() {
        IDialogSettings workbenchSettings = MercurialEclipsePlugin.getDefault()
        .getDialogSettings();
        IDialogSettings section = workbenchSettings.getSection(SECTION_NAME);
        if (section == null) {
            section = workbenchSettings.addNewSection(SECTION_NAME);
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

    /**
     * @return a list of selected managed projects, or all managed projects, if there was
     * no selection. Never returns null, but can return empty list
     * {@inheritDoc}
     */
    @Override
    protected IResource[] getRootResources() {
        return projects != null ? projects : MercurialStatusCache.getInstance()
                .getAllManagedProjects();
    }

    @Override
    public void addPages() {
        IResource[] rootResources = getRootResources();
        if (rootResources.length == 0) {
            return;
        }
        pageProperties = initProperties(rootResources);
        // creates selection page
        super.addPages();
        page = createrepositoryConfigPage();
        addPage(page);
    }

    private ConfigurationWizardMainPage createrepositoryConfigPage() {
        ConfigurationWizardMainPage mainPage = new ConfigurationWizardMainPage(
                Messages
                .getString("MercurialParticipantSynchronizeWizard.repositoryPage.name"), //$NON-NLS-1$
                Messages
                .getString("MercurialParticipantSynchronizeWizard.repositoryPage.title"), MercurialEclipsePlugin //$NON-NLS-1$
                .getImageDescriptor(Messages
                        .getString("MercurialParticipantSynchronizeWizard.repositoryPage.image"))); //$NON-NLS-1$

        mainPage.setShowBundleButton(false);
        mainPage.setShowCredentials(true);
        mainPage
        .setDescription(Messages
                .getString("MercurialParticipantSynchronizeWizard.repositoryPage.description")); //$NON-NLS-1$
        mainPage.setDialogSettings(getDialogSettings());
        mainPage.setProperties(pageProperties);
        return mainPage;
    }

    /**
     * @return true if all information needed to synchronize is available
     */
    public boolean isComplete() {
        IResource[] resources = getRootResources();
        // TODO currently it accepts only one selected project
        if(projects == resources && projects.length == 1){
            pageProperties = initProperties(resources);
            if(isValid(PROP_URL)) {
                if (isValid(PROP_USER)) {
                    if (isValid(PROP_PASSWORD)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isValid(String key){
        String value = pageProperties.getProperty(key);
        return value != null && value.trim().length() > 0;
    }

    /**
     * @param rootResources non null, non empty array with selected managed resources
     * @return non null proeprties with possible repository data initialized from given
     * resources (may be empty)
     */
    public static Properties initProperties(IResource[] rootResources) {
        Properties properties = new Properties();
        if(rootResources.length == 1){
            HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
            HgRepositoryLocation repoLocation = repoManager.getDefaultProjectRepoLocation((IProject) rootResources[0]);
            if(repoLocation != null){
                if(repoLocation.getLocation() != null) {
                    properties.setProperty(PROP_URL, repoLocation.getLocation());
                    if(repoLocation.getUser() != null) {
                        properties.setProperty(PROP_USER, repoLocation.getUser());
                        if(repoLocation.getPassword() != null) {
                            properties.setProperty(PROP_PASSWORD, repoLocation.getPassword());
                        }
                    }
                }
            }
        }
        return properties;
    }

    @Override
    public boolean performFinish() {
        if(page == null){
            // UI was not created, so we just need to continue with synchronization
            createParticipant2();
            return true;
        }
        page.finish(new NullProgressMonitor());
        this.pageProperties = page.getProperties();
        return super.performFinish();
    }

    @SuppressWarnings("restriction")
    protected final void createParticipant2() {
        ISynchronizeParticipant participant = createParticipant(Utils.getResourceMappings(projects));
        TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[]{participant});
        // We don't know in which site to show progress because a participant could actually be shown in multiple sites.
        participant.run(null /* no site */);
    }

    @Override
    protected ISynchronizeParticipant createParticipant(ResourceMapping[] selectedMappings) {

        String url = pageProperties.getProperty(PROP_URL);
        String user = pageProperties.getProperty(PROP_USER);
        String pass = pageProperties.getProperty(PROP_PASSWORD);
        HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();

        // TODO implementation is incomplete
        Set<IProject> selectedProjects = new HashSet<IProject>();
        for (ResourceMapping mapping : selectedMappings) {
            IProject[] projects2 = mapping.getProjects();
            for (IProject iProject : projects2) {
                selectedProjects.add(iProject);
            }
        }
        if (selectedProjects.isEmpty()) {
            return null;
        }

        HgRepositoryLocation repo;
        try {
            repo = repoManager.getRepoLocation(url, user, pass);
            if(pass != null && user != null){
                if(!pass.equals(repo.getPassword())){
                    // At least 1 project exists, update location for that project
                    repo = repoManager.updateRepoLocation(selectedProjects.iterator().next(), url, null, user, pass);
                }
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            page.setErrorMessage(e.getLocalizedMessage());
            return null;
        }

        // this is a hack to get only selected entries. I can't find the setting which
        // says to the selection page to provide really only things which are *selected*
        // so if it reports that user has selected MORE then we initially given to it,
        // then it's just kidding...
        IResource[] array = getRootResources();
        if(array.length > selectedProjects.size()){
            array = selectedProjects.toArray(new IResource[0]);
        }

        Set<IProject> repoProjects = repoManager.getAllRepoLocationProjects(repo);
        for (IResource resource : array) {
            IProject project = resource.getProject();
            if(repoProjects.contains(project)){
                continue;
            }
            try {
                repoManager.addRepoLocation(project, repo);
                // TODO is it required?
                HgRepositoryLocation repoLocation = repoManager.getDefaultProjectRepoLocation(project);
                if(repoLocation == null){
                    repoManager.setDefaultProjectRepository(project, repo);
                }
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }

        ISynchronizeParticipantReference participant = TeamUI.getSynchronizeManager().get(
                MercurialSynchronizeParticipant.class.getName(), repo.getLocation());

        // do not reuse participants which may already existing, but dispose them
        // not doing this would lead to the state where many sync. participants would listen
        // to resource changes and update/request same data/cashes many times
        // we can not reuse participants because their scope can be different (if there are
        // more then one project under same repository)
        if(participant != null){
            try {
                ISynchronizeParticipant participant2 = participant.getParticipant();
                TeamUI.getSynchronizeManager().removeSynchronizeParticipants(new ISynchronizeParticipant[]{participant2});
                while(Display.getCurrent().readAndDispatch()){
                    // give Team UI a chance to dispose the sync page, if any
                }
            } catch (TeamException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }

        // Create a new participant for given repo/project pair
        RepositorySynchronizationScope scope = new RepositorySynchronizationScope(repo, array);
        MercurialSynchronizeSubscriber subscriber = new MercurialSynchronizeSubscriber(scope);
        HgSubscriberScopeManager manager = new HgSubscriberScopeManager(selectedMappings, subscriber);
        MergeContext ctx = new HgSubscriberMergeContext(subscriber, manager);
        return new MercurialSynchronizeParticipant(ctx, repo);
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        if(selection != null && !selection.isEmpty()){
            Object[] array = selection.toArray();
            Set<IResource> roots = new HashSet<IResource>();
            IProject[] managed = MercurialStatusCache.getInstance().getAllManagedProjects();
            for (Object object : array) {
                if(object instanceof IResource){
                    IResource resource = (IResource) object;
                    for (IProject project : managed) {
                        if(project.contains(resource)) {
                            // add project as a root of resource
                            roots.add(project);
                        }
                    }
                }
            }
            projects = roots.toArray(new IResource[roots.size()]);
        }
    }

}
