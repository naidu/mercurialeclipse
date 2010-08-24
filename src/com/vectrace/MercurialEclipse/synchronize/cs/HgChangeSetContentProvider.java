/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   IBM Corporation - initial API and implementation
 *     Andrei Loskutov (Intland) - adopting to hg
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager;
import org.eclipse.team.internal.core.subscribers.IChangeSetChangeListener;
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager.CollectorChangeEvent;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.mapping.SynchronizationContentProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.INavigatorSorterService;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.PresentationMode;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

@SuppressWarnings("restriction")
public class HgChangeSetContentProvider extends SynchronizationContentProvider /* ResourceModelContentProvider */  {

	public static final String ID = "com.vectrace.MercurialEclipse.changeSetContent";

	private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();

	private final class UcommittedSetListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			Object input = getTreeViewer().getInput();
			if (input instanceof HgChangeSetModelProvider) {
				Utils.asyncExec(new Runnable() {
					public void run() {
						TreeViewer treeViewer = getTreeViewer();
						treeViewer.getTree().setRedraw(false);
						treeViewer.refresh(uncommittedSet, true);
						treeViewer.getTree().setRedraw(true);
					}
				}, getTreeViewer());
			}
		}
	}

	private final class PreferenceListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			Object input = getTreeViewer().getInput();
			if (event.getProperty().equals(PresentationMode.PREFERENCE_KEY)
					&& input instanceof HgChangeSetModelProvider) {
				Utils.asyncExec(new Runnable() {
					public void run() {
						TreeViewer treeViewer = getTreeViewer();
						treeViewer.getTree().setRedraw(false);
						treeViewer.refresh(uncommittedSet, true);
						treeViewer.refresh(outgoing, true);
						treeViewer.refresh(incoming, true);
						treeViewer.getTree().setRedraw(true);
					}
				}, getTreeViewer());
			}
		}
	}

	private final class CollectorListener implements IChangeSetChangeListener, BatchingChangeSetManager.IChangeSetCollectorChangeListener {

		public void setAdded(final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			if (isVisibleInMode((ChangeSet) set)) {
				final ChangesetGroup toRefresh = ((ChangeSet) set).getDirection() == Direction.INCOMING ? incoming
						: outgoing;
				boolean added = toRefresh.getChangesets().add((ChangeSet) set);
				if(added) {
					Utils.asyncExec(new Runnable() {
						public void run() {
							getTreeViewer().refresh(toRefresh, true);
						}
					}, getTreeViewer());
				}
			}
		}

		public void setRemoved(final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			if (isVisibleInMode((ChangeSet) set)) {
				final ChangesetGroup toRefresh = ((ChangeSet) set).getDirection() == Direction.INCOMING ? incoming
						: outgoing;
				boolean removed = toRefresh.getChangesets().remove(set);
				if(removed) {
					Utils.asyncExec(new Runnable() {
						public void run() {
							getTreeViewer().refresh(toRefresh, true);
						}
					}, getTreeViewer());
				}
			}
		}

		public void defaultSetChanged(final org.eclipse.team.internal.core.subscribers.ChangeSet previousDefault,
				final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			// user has requested a manual refresh: simply refresh root elements
			getRootElements();
		}

		public void nameChanged(final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			// ignored
		}

		public void resourcesChanged(final org.eclipse.team.internal.core.subscribers.ChangeSet set, final IPath[] paths) {
			// ignored
		}

		public void changeSetChanges(final CollectorChangeEvent event, IProgressMonitor monitor) {
			// ignored
		}
	}

	private HgChangesetsCollector csCollector;
	private boolean collectorInitialized;
	private final WorkingChangeSet uncommittedSet;
	private final IChangeSetChangeListener collectorListener;
	private final IPropertyChangeListener uncommittedSetListener;
	private final IPropertyChangeListener preferenceListener;
	private final ChangesetGroup incoming;
	private final ChangesetGroup outgoing;
	private WorkbenchContentProvider provider;

	public HgChangeSetContentProvider() {
		super();
		uncommittedSet = new WorkingChangeSet("Uncommitted");
		incoming = new ChangesetGroup("Incoming", Direction.INCOMING);
		outgoing = new ChangesetGroup("Outgoing", Direction.OUTGOING);
		collectorListener = new CollectorListener();
		uncommittedSetListener = new UcommittedSetListener();
		preferenceListener = new PreferenceListener();

		MercurialEclipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(preferenceListener);
	}

	@Override
	protected String getModelProviderId() {
		return HgChangeSetModelProvider.ID;
	}

	private boolean isOutgoingVisible(){
		return getConfiguration().getMode() != ISynchronizePageConfiguration.INCOMING_MODE;
	}

	private boolean isIncomingVisible(){
		return getConfiguration().getMode() != ISynchronizePageConfiguration.OUTGOING_MODE;
	}

	private boolean isEnabled() {
		final Object input = getViewer().getInput();
		return input instanceof HgChangeSetModelProvider;
	}

	@Override
	public Object[] getChildren(Object parent) {
		return getElements(parent);
	}

	@Override
	public Object[] getElements(Object parent) {
		if (parent instanceof ISynchronizationContext) {
			// Do not show change sets when all models are visible because
			// model providers that override the resource content may cause
			// problems for the change set content provider
			return new Object[0];
		}
		if(isEnabled()){
			if(!((HgChangeSetModelProvider) getViewer().getInput()).isParticipantCreated()){
				initCollector();
				// on startup, do not start to show anything for the first time:
				// show "reminder" page which allows user to choose synchronize or not
				// return new Object[0];
				// TODO right now it doesn't make sense to show "reminder page" as we
				// connect to the remote servers automatically as soon as the sync view
				// shows up.
			}
		}

		if (parent == getModelProvider()) {
			return getRootElements();
		} else if (parent instanceof ChangesetGroup) {
			ChangesetGroup group = (ChangesetGroup) parent;
			Direction direction = group.getDirection();
			if (isOutgoingVisible()	&& isOutgoing(direction)) {
				return group.getChangesets().toArray();
			}
			if(isIncomingVisible() && direction == Direction.INCOMING){
				return group.getChangesets().toArray();
			}
		} else if (parent instanceof ChangeSet) {
			FileFromChangeSet[] files = ((ChangeSet) parent).getChangesetFiles();

			if (files.length != 0) {
				switch (PresentationMode.get()) {
				case FLAT:
					return files;
				case TREE:
					return collectTree(files);
				case COMPRESSED_TREE:
					return collectCompressedTree(files);
				}
			}
		} else if (parent instanceof PathFromChangeSet) {
			return ((PathFromChangeSet) parent).getChildren();
		}

		return new Object[0];
	}

	private Object[] collectTree(FileFromChangeSet[] files) {
		List<Object> out = new ArrayList<Object>(files.length * 2);

		for (FileFromChangeSet file : files) {
			out.add(file.getPath().removeLastSegments(1));
			out.add(file);
		}

		return collectTree(out);
	}

	private Object[] collectTree(List<Object> files) {
		HashMap<String, List<Object>> map = new HashMap<String, List<Object>>();
		List<Object> out = new ArrayList<Object>();

		for (Iterator<Object> it = files.iterator(); it.hasNext();) {
			IPath path = (IPath) it.next();
			FileFromChangeSet file = (FileFromChangeSet) it.next();

			if (path == null || 0 == path.segmentCount()) {
				out.add(file);
			} else {
				String seg = path.segment(0);
				List<Object> l = map.get(seg);

				if (l == null) {
					map.put(seg, l = new ArrayList<Object>());
				}

				l.add(path.removeFirstSegments(1));
				l.add(file);
			}
		}

		for (String seg : map.keySet()) {
			final List<Object> data = map.get(seg);

			out.add(new PathFromChangeSet(seg) {
				@Override
				public Object[] getChildren() {
					return collectTree(data);
				}
			});
		}

		return out.toArray(new Object[out.size()]);
	}

	private Object[] collectCompressedTree(FileFromChangeSet[] files) {
		HashMap<IPath, List<FileFromChangeSet>> map = new HashMap<IPath, List<FileFromChangeSet>>();
		List<Object> out = new ArrayList<Object>();

		for (FileFromChangeSet file : files) {
			IPath path = file.getPath().removeLastSegments(1);
			List<FileFromChangeSet> l = map.get(path);

			if (l == null) {
				map.put(path, l = new ArrayList<FileFromChangeSet>());
			}

			l.add(file);
		}

		for (IPath path : map.keySet()) {
			final List<FileFromChangeSet> data = map.get(path);
			if (path.isEmpty()) {
				out.addAll(data);
			} else {
				out.add(new PathFromChangeSet(path.toString()) {
					@Override
					public Object[] getChildren() {
						return data.toArray(new FileFromChangeSet[data.size()]);
					}
				});
			}
		}

		return out.toArray(new Object[out.size()]);
	}

	private void ensureRootsAdded() {
		TreeViewer viewer = getTreeViewer();
		TreeItem[] children = viewer.getTree().getItems();
		if(children.length == 0){
			viewer.add(viewer.getInput(), getRootElements());
		}
	}

	private Object[] getRootElements() {
		initCollector();
		List<ChangeSet> result = new ArrayList<ChangeSet>();
		Collection<ChangeSet> allSets = getAllSets();
		for (ChangeSet set : allSets) {
			if (hasChildren(set)) {
				result.add(set);
			}
		}
		boolean showOutgoing = isOutgoingVisible();
		boolean showIncoming = isIncomingVisible();
		for (ChangeSet set : result) {
			Direction direction = set.getDirection();
			if(showOutgoing && (isOutgoing(direction))){
				outgoing.getChangesets().add(set);
			}
			if(showIncoming && direction == Direction.INCOMING){
				incoming.getChangesets().add(set);
			}
		}
		addAllUnassignedToUnassignedSet();
		return new Object[]{uncommittedSet, outgoing, incoming};
	}

	private synchronized void initCollector() {
		if (!collectorInitialized) {
			initializeChangeSets(getChangeSetCapability());
			collectorInitialized = true;
		}
	}

	private void addAllUnassignedToUnassignedSet() {
		uncommittedSet.update(STATUS_CACHE, null);
	}


	@Override
	protected ResourceTraversal[] getTraversals(
			ISynchronizationContext context, Object object) {
		if (object instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) object;
			IResource[] resources = set.getResources();
			return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		if(object instanceof IResource){
			IResource[] resources = new IResource[]{(IResource) object};
			return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		if(object instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) object;
			Set<ChangeSet> changesets = group.getChangesets();
			Set<IFile> all = new HashSet<IFile>();
			for (ChangeSet changeSet : changesets) {
				all.addAll(changeSet.getFiles());
			}
			IResource[] resources = all.toArray(new IResource[0]);
			return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		return new ResourceTraversal[0];
	}

	/**
	 * @see org.eclipse.team.ui.mapping.SynchronizationContentProvider#hasChildrenInContext(org.eclipse.team.core.mapping.ISynchronizationContext,
	 *      java.lang.Object)
	 */
	@Override
	protected boolean hasChildrenInContext(ISynchronizationContext context, Object element) {
		if (element instanceof ChangeSet) {
			return hasChildren((ChangeSet) element);
		} else if (element instanceof ChangesetGroup) {
			ChangesetGroup group = (ChangesetGroup) element;
			Direction direction = group.getDirection();
			if (isOutgoingVisible()	&& isOutgoing(direction)) {
				return true;
			}
			if(isIncomingVisible() && direction == Direction.INCOMING){
				return true;
			}
		} else if (element instanceof PathFromChangeSet) {
			return true;
		}
		return false;
	}

	private boolean isVisibleInMode(ChangeSet cs) {
		int mode = getConfiguration().getMode();
		if (cs != null) {
			switch (mode) {
			case ISynchronizePageConfiguration.BOTH_MODE:
				return true;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				return containsConflicts(cs);
			case ISynchronizePageConfiguration.INCOMING_MODE:
				return  cs.getDirection() == Direction.INCOMING;
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				return hasConflicts(cs) || isOutgoing(cs.getDirection());
			default:
				break;
			}
		}
		return true;
	}

	private boolean isOutgoing(Direction direction) {
		return direction == Direction.OUTGOING || direction == Direction.LOCAL;
	}

	private boolean hasConflicts(ChangeSet cs) {
		// Conflict mode not meaningful in a DVCS
		return false;
	}

	private boolean containsConflicts(ChangeSet cs) {
		// Conflict mode not meaningful in a DVCS
		return false;
	}

	private boolean hasChildren(ChangeSet changeset) {
		return isVisibleInMode(changeset)
				&& (!changeset.getFiles().isEmpty() || changeset.getChangesetFiles().length > 0);
	}

	/**
	 * Return all the change sets (incoming and outgoing). This
	 * list must not include the unassigned set.
	 * @return all the change sets (incoming and outgoing)
	 */
	private Collection<ChangeSet> getAllSets() {
		if (csCollector != null) {
			return csCollector.getChangeSets();
		}
		return new HashSet<ChangeSet>();
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		HgChangeSetSorter sorter = getSorter();
		if (sorter != null) {
			sorter.setConfiguration(getConfiguration());
		}
		MercurialSynchronizeParticipant participant = (MercurialSynchronizeParticipant) getConfiguration().getParticipant();
		uncommittedSet.setContext((HgSubscriberMergeContext) participant.getContext());
	}

	private HgChangeSetSorter getSorter() {
		INavigatorContentService contentService = getExtensionSite().getService();
		INavigatorSorterService sortingService = contentService.getSorterService();
		INavigatorContentExtension extension = getExtensionSite().getExtension();
		if (extension != null) {
			ViewerSorter sorter = sortingService.findSorter(extension.getDescriptor(),
					getModelProvider(), incoming, incoming);
			if (sorter instanceof HgChangeSetSorter) {
				return (HgChangeSetSorter) sorter;
			}
		}
		return null;
	}

	private void initializeChangeSets(ChangeSetCapability csc) {
		if (csc.supportsCheckedInChangeSets()) {
			csCollector = ((HgChangeSetCapability) csc).createSyncInfoSetChangeSetCollector(getConfiguration());
			csCollector.addListener(collectorListener);
			IProject[] projects = csCollector.getSubscriber().getProjects();
			uncommittedSet.setRoots(projects);
			uncommittedSet.addListener(uncommittedSetListener);
			STATUS_CACHE.addObserver(uncommittedSet);
		}
	}

	@Override
	public void dispose() {
		if (csCollector != null) {
			csCollector.removeListener(collectorListener);
			csCollector.dispose();
		}
		MercurialEclipsePlugin.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceListener);
		uncommittedSet.removeListener(uncommittedSetListener);
		STATUS_CACHE.deleteObserver(uncommittedSet);
		uncommittedSet.dispose();
		outgoing.getChangesets().clear();
		incoming.getChangesets().clear();
		super.dispose();
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		Utils.asyncExec(new Runnable() {
			public void run() {
				ensureRootsAdded();
			}
		}, getTreeViewer());

		if (csCollector != null) {
			csCollector.handleChange(event);
		}

		// no other updates here, as it simply doesn't fit into the changeset concept.
	}

	private ChangeSetCapability getChangeSetCapability() {
		ISynchronizeParticipant participant = getConfiguration().getParticipant();
		if (participant instanceof IChangeSetProvider) {
			IChangeSetProvider csProvider = (IChangeSetProvider) participant;
			return csProvider.getChangeSetCapability();
		}
		return null;
	}

	private TreeViewer getTreeViewer() {
		return (TreeViewer) getViewer();
	}

	@Override
	protected ITreeContentProvider getDelegateContentProvider() {
		if (provider == null) {
			provider = new WorkbenchContentProvider();
		}
		return provider;
	}

	@Override
	protected Object getModelRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 *
	 * @param file may be null
	 * @return may return null, if the given file is null, not selected or is not contained
	 * in any selected changesets
	 */
	public ChangeSet getParentOfSelection(FileFromChangeSet file){
		TreeItem[] selection = getTreeViewer().getTree().getSelection();
		for (TreeItem treeItem : selection) {
			if(treeItem.getData() != file){
				continue;
			}
			TreeItem parentItem = treeItem.getParentItem();

			while (parentItem != null && !(parentItem.getData() instanceof ChangeSet)) {
				parentItem = parentItem.getParentItem();
			}

			if (parentItem != null) {
				return (ChangeSet) parentItem.getData();
			}
		}
		return null;
	}

	/**
	 * @param changeset may be null
	 * @return may return null
	 */
	public ChangesetGroup getParentGroup(ChangeSet changeset){
		if(changeset == null || changeset instanceof WorkingChangeSet){
			return null;
		}
		if(changeset.getDirection() == Direction.INCOMING){
			return incoming;
		}
		return outgoing;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HgChangeSetContentProvider [collectorInitialized=");
		builder.append(collectorInitialized);
		builder.append(", ");
		if (csCollector != null) {
			builder.append("csCollector=");
			builder.append(csCollector);
			builder.append(", ");
		}
		if (provider != null) {
			builder.append("provider=");
			builder.append(provider);
			builder.append(", ");
		}
		if (incoming != null) {
			builder.append("incoming=");
			builder.append(incoming);
			builder.append(", ");
		}
		if (outgoing != null) {
			builder.append("outgoing=");
			builder.append(outgoing);
			builder.append(", ");
		}
		if (uncommittedSet != null) {
			builder.append("uncommittedSet=");
			builder.append(uncommittedSet);
		}
		builder.append("]");
		return builder.toString();
	}

	// inner types

	public abstract static class PathFromChangeSet {

		private final String display;

		/**
		 * Constructor for tree mode
		 *
		 * @param seg
		 *            The leading segment
		 */
		public PathFromChangeSet(String seg) {
			display = seg;
		}

		@Override
		public String toString() {
			return display;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object other) {
			return (other instanceof PathFromChangeSet) && ((PathFromChangeSet)other).display.equals(display);
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return 73 ^ display.hashCode();
		}

		public abstract Object[] getChildren();
	}
}
