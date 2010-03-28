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
import java.util.HashSet;
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
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager.CollectorChangeEvent;
import org.eclipse.team.internal.core.subscribers.IChangeSetChangeListener;
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

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.HgSubscriberMergeContext;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

@SuppressWarnings("restriction")
public class HgChangeSetContentProvider extends SynchronizationContentProvider /* ResourceModelContentProvider */ {

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

	private final class CollectorListener implements IChangeSetChangeListener, BatchingChangeSetManager.IChangeSetCollectorChangeListener {

		public void setAdded(final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			if (isVisibleInMode((ChangeSet)set)) {
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
			if (isVisibleInMode((ChangeSet)set)) {
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
	private final ChangesetGroup incoming;
	private final ChangesetGroup outgoing;
	private WorkbenchContentProvider provider;

	private final UncommittedChangesetManager uncommittedCsManager;

	public HgChangeSetContentProvider() {
		super();
		uncommittedSet = new WorkingChangeSet("Uncommitted");
		incoming = new ChangesetGroup("Incoming", Direction.INCOMING);
		outgoing = new ChangesetGroup("Outgoing", Direction.OUTGOING);
		collectorListener = new CollectorListener();
		uncommittedSetListener = new UcommittedSetListener();
		uncommittedCsManager = new UncommittedChangesetManager(this);
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
		return (input instanceof HgChangeSetModelProvider);
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
			if(!((HgChangeSetModelProvider)getViewer().getInput()).isParticipantCreated()){
				initCollector();
				// on startup, do not start to show anything for the first time:
				// show "reminder" page which allows user to choose synchronize or not
				return new Object[0];
			}
		}
		if (parent == getModelProvider()) {
			return getRootElements();
		}
		if (parent instanceof ChangeSet) {
			return ((ChangeSet)parent).getChangesetFiles();
		}
		if (parent instanceof ChangesetGroup) {
			ChangesetGroup group = (ChangesetGroup) parent;
			Direction direction = group.getDirection();
			if (isOutgoingVisible()	&& isOutgoing(direction)) {
				return group.getChangesets().toArray();
			}
			if(isIncomingVisible() && direction == Direction.INCOMING){
				return group.getChangesets().toArray();
			}
			if(direction == Direction.LOCAL){
				return group.getChangesets().toArray();
			}
		}
		return new Object[0];
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
		// return super.getTraversals(context, object);
	}

	/**
	 * Return whether the given element has children in the given
	 * context. The children may or may not exist locally.
	 * By default, this method returns true if the traversals for
	 * the element contain any diffs. This could result in false
	 * positives. Subclasses can override to provide a more
	 * efficient or precise answer.
	 * @param element a model element.
	 * @return whether the given element has children in the given context
	 */
	@Override
	protected boolean hasChildrenInContext(ISynchronizationContext context, Object element) {
		return internalHasChildren(element);
//		ResourceTraversal[] traversals = getTraversals(context, element);
//		if (traversals == null) {
//			return true;
//		}
//		return context.getDiffTree().getDiffs(traversals).length > 0;
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
		// XXX implement conflicts display
//		if (cs instanceof DiffChangeSet) {
//			DiffChangeSet dcs = (DiffChangeSet) cs;
//			return dcs.getDiffTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) > 0;
//		}
		return false;
	}

	private boolean containsConflicts(ChangeSet cs) {
		// XXX implement conflicts display
//		if (cs instanceof DiffChangeSet) {
//			DiffChangeSet dcs = (DiffChangeSet) cs;
//			return dcs.getDiffTree().hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), ResourceModelLabelProvider.CONFLICT_FILTER);
//		}
		return false;
	}


	private boolean internalHasChildren(Object first) {
		if (first instanceof ChangeSet) {
			return hasChildren((ChangeSet)first);
		}
		if (first instanceof ChangesetGroup) {
			ChangesetGroup group = (ChangesetGroup) first;
			Direction direction = group.getDirection();
			if (isOutgoingVisible()	&& isOutgoing(direction)) {
				return true;
			}
			if(isIncomingVisible() && direction == Direction.INCOMING){
				return true;
			}
		}
		return false;
	}

	private boolean hasChildren(ChangeSet changeset) {
		return isVisibleInMode(changeset) && hasChildrenInContext(changeset);
	}

	private boolean hasChildrenInContext(ChangeSet set) {
		return !set.getFiles().isEmpty() || set.getChangesetFiles().length > 0;
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
			csCollector = ((HgChangeSetCapability)csc).createSyncInfoSetChangeSetCollector(getConfiguration());
			csCollector.addListener(collectorListener);
			IProject[] projects = csCollector.getSubscriber().getProjects();
			uncommittedCsManager.setProjects(projects);
			uncommittedSet.setRoots(uncommittedCsManager);
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
		return ((TreeViewer)getViewer());
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
			if(parentItem != null){
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


}
