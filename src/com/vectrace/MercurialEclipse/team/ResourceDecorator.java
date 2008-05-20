/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.BitSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;

/**
 * @author zingo
 * 
 */
public class ResourceDecorator extends LabelProvider implements
        ILightweightLabelDecorator, Observer
// FlagManagerListener
{

    private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache
            .getInstance();
    private static final IncomingChangesetCache INCOMING_CACHE = IncomingChangesetCache
            .getInstance();
    private static final LocalChangesetCache LOCAL_CACHE = LocalChangesetCache
            .getInstance();

    // set to true when having 2 different statuses in a folder flags it has
    // modified
    private static boolean folder_logic_2MM;

    public ResourceDecorator() {
        configureFromPreferences();
        STATUS_CACHE.addObserver(this);
        LOCAL_CACHE.addObserver(this);
        INCOMING_CACHE.addObserver(this);
    }

    @Override
    public void dispose() {
        STATUS_CACHE.deleteObserver(this);
        STATUS_CACHE.clear();
        INCOMING_CACHE.deleteObserver(this);
        INCOMING_CACHE.clear();
        LOCAL_CACHE.deleteObserver(this);
        LOCAL_CACHE.clear();
        super.dispose();
    }

    private static void configureFromPreferences() {
        IPreferenceStore store = MercurialEclipsePlugin.getDefault()
                .getPreferenceStore();
        folder_logic_2MM = MercurialPreferenceConstants.LABELDECORATOR_LOGIC_2MM
                .equals(store
                        .getString(MercurialPreferenceConstants.LABELDECORATOR_LOGIC));
    }

    public void decorate(Object element, IDecoration decoration) {
        try {
            IResource resource = (IResource) element;
            IProject project = resource.getProject();

            if (null == RepositoryProvider.getProvider(project,
                    MercurialTeamProvider.ID)) {
                return;
            }

            if (!MercurialUtilities.isResourceInReposetory(resource, true)) {
                // Resource could be inside a link or something do nothing
                // in the future this could check is this is another repository
                return;
            }

            boolean showChangeset = Boolean
                    .valueOf(
                            MercurialUtilities
                                    .getPreference(
                                            MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET,
                                            "false")).booleanValue();
            if (showChangeset) {
                // get recent project versions
                if (!STATUS_CACHE.getLock(project).isLocked()
                        && !STATUS_CACHE.getLock(resource).isLocked()
                        && !STATUS_CACHE.isStatusKnown(project)
                        && !LOCAL_CACHE.isLocalUpdateInProgress(project)
                        && !LOCAL_CACHE.isLocalUpdateInProgress(resource)
                        && !LOCAL_CACHE.isLocallyKnown(resource.getProject())) {
                    // LOCAL_CACHE notifies resource decorator when it's
                    // finished.
                    RefreshJob job = new RefreshJob(
                            "Refreshing changeset decoration", null, resource
                                    .getProject(), showChangeset);
                    job.schedule();
                    job.join();
                    return;
                }
            } else {
                if (!STATUS_CACHE.getLock(project).isLocked()
                        && !STATUS_CACHE.getLock(resource).isLocked()
                        && !STATUS_CACHE.isStatusKnown(project)) {
                    RefreshStatusJob job = new RefreshStatusJob(
                            "Updating status for project " + project.getName()
                                    + " on behalf of resource "
                                    + resource.getName(), project);
                    job.schedule();
                    job.join();
                    return;
                }
            }

            ImageDescriptor overlay = null;
            String prefix = null;
            BitSet output = STATUS_CACHE.getStatus(resource);
            if (output != null) {
                // BitSet output = fr.getStatus();
                // "ignore" does not really count as modified
                if (folder_logic_2MM
                        && (output.cardinality() > 2 || (output.cardinality() == 2 && !output
                                .get(MercurialStatusCache.BIT_IGNORE)))) {
                    overlay = DecoratorImages.modifiedDescriptor;
                    prefix = ">";
                } else {
                    switch (output.length() - 1) {
                    case MercurialStatusCache.BIT_MODIFIED:
                        overlay = DecoratorImages.modifiedDescriptor;
                        prefix = ">";
                        break;
                    case MercurialStatusCache.BIT_ADDED:
                        overlay = DecoratorImages.addedDescriptor;
                        prefix = ">";
                        break;
                    case MercurialStatusCache.BIT_UNKNOWN:
                        overlay = DecoratorImages.notTrackedDescriptor;
                        prefix = ">";
                        break;
                    case MercurialStatusCache.BIT_CLEAN:
                        overlay = DecoratorImages.managedDescriptor;
                        break;
                    // case BIT_IGNORE:
                    // do nothing
                    case MercurialStatusCache.BIT_REMOVED:
                        overlay = DecoratorImages.removedDescriptor;
                        prefix = ">";
                        break;
                    case MercurialStatusCache.BIT_DELETED:
                        overlay = DecoratorImages.deletedStillTrackedDescriptor;
                        prefix = ">";
                        break;
                    }
                }
            } else {
                // empty folder, do nothing
            }
            if (overlay != null) {
                decoration.addOverlay(overlay);
            }

            // we want a prefix, even if no changeset is displayed
            if (prefix != null) {
                decoration.addPrefix(prefix);
            }

            if (showChangeset) {

                // label info for incoming changesets
                ChangeSet cs = null;
                try {
                    cs = INCOMING_CACHE.getNewestIncomingChangeSet(resource);
                } catch (HgException e1) {
                    MercurialEclipsePlugin.logError(e1);
                }

                if (cs != null) {
                    if (prefix == null) {
                        prefix = "<";
                    } else {
                        prefix = "<" + prefix;
                    }
                    // decorate new prefix
                    decoration.addPrefix(prefix);
                }

                // local changeset info
                try {
                    ChangeSet changeSet = null;
                    // the order is important here as well :-).
                    if (!LOCAL_CACHE.isLocalUpdateInProgress(project)
                            && LOCAL_CACHE.isLocallyKnown(project)) {
                        changeSet = LOCAL_CACHE
                                .getNewestLocalChangeSet(project);
                    }

                    if (changeSet != null) {
                        String hex = ":" + changeSet.getNodeShort();
                        // suffix for project
                        String suffix = " [" + changeSet.getChangesetIndex()
                                + hex + "]";

                        // suffix for files
                        if (!LOCAL_CACHE.isLocalUpdateInProgress(project)
                                && !LOCAL_CACHE
                                        .isLocalUpdateInProgress(resource)
                                && resource.getType() == IResource.FILE) {
                            changeSet = LOCAL_CACHE
                                    .getNewestLocalChangeSet(resource);
                            if (changeSet != null) {
                                suffix = " [" + changeSet.getChangesetIndex()
                                        + "] ";

                                if (cs != null) {
                                    suffix += "< [" + cs.getChangesetIndex()
                                            + ":" + cs.getNodeShort() + " "
                                            + cs.getUser() + "]";
                                }
                            }
                        }

                        // only decorate files and project with suffix
                        if (resource.getType() != IResource.FOLDER) {
                            decoration.addSuffix(suffix);
                        }
                    }

                } catch (HgException e) {
                    MercurialEclipsePlugin.logWarning(
                            "Couldn't get version of resource " + resource, e);
                }
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    /**
     * Called when the configuration of the decorator changes
     */
    public static void onConfigurationChanged() {
        String decoratorId = ResourceDecorator.class.getName();
        configureFromPreferences();
        PlatformUI.getWorkbench().getDecoratorManager().update(decoratorId);
    }

    @SuppressWarnings("unchecked")
    public void update(Observable o, Object updatedObject) {
        // final IWorkbench workbench = PlatformUI.getWorkbench();
        // final String decoratorId = ResourceDecorator.class.getName();
        // new SafeUiJob("Update Decorations") {
        // @Override
        // protected IStatus runSafe(IProgressMonitor monitor) {
        // // FIXME: fire events for the changed resources instead!
        // workbench.getDecoratorManager().update(decoratorId);
        // return super.runSafe(monitor);
        // }
        // }.schedule();
        if (updatedObject instanceof Set) {
            Set changed = (Set) updatedObject;
            LabelProviderChangedEvent event = new LabelProviderChangedEvent(
                    this, changed.toArray());
            fireLabelProviderChanged(event);
        }
    }

}
