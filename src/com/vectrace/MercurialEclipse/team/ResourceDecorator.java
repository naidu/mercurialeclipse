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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author zingo
 * 
 */
public class ResourceDecorator extends LabelProvider implements
        ILightweightLabelDecorator, Observer
// FlagManagerListener
{
    private static MercurialStatusCache statusCache = MercurialStatusCache
            .getInstance();
    // private FlagManager flagManager =
    // MercurialEclipsePlugin.getDefault().getFlagManager();

    // set to true when having 2 different statuses in a folder flags it has
    // modified
    private static boolean folder_logic_2MM;

    public ResourceDecorator() {
        configureFromPreferences();
        statusCache.addObserver(this);
        // flagManager.addListener(this);
    }

    @Override
    public void dispose() {
        // flagManager.removeListener(this);
        statusCache.deleteObserver(this);
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

            if (!statusCache.isStatusKnown((project))) {
                return;
            }                                     

            ImageDescriptor overlay = null;
            String prefix = null;
            BitSet output = statusCache.getStatus(resource);
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
            
            // get recent project versions
            LocalChangesetCache.getInstance().getLocalChangeSets(project);
            
            // label info for incoming changesets
            ChangeSet cs = null;
            try {
                cs = IncomingChangesetCache.getInstance()
                        .getNewestIncomingChangeSet(resource);
            } catch (HgException e1) {
                MercurialEclipsePlugin.logError(e1);
            }

            if (cs != null) {
                if (prefix == null) {
                    prefix = "<";
                } else {
                    prefix = "<" + prefix;
                }
            }

            if (prefix != null) {
                decoration.addPrefix(prefix);
            }

            // local changeset info
            try {
                ChangeSet changeSet = LocalChangesetCache.getInstance()
                        .getNewestLocalChangeSet(resource);

                if (changeSet != null) {
                    String hex = ":" + changeSet.getNodeShort();
                    String suffix = " [" + changeSet.getChangesetIndex() + hex
                            + "]";

                    if (resource.getType() == IResource.FILE) {
                        suffix = " [" + changeSet.getChangesetIndex() + "] ";

                        if (cs != null) {
                            suffix += "< [" + cs.getChangesetIndex() + ":"
                                    + cs.getNodeShort() + " " + cs.getUser()
                                    + "]";
                        }
                    }

                    if (resource.getType() != IResource.FOLDER) {
                        decoration.addSuffix(suffix);
                    }
                }

            } catch (HgException e) {
                MercurialEclipsePlugin.logWarning(
                        "Couldn't get version of resource " + resource, e);
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

    public void update(Observable o, Object updatedObject) {
        if (o == statusCache) {
            final IWorkbench workbench = PlatformUI.getWorkbench();
            final String decoratorId = ResourceDecorator.class.getName();
            new SafeUiJob("Update Decorations") {
                @Override
                protected IStatus runSafe(IProgressMonitor monitor) {
                    workbench.getDecoratorManager().update(decoratorId);
                    return super.runSafe(monitor);
                }
            }.schedule();
        }
    }

}
