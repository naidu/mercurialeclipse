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
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
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

/**
 * @author zingo
 * 
 */
public class DecoratorStatus extends LabelProvider implements
		ILightweightLabelDecorator, IResourceChangeListener, Observer {

	// set to true when having 2 different statuses in a folder flags it has
	// modified
	private static boolean folder_logic_2MM;
	private static MercurialStatusCache statusCache;

	/**
	 * 
	 */
	public DecoratorStatus() {
		configureFromPreferences();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
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
		try {
			statusCache.refresh();
		} catch (TeamException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public void decorate(Object element, IDecoration decoration) {
		IResource objectResource = (IResource) element;
		IProject objectProject = objectResource.getProject();

		if (null == RepositoryProvider.getProvider(objectProject,
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
				statusCache.refresh(objectProject);
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
		if (prefix != null) {
			decoration.addPrefix(prefix);
		}
		if (statusCache.isVersionKnown(objectResource)) {
			try {
				ChangeSet changeSet = statusCache.getVersion(objectResource);
				String hex = "";
				if (objectResource.getType() == IResource.PROJECT) {
					hex = ":" + changeSet.getChangeset();
				}

				decoration.addSuffix(" [" + changeSet.getChangesetIndex() + hex + "]");
			} catch (HgException e) {
				MercurialEclipsePlugin
						.logWarning("Couldn't get version of resource "
								+ objectResource, e);
			}
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
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			IResourceDelta[] children = event.getDelta().getAffectedChildren();
			for (IResourceDelta delta : children) {
				final IResource res = delta.getResource();
				if (null != RepositoryProvider.getProvider(res.getProject(),
						MercurialTeamProvider.ID)) {
					// Atleast one resource in a project managed by MEP has
					// changed, schedule a refresh();

					try {
						statusCache.refresh(res.getProject());
					} catch (TeamException e) {
						MercurialEclipsePlugin.logError(
								"Couldn't refresh project:", e);
					}

					return;
				}
			}
		}
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
