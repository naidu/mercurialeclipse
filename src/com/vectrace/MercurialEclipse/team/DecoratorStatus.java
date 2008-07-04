/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - large contribution
 *     Jerome Negre              - fixing folders' state
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
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
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
public class DecoratorStatus extends LabelProvider implements
		ILightweightLabelDecorator, Observer {

	// set to true when having 2 different statuses in a folder flags it has
	// modified
	private static boolean folder_logic_2MM;
	private static MercurialStatusCache statusCache;

	/**
	 * 
	 */
	public DecoratorStatus() {
		configureFromPreferences();		
		statusCache = MercurialStatusCache.getInstance();
		statusCache.addObserver(this);
	}

	private static void configureFromPreferences() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault()
				.getPreferenceStore();
		folder_logic_2MM = MercurialPreferenceConstants.LABELDECORATOR_LOGIC_2MM
				.equals(store
						.getString(MercurialPreferenceConstants.LABELDECORATOR_LOGIC));
	}

	/**
	 * Clears the known status of all resources and projects. and calls for a
	 * update of decoration
	 * 
	 */
	public static void refresh() {
		/*
		 * While this clearing of status is a "naive" implementation, it is
		 * simple.
		 */
		configureFromPreferences();
		// try {
		// statusCache.refresh();
		// } catch (TeamException e) {
		// MercurialEclipsePlugin.logError(e);
		// }
	}

	public void decorate(Object element, IDecoration decoration) {
		IResource objectResource = (IResource) element;
		IProject objectProject = objectResource.getProject();

		if (objectProject == null || null == RepositoryProvider.getProvider(objectProject,
				MercurialTeamProvider.ID)) {
			return;
		}

		if (MercurialUtilities.isResourceInReposetory(objectResource, true) != true) {
			// Resource could be inside a link or something do nothing
			// in the future this could check is this is another repository
			return;
		}

		if (!statusCache.isStatusKnown((objectProject))) {
			try {
				statusCache.refreshStatus(objectProject,null);
			} catch (TeamException ex) {
				MercurialEclipsePlugin.logError(ex);
				return;
			}
		}

		BitSet output = statusCache.getStatus(objectResource);
		ImageDescriptor overlay = null;
		String prefix = null;
		if (output != null) {
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

		ChangeSet cs = null;
		try {
			cs = IncomingChangesetCache.getInstance().getNewestIncomingChangeSet(objectResource);
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

		try {
			ChangeSet changeSet = LocalChangesetCache.getInstance().getNewestLocalChangeSet(objectResource);
			if (changeSet != null) {
				String hex = ":" + changeSet.getNodeShort();
				String suffix = " [" + changeSet.getChangesetIndex() + hex
						+ "]";

				if (objectResource.getType() == IResource.FILE) {
					suffix = " [" + changeSet.getChangesetIndex() +"] ";

					if (cs != null) {
						suffix += "< [" + cs.getChangesetIndex() + ":"
								+ cs.getNodeShort() + " " + cs.getUser() + "]";
					}
				}

				if (objectResource.getType() != IResource.FOLDER) {
					decoration.addSuffix(suffix);
				}
			}

		} catch (HgException e) {
			MercurialEclipsePlugin.logWarning(
					"Couldn't get version of resource " + objectResource, e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
	}
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object,
	 *      java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	public void update(Observable o, Object updatedObject) {
		if (o == statusCache) {
			final IWorkbench workbench = PlatformUI.getWorkbench();
			final String decoratorId = DecoratorStatus.class.getName();
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
