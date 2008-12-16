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

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.HgDecoratorConstants;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.AbstractCache;
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

    private static String[] fonts = new String[] {
            HgDecoratorConstants.ADDED_FONT,
            HgDecoratorConstants.CONFLICT_FONT,
            HgDecoratorConstants.DELETED_FONT,
            HgDecoratorConstants.REMOVED_FONT,
            HgDecoratorConstants.UNKNOWN_FONT,
            HgDecoratorConstants.IGNORED_FONT, HgDecoratorConstants.CHANGE_FONT };

    private static String[] colors = new String[] {
            HgDecoratorConstants.ADDED_BACKGROUND_COLOR,
            HgDecoratorConstants.ADDED_FOREGROUND_COLOR,
            HgDecoratorConstants.CHANGE_BACKGROUND_COLOR,
            HgDecoratorConstants.CHANGE_FOREGROUND_COLOR,
            HgDecoratorConstants.CONFLICT_BACKGROUND_COLOR,
            HgDecoratorConstants.CONFLICT_FOREGROUND_COLOR,
            HgDecoratorConstants.IGNORED_BACKGROUND_COLOR,
            HgDecoratorConstants.IGNORED_FOREGROUND_COLOR,
            HgDecoratorConstants.DELETED_BACKGROUND_COLOR,
            HgDecoratorConstants.DELETED_FOREGROUND_COLOR,
            HgDecoratorConstants.REMOVED_BACKGROUND_COLOR,
            HgDecoratorConstants.REMOVED_FOREGROUND_COLOR,
            HgDecoratorConstants.UNKNOWN_BACKGROUND_COLOR,
            HgDecoratorConstants.UNKNOWN_FOREGROUND_COLOR };

    // set to true when having 2 different statuses in a folder flags it has
    // modified
    private static boolean folder_logic_2MM;
    private ITheme theme;

    public ResourceDecorator() {
        configureFromPreferences();
        STATUS_CACHE.addObserver(this);
        LOCAL_CACHE.addObserver(this);
        INCOMING_CACHE.addObserver(this);
        theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        this.ensureFontAndColorsCreated(fonts, colors);
    }

    /**
     * This method will ensure that the fonts and colors used by the decorator
     * are cached in the registries. This avoids having to syncExec when
     * decorating since we ensure that the fonts and colors are pre-created.
     * 
     * @param f
     *            fonts ids to cache
     * @param c
     *            color ids to cache
     */
    private void ensureFontAndColorsCreated(final String[] f, final String[] c) {
        MercurialEclipsePlugin.getStandardDisplay().syncExec(new Runnable() {
            public void run() {
                for (int i = 0; i < c.length; i++) {
                    theme.getColorRegistry().get(c[i]);
                }
                for (int i = 0; i < f.length; i++) {
                    theme.getFontRegistry().get(f[i]);
                }
            }
        });
    }

    @Override
    public void dispose() {
        STATUS_CACHE.deleteObserver(this);
        STATUS_CACHE.clear();
        INCOMING_CACHE.deleteObserver(this);
        INCOMING_CACHE.clear();
        LOCAL_CACHE.deleteObserver(this);
        LOCAL_CACHE.clear();
        AbstractCache.clearNodeMap();
        super.dispose();
    }

    private static void configureFromPreferences() {
        IPreferenceStore store = MercurialEclipsePlugin.getDefault()
                .getPreferenceStore();
        folder_logic_2MM = MercurialPreferenceConstants.LABELDECORATOR_LOGIC_2MM
                .equals(store
                        .getString(MercurialPreferenceConstants.LABELDECORATOR_LOGIC));
    }

    public void decorate(Object element, IDecoration d) {
        try {
            IResource resource = (IResource) element;
            IProject project = resource.getProject();

            if (project == null
                    || RepositoryProvider.getProvider(project,
                            MercurialTeamProvider.ID) == null
                    || !project.isAccessible()) {
                return;
            }

            if (!MercurialUtilities.hgIsTeamProviderFor(resource, false)) {
                // Resource could be inside a link or something do nothing
                // in the future this could check is this is another repository
                return;
            }

            boolean coloriseLabels = isColorise();

            boolean showChangeset = isShowChangeset();
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
                            Messages
                                    .getString("ResourceDecorator.refreshingChangesetDeco"), null, resource //$NON-NLS-1$
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
                            Messages
                                    .getString("ResourceDecorator.updatingStatusForProject.1") + project.getName() //$NON-NLS-1$
                                    + Messages
                                            .getString("ResourceDecorator.updatingStatusForProject.2") //$NON-NLS-1$
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
                    prefix = ">"; //$NON-NLS-1$
                    if (coloriseLabels) {
                        setBackground(d,
                                HgDecoratorConstants.CHANGE_BACKGROUND_COLOR);
                        setForeground(d,
                                HgDecoratorConstants.CHANGE_FOREGROUND_COLOR);
                        setFont(d, HgDecoratorConstants.CHANGE_FONT);
                    }
                } else {
                    switch (output.length() - 1) {
                    case MercurialStatusCache.BIT_IGNORE:
                        if (coloriseLabels) {
                            setBackground(
                                    d,
                                    HgDecoratorConstants.IGNORED_BACKGROUND_COLOR);
                            setForeground(
                                    d,
                                    HgDecoratorConstants.IGNORED_FOREGROUND_COLOR);
                            setFont(d, HgDecoratorConstants.IGNORED_FONT);
                        }
                        break;
                    case MercurialStatusCache.BIT_MODIFIED:
                        overlay = DecoratorImages.modifiedDescriptor;
                        prefix = ">"; //$NON-NLS-1$
                        if (coloriseLabels) {
                            setBackground(
                                    d,
                                    HgDecoratorConstants.CHANGE_BACKGROUND_COLOR);
                            setForeground(
                                    d,
                                    HgDecoratorConstants.CHANGE_FOREGROUND_COLOR);
                            setFont(d, HgDecoratorConstants.CHANGE_FONT);
                        }
                        break;
                    case MercurialStatusCache.BIT_ADDED:
                        overlay = DecoratorImages.addedDescriptor;
                        prefix = ">"; //$NON-NLS-1$
                        if (coloriseLabels) {
                            setBackground(d,
                                    HgDecoratorConstants.ADDED_BACKGROUND_COLOR);
                            setForeground(d,
                                    HgDecoratorConstants.ADDED_FOREGROUND_COLOR);
                            setFont(d, HgDecoratorConstants.ADDED_FONT);
                        }
                        break;
                    case MercurialStatusCache.BIT_UNKNOWN:
                        overlay = DecoratorImages.notTrackedDescriptor;
                        prefix = ">"; //$NON-NLS-1$
                        if (coloriseLabels) {
                            setBackground(
                                    d,
                                    HgDecoratorConstants.UNKNOWN_BACKGROUND_COLOR);
                            setForeground(
                                    d,
                                    HgDecoratorConstants.UNKNOWN_FOREGROUND_COLOR);
                            setFont(d, HgDecoratorConstants.UNKNOWN_FONT);
                        }
                        break;
                    case MercurialStatusCache.BIT_CLEAN:
                        overlay = DecoratorImages.managedDescriptor;
                        break;
                    // case BIT_IGNORE:
                    // do nothing
                    case MercurialStatusCache.BIT_REMOVED:
                        overlay = DecoratorImages.removedDescriptor;
                        prefix = ">"; //$NON-NLS-1$
                        if (coloriseLabels) {
                            setBackground(
                                    d,
                                    HgDecoratorConstants.REMOVED_BACKGROUND_COLOR);
                            setForeground(
                                    d,
                                    HgDecoratorConstants.REMOVED_FOREGROUND_COLOR);
                            setFont(d, HgDecoratorConstants.REMOVED_FONT);
                        }
                        break;
                    case MercurialStatusCache.BIT_DELETED:
                        overlay = DecoratorImages.deletedStillTrackedDescriptor;
                        prefix = ">"; //$NON-NLS-1$
                        if (coloriseLabels) {
                            setBackground(
                                    d,
                                    HgDecoratorConstants.DELETED_BACKGROUND_COLOR);
                            setForeground(
                                    d,
                                    HgDecoratorConstants.DELETED_FOREGROUND_COLOR);
                            setFont(d, HgDecoratorConstants.DELETED_FONT);
                        }
                        break;
                    case MercurialStatusCache.BIT_CONFLICT:
                        overlay = DecoratorImages.conflictDescriptor;
                        prefix = ">"; //$NON-NLS-1$
                        if (coloriseLabels) {
                            setBackground(
                                    d,
                                    HgDecoratorConstants.CONFLICT_BACKGROUND_COLOR);
                            setForeground(
                                    d,
                                    HgDecoratorConstants.CONFLICT_FOREGROUND_COLOR);
                            setFont(d, HgDecoratorConstants.CONFLICT_FONT);
                        }
                        break;
                    }
                }
            } else {
                // empty folder, do nothing
            }
            if (overlay != null) {
                d.addOverlay(overlay);
            }

            if (showChangeset) {

                // label info for incoming changesets
                ChangeSet newestIncomingChangeSet = null;
                try {
                    newestIncomingChangeSet = INCOMING_CACHE
                            .getNewestIncomingChangeSet(resource);
                } catch (HgException e1) {
                    MercurialEclipsePlugin.logError(e1);
                }

                if (newestIncomingChangeSet != null) {
                    if (prefix == null) {
                        prefix = "<"; //$NON-NLS-1$
                    } else {
                        prefix = "<" + prefix; //$NON-NLS-1$
                    }
                }

                // local changeset info
                try {
                    // init suffix with project changeset information
                    String suffix = ""; //$NON-NLS-1$
                    if (resource.getType() == IResource.PROJECT) {
                        suffix = getSuffixForProject(project);
                    }

                    // overwrite suffix for files
                    if (resource.getType() == IResource.FILE) {
                        suffix = getSuffixForFiles(resource,
                                newestIncomingChangeSet);
                    }

                    // only decorate files and project with suffix
                    if (resource.getType() != IResource.FOLDER) {
                        d.addSuffix(suffix);
                    }

                } catch (HgException e) {
                    MercurialEclipsePlugin
                            .logWarning(
                                    Messages
                                            .getString("ResourceDecorator.couldntGetVersionOfResource") + resource, e); //$NON-NLS-1$
                }
            } else {
                if (resource.getType() == IResource.PROJECT) {
                    String suffix = getSuffixForProject(project);
                    d.addSuffix(suffix);
                }
            }

            // we want a prefix, even if no changeset is displayed
            if (prefix != null) {
                d.addPrefix(prefix);
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    private void setBackground(IDecoration d, String id) {
        d.setBackgroundColor(theme.getColorRegistry().get(id));
    }

    private void setForeground(IDecoration d, String id) {
        d.setForegroundColor(theme.getColorRegistry().get(id));
    }

    private void setFont(IDecoration d, String id) {
        d.setFont(theme.getFontRegistry().get(id));
    }

    /**
     * @return
     */
    private boolean isShowChangeset() {
        boolean showChangeset = Boolean
                .valueOf(
                        HgClients
                                .getPreference(
                                        MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET,
                                        "false")).booleanValue(); //$NON-NLS-1$
        return showChangeset;
    }

    private boolean isColorise() {
        boolean colorise = Boolean.valueOf(
                HgClients.getPreference(
                        MercurialPreferenceConstants.PREF_DECORATE_WITH_COLORS,
                        "false")).booleanValue(); //$NON-NLS-1$
        return colorise;
    }

    /**
     * @param resource
     * @param project
     * @param cs
     * @param suffix
     * @return
     * @throws HgException
     */
    private String getSuffixForFiles(IResource resource, ChangeSet cs)
            throws HgException {
        String suffix = ""; //$NON-NLS-1$
        // suffix for files
        if (!LOCAL_CACHE.isLocalUpdateInProgress(resource.getProject())
                && !STATUS_CACHE.isAdded(resource.getProject(), resource
                        .getLocation())
                && !LOCAL_CACHE.isLocalUpdateInProgress(resource)) {
            ChangeSet fileCs = LOCAL_CACHE.getNewestLocalChangeSet(resource);
            if (fileCs != null) {
                suffix = " [" + fileCs.getChangesetIndex() + " - " //$NON-NLS-1$ //$NON-NLS-2$
                        + fileCs.getAgeDate() + " - " + fileCs.getUser() + "]"; //$NON-NLS-1$ //$NON-NLS-2$

                if (cs != null) {
                    suffix += "< [" + cs.getChangesetIndex() + ":" //$NON-NLS-1$ //$NON-NLS-2$
                            + cs.getNodeShort() + " - " + cs.getAgeDate() //$NON-NLS-1$
                            + " - " + cs.getUser() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
        return suffix;
    }

    /**
     * @param project
     * @param changeSet
     * @return
     * @throws CoreException
     * @throws IOException
     */
    private String getSuffixForProject(IProject project) throws CoreException,
            IOException {
        ChangeSet changeSet = null;
        String suffix = ""; //$NON-NLS-1$
        if (!LOCAL_CACHE.isLocalUpdateInProgress(project)) {
            if (isShowChangeset()) {
                LocalChangesetCache.getInstance().getLocalChangeSets(project);
            }
            changeSet = LocalChangesetCache.getInstance()
                    .getCurrentWorkDirChangeset(project);
        } else {
            suffix = Messages.getString("ResourceDecorator.new"); //$NON-NLS-1$
        }
        if (changeSet != null) {
            suffix = " [ "; //$NON-NLS-1$
            String hex = ":" + changeSet.getNodeShort(); //$NON-NLS-1$
            String tags = changeSet.getTag();
            String branch = changeSet.getBranch();
            String merging = project
                    .getPersistentProperty(ResourceProperties.MERGING);

            // rev info
            suffix += changeSet.getChangesetIndex() + hex;

            // branch info
            if (branch != null && branch.length() > 0) {
                suffix += " @ " + branch; //$NON-NLS-1$
            }

            // tags
            if (tags != null && tags.length() > 0) {
                suffix += " (" + tags + ")"; //$NON-NLS-1$ //$NON-NLS-2$
            }

            // merge info
            if (merging != null && merging.length() > 0) {
                suffix += Messages.getString("ResourceDecorator.merging") + merging; //$NON-NLS-1$
            }
            suffix += " ]"; //$NON-NLS-1$
        }
        return suffix;
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
        if (updatedObject instanceof Set) {
            Set<IResource> changed = (Set<IResource>) updatedObject;
            List<IResource> notification = new ArrayList<IResource>(1000);
            int i = 0;
            for (IResource resource : changed) {
                if (i % 1000 == 0 && notification.size() > 0) {
                    fireNotification(notification);
                }
                notification.add(resource);
                i++;
            }
            if (notification.size() > 0) {
                fireNotification(notification);
            }
        }
    }

    /**
     * @param notification
     */
    private void fireNotification(List<IResource> notification) {
        LabelProviderChangedEvent event = new LabelProviderChangedEvent(this,
                notification.toArray());
        fireLabelProviderChanged(event);
        notification.clear();
    }

}
