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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.model.FlagManager;
import com.vectrace.MercurialEclipse.model.FlagManagerListener;
import com.vectrace.MercurialEclipse.model.FlaggedProject;
import com.vectrace.MercurialEclipse.model.FlaggedResource;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author zingo
 * 
 */
public class ResourceDecorator extends LabelProvider implements ILightweightLabelDecorator,
        FlagManagerListener {

    private FlagManager flagManager = MercurialEclipsePlugin.getDefault().getFlagManager();

    // set to true when having 2 different statuses in a folder flags it has
    // modified
    private static boolean folder_logic_2MM;

    public ResourceDecorator() {
        configureFromPreferences();
        flagManager.addListener(this);
    }

    @Override
    public void dispose() {
        flagManager.removeListener(this);
        super.dispose();
    }

    private static void configureFromPreferences() {
        IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
        folder_logic_2MM = MercurialPreferenceConstants.LABELDECORATOR_LOGIC_2MM.equals(store
                .getString(MercurialPreferenceConstants.LABELDECORATOR_LOGIC));
    }

    public void decorate(Object element, IDecoration decoration) {
        try {
            IResource resource = (IResource) element;
            IProject project = resource.getProject();

            if (null == RepositoryProvider.getProvider(project, MercurialTeamProvider.ID)) {
                return;
            }

            if (!MercurialUtilities.isResourceInReposetory(resource, true)) {
                // Resource could be inside a link or something do nothing
                // in the future this could check is this is another repository
                return;
            }

            FlaggedProject fp = flagManager.getFlaggedProject(project);

            FlaggedResource fr = fp.getFlaggedResource(resource);
            // TODO render fr.isConflict?
            ImageDescriptor overlay = null;
            String prefix = null;
            if (fr != null) {
                BitSet output = fr.getStatus();
                // "ignore" does not really count as modified
                if (folder_logic_2MM
                        && (output.cardinality() > 2 || (output.cardinality() == 2 && !output
                                .get(FlaggedResource.BIT_IGNORE)))) {
                    overlay = DecoratorImages.modifiedDescriptor;
                    prefix = ">";
                } else {
                    switch (output.length() - 1) {
                        case FlaggedResource.BIT_MODIFIED:
                            overlay = DecoratorImages.modifiedDescriptor;
                            prefix = ">";
                            break;
                        case FlaggedResource.BIT_ADDED:
                            overlay = DecoratorImages.addedDescriptor;
                            prefix = ">";
                            break;
                        case FlaggedResource.BIT_UNKNOWN:
                            overlay = DecoratorImages.notTrackedDescriptor;
                            prefix = ">";
                            break;
                        case FlaggedResource.BIT_CLEAN:
                            overlay = DecoratorImages.managedDescriptor;
                            break;
                        // case BIT_IGNORE:
                        // do nothing
                        case FlaggedResource.BIT_REMOVED:
                            overlay = DecoratorImages.removedDescriptor;
                            prefix = ">";
                            break;
                        case FlaggedResource.BIT_DELETED:
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
            if (prefix != null) {
                decoration.addPrefix(prefix);
            }
            if (resource == project) {
                decoration.addSuffix(" [" + fp.getVersion() + "]");
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
    }
    
    /**
     * Called when the configuration of the decorator changes
     */
    public static  void onConfigurationChanged() {
        String decoratorId = ResourceDecorator.class.getName();
        configureFromPreferences();
        PlatformUI.getWorkbench().getDecoratorManager().update(decoratorId);
    }

    public void onRefresh(IProject project) {
        new SafeUiJob("Update Decorations") {
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                String decoratorId = ResourceDecorator.class.getName();
                PlatformUI.getWorkbench().getDecoratorManager().update(decoratorId);
                return Status.OK_STATUS;
            }
        }.schedule();
    }

}
