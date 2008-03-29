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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author zingo
 * 
 */
public class DecoratorStatus extends LabelProvider implements ILightweightLabelDecorator, IResourceChangeListener {

	//relative order for folders
	private final static int BIT_DELETED    = 0;
	private final static int BIT_REMOVED    = 1;
	private final static int BIT_IGNORE     = 2;
	private final static int BIT_CLEAN      = 3;
	private final static int BIT_UNKNOWN    = 4;
	private final static int BIT_ADDED      = 5;
	private final static int BIT_MODIFIED   = 6;
	private final static int BIT_IMPOSSIBLE = 7;
	
	/** Used to store the last known status of a resource */
	private static Map<IResource, BitSet> statusMap = new HashMap<IResource, BitSet>();

	/** Used to store which projects have already been parsed */
	private static Set<IProject> knownStatus = new HashSet<IProject>();

	private static Map<IProject, String> versions = new HashMap<IProject, String>();

	//set to true when having 2 different statuses in a folder flags it has modified
	private static boolean folder_logic_2MM;
	
	/**
	 * 
	 */
	public DecoratorStatus() {
		configureFromPreferences();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	private static void configureFromPreferences() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault()
				.getPreferenceStore();
		folder_logic_2MM = MercurialPreferenceConstants.LABELDECORATOR_LOGIC_2MM
				.equals(store
						.getString(MercurialPreferenceConstants.LABELDECORATOR_LOGIC));
	}
	
	/** 
	 * Clears the known status of all resources and projects.
	 * and calls for a update of decoration
	 *  
	 */
	public static void refresh() {
		/* While this clearing of status is a "naive" implementation, it is simple. */
		IWorkbench workbench = PlatformUI.getWorkbench();
		String decoratorId = DecoratorStatus.class.getName();
		configureFromPreferences();
		workbench.getDecoratorManager().update(decoratorId);
		statusMap.clear();
		knownStatus.clear();
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

		if (!knownStatus.contains(objectProject)) {
			/* hg status on project (all files) instead of per file basis*/
			try {
				refresh(objectProject);
			} catch (HgException ex) {
				MercurialEclipsePlugin.logError(ex);
				return;
			}
		}

		BitSet output = statusMap.get(element);
		ImageDescriptor overlay = null;
		String prefix = null;
		if(output!=null) {
			if(folder_logic_2MM && output.cardinality()>1) {
				overlay = DecoratorImages.modifiedDescriptor;
				prefix = ">";
			} else {
				switch(output.length()-1) {
					case BIT_MODIFIED:
						overlay = DecoratorImages.modifiedDescriptor;
						prefix = ">";
						break;
					case BIT_ADDED:
						overlay = DecoratorImages.addedDescriptor;
						prefix = ">";
						break;
					case BIT_UNKNOWN:
						overlay = DecoratorImages.notTrackedDescriptor;
						prefix = ">";
						break;
					case BIT_CLEAN:
						overlay = DecoratorImages.managedDescriptor;
						break;
					//case BIT_IGNORE:
					//do nothing
					case BIT_REMOVED:
						overlay = DecoratorImages.removedDescriptor;
						prefix = ">";
						break;
					case BIT_DELETED:
						overlay = DecoratorImages.deletedStillTrackedDescriptor;
						prefix = ">";
						break;
				}
			}
		} else {
			//empty folder, do nothing
		}
		if(overlay != null) {
			decoration.addOverlay(overlay);
		}
		if(prefix != null) {
			decoration.addPrefix(prefix);
		}
		if (versions.containsKey(element)) {
			decoration.addSuffix(" [" + versions.get(element) + "]");
		}
	}

	/**
	 * @param project
	 * @throws HgException
	 */
	private void refresh(IProject project) throws HgException {
		versions.put(project, HgIdentClient.getCurrentRevision(project));
		parseStatusCommand(project, HgStatusClient.getStatus(project));
	}

	/**
	 * @param output
	 */
	private void parseStatusCommand(IProject ctr, String output) {
		IContainer ctrParent = ctr.getParent();
		knownStatus.add(ctr);
		Scanner scanner = new Scanner(output);
		while (scanner.hasNext()) {
			String status = scanner.next();
			String localName = scanner.nextLine();
			IResource member = ctr.getFile(localName.trim());

			BitSet bitSet = new BitSet();
			bitSet.set(getBitIndex(status.charAt(0)));
			statusMap.put(member, bitSet);
			
			//ancestors
			for (IResource parent = member.getParent(); parent != ctrParent; parent = parent.getParent()) {
				BitSet parentBitSet = statusMap.get(parent);
				if(parentBitSet!=null) {
					bitSet = (BitSet)bitSet.clone();
					bitSet.or(parentBitSet);
				}
				statusMap.put(parent, bitSet);
			}
		}
	}
	
	private final int getBitIndex(char status) {
		switch(status) {
			case '!':
				return BIT_DELETED;
			case 'R':
				return BIT_REMOVED;
			case 'I':
				return BIT_IGNORE;
			case 'C':
				return BIT_CLEAN;
			case '?':
				return BIT_UNKNOWN;
			case 'A':
				return BIT_ADDED;
			case 'M':
				return BIT_MODIFIED;
			default:
				MercurialEclipsePlugin.logWarning("Unknown status: '"+status+"'", null);
				return BIT_IMPOSSIBLE;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object,
	 *      java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
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
				IResource res = delta.getResource();
				if (null != RepositoryProvider.getProvider(res.getProject(),
						MercurialTeamProvider.ID)) {
					// Atleast one resource in a project managed by MEP has
					// changed, schedule a refresh();

					new SafeUiJob("Update Decorations") {
						@Override
						protected IStatus runSafe(IProgressMonitor monitor) {
							refresh();
							return super.runSafe(monitor);
						}
					}.schedule();
					return;
				}
			}
		}
	}
}
