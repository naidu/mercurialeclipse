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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager;
import org.eclipse.team.internal.core.subscribers.IChangeSetChangeListener;
import org.eclipse.team.internal.core.subscribers.BatchingChangeSetManager.CollectorChangeEvent;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceModelContentProvider;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.INavigatorContentExtension;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.navigator.INavigatorSorterService;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

@SuppressWarnings("restriction")
public class HgChangeSetContentProvider extends ResourceModelContentProvider  {

	private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();

	private final class UcommittedSetListener implements IPropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			Object input = getTreeViewer().getInput();
			if (input instanceof HgChangeSetModelProvider) {
				Utils.asyncExec(new Runnable() {
					public void run() {
						TreeViewer treeViewer = getTreeViewer();
						treeViewer.refresh(uncommittedSet, true);
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
			// ignored
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

	public HgChangeSetContentProvider() {
		super();
		uncommittedSet = new WorkingChangeSet("Uncommitted");
		incoming = new ChangesetGroup("Incoming", Direction.INCOMING);
		outgoing = new ChangesetGroup("Outgoing", Direction.OUTGOING);
		collectorListener = new CollectorListener();
		uncommittedSetListener = new UcommittedSetListener();
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
	public Object[] getElements(Object parent) {
		if (parent instanceof ISynchronizationContext) {
			// Do not show change sets when all models are visible because
			// model providers that override the resource content may cause
			// problems for the change set content provider
			return new Object[0];
		}
		if(isEnabled()){
			if(!((HgChangeSetModelProvider)getViewer().getInput()).isParticipantCreated()){
				// on startup, do not start to show anything for the first time:
				// show "reminder" page which allows user to choose synchronize or not
				initCollector();
				return new Object[0];
			}
		}
		if (parent == getModelProvider()) {
			return getRootElements();
		}
		return super.getElements(parent);
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

	private void initCollector() {
		if (!collectorInitialized) {
			initializeChangeSets(getChangeSetCapability());
			collectorInitialized = true;
		}
	}

	private void addAllUnassignedToUnassignedSet() {
		Set<IResource> dirty = getDirtyFiles();
		try {
			uncommittedSet.beginInput();
			uncommittedSet.clear();
			for (IResource resource : dirty) {
				if(resource instanceof IFile) {
					uncommittedSet.add((IFile) resource);
				}
			}
		} finally {
			uncommittedSet.endInput(null);
		}
	}

	private Set<IResource> getDirtyFiles() {
		HgChangeSetModelProvider modelProvider = (HgChangeSetModelProvider) getModelProvider();
		IProject[] projects = modelProvider.getSubscriber().getProjects();
		int bits = MercurialStatusCache.MODIFIED_MASK;
		Set<IResource> resources = new HashSet<IResource>();
		for (IProject project : projects) {
			resources.addAll(STATUS_CACHE.getFiles(bits, project));
		}
		return resources;
	}

	@Override
	protected ResourceTraversal[] getTraversals(
			ISynchronizationContext context, Object object) {
		if (object instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) object;
			IResource[] resources = set.getResources();
			return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
		}
		return super.getTraversals(context, object);
	}

	@Override
	public Object[] getChildren(TreePath parentPath) {
		if (!isEnabled()) {
			return new Object[0];
		}
		if (parentPath.getSegmentCount() == 0) {
			return getRootElements();
		}
		Object child = parentPath.getLastSegment();
		if (child instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) child;
			if(isVisibleInMode(group.getDirection())) {
				return group.getChangesets().toArray();
			}
		} else if (child instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) child;
			return set.getResources();
		} else if(child instanceof IFile){
			return new Object[0];
		}
		return new Object[0];
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

	private boolean isVisibleInMode(Direction direction) {
		if (direction == Direction.INCOMING) {
			return isIncomingVisible();
		}
		if (isOutgoing(direction)) {
			return isOutgoingVisible();
		}
		return false;
	}

	private boolean isOutgoing(Direction direction) {
		return direction == Direction.OUTGOING || direction == Direction.LOCAL;
	}

	private boolean hasConflicts(ChangeSet cs) {
		// XXX
//		if (cs instanceof DiffChangeSet) {
//			DiffChangeSet dcs = (DiffChangeSet) cs;
//			return dcs.getDiffTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) > 0;
//		}
		return false;
	}

	private boolean containsConflicts(ChangeSet cs) {
		// XXX
//		if (cs instanceof DiffChangeSet) {
//			DiffChangeSet dcs = (DiffChangeSet) cs;
//			return dcs.getDiffTree().hasMatchingDiffs(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), ResourceModelLabelProvider.CONFLICT_FILTER);
//		}
		return false;
	}


	@Override
	public boolean hasChildren(TreePath path) {
		if (path.getSegmentCount() == 1) {
			Object first = path.getFirstSegment();
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
		}
		return getChildren(path).length > 0;
	}

	private boolean hasChildren(ChangeSet changeset) {
		return isVisibleInMode(changeset) && hasChildrenInContext(changeset);
	}

	private boolean hasChildrenInContext(ChangeSet set) {
		return !set.getFiles().isEmpty();
	}

	@Override
	public TreePath[] getParents(Object element) {
		if (element instanceof WorkingChangeSet) {
			return new TreePath[] { TreePath.EMPTY };
		}
		if (element instanceof ChangeSet) {
			if(((ChangeSet) element).getDirection() == Direction.INCOMING){
				return new TreePath[]{new TreePath(new Object[]{incoming})};
			}
			return new TreePath[]{new TreePath(new Object[]{outgoing})};
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			ChangeSet[] sets = getSetsContaining(resource);
			if (sets.length > 0) {
				List<TreePath> result = new ArrayList<TreePath>();
				for (int i = 0; i < sets.length; i++) {
					ChangeSet set = sets[i];
					TreePath path = getPathForElement(set, resource);
					if (path != null) {
						result.add(path);
					}
				}
				return result.toArray(new TreePath[result.size()]);
			}
			TreePath path = getPathForElement(uncommittedSet, resource);
			if (path != null) {
				return new TreePath[] { path };
			}
		}

		return new TreePath[0];
	}

	private ChangeSet[] getSetsContaining(IResource resource) {
		List<ChangeSet> result = new ArrayList<ChangeSet>();
		Collection<ChangeSet> allSets = getAllSets();
		for (ChangeSet set : allSets) {
			if (set.contains(resource)) {
				result.add(set);
			}
		}
		return result.toArray(new ChangeSet[result.size()]);
	}

	/**
	 * Return all the change sets (incoming and outgoing). This
	 * list must not include the unassigned set.
	 * @return all the change sets (incoming and outgoing)
	 */
	private Collection<ChangeSet> getAllSets() {
		Set<ChangeSet> result = new HashSet<ChangeSet>();
		if (csCollector != null) {
			org.eclipse.team.internal.core.subscribers.ChangeSet[] sets = csCollector.getSets();
			for (org.eclipse.team.internal.core.subscribers.ChangeSet set : sets) {
				result.add((ChangeSet) set);
			}
		}
		return result;
	}

	private TreePath getPathForElement(ChangeSet set, IResource resource) {
		List<Object> pathList = getPath(set, resource);
		if (pathList != null) {
			pathList.add(0, set);
			return new TreePath(pathList.toArray());
		}
		return null;
	}

	private List<Object> getPath(ChangeSet set, IResource resource) {
		if (resource == null || resource.getType() == IResource.ROOT
				|| !set.contains(resource)) {
			return null;
		}
		List<Object> result = new ArrayList<Object>();
		result.add(resource.getProject());
		if (resource.getType() != IResource.PROJECT) {
			String layout = getTraversalCalculator().getLayout();
			if (layout.equals(IPreferenceIds.FLAT_LAYOUT)) {
				result.add(resource.getParent());
			} else if (layout.equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FOLDER) {
				result.add(resource.getParent());
			} else if (layout.equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FILE) {
				IContainer parent = resource.getParent();
				if (parent.getType() != IResource.PROJECT) {
					result.add(parent);
				}
			} else {
				List<IResource> resourcePath = new ArrayList<IResource>();
				IResource next = resource;
				while (next.getType() != IResource.PROJECT) {
					resourcePath.add(next);
					next = next.getParent();
				}
				for (int i = resourcePath.size() - 1; i >=0; i--) {
					result.add(resourcePath.get(i));
				}
			}
		}
		return result;
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		HgChangeSetSorter sorter = getSorter();
		if (sorter != null) {
			sorter.setConfiguration(getConfiguration());
		}
	}

	private HgChangeSetSorter getSorter() {
		INavigatorContentService contentService = getExtensionSite().getService();
		INavigatorSorterService sortingService = contentService.getSorterService();
		INavigatorContentExtension extension = getExtensionSite().getExtension();
		if (extension != null) {
			ViewerSorter sorter = sortingService.findSorter(extension.getDescriptor(),
					getModelProvider(), new WorkingChangeSet(""), new WorkingChangeSet(""));
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
		uncommittedSet.clear();
		outgoing.getChangesets().clear();
		incoming.getChangesets().clear();
		super.dispose();
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		// Override inherited method to reconcile sub-trees
		IPath[] removed = event.getRemovals();
		IDiff[] added = event.getAdditions();
		IDiff[] changed = event.getChanges();
		IPath root = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		Utils.asyncExec(new Runnable() {
			public void run() {
				ensureRootsAdded();
			}
		}, getTreeViewer());

		// Only adjust the set of the rest. The others will be handled by the collectors

		try {
			// XXX the code below should go away ASAP and be replaced with simply listening
			// on STATUS_CACHE changes.
			uncommittedSet.beginInput();
			for (IPath path : removed) {
				uncommittedSet.remove(ResourceUtils.getFileHandle(root.append(path)));
			}
			for (IDiff diff : added) {
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if(resource instanceof IFile && !STATUS_CACHE.isClean(resource)
						&& STATUS_CACHE.isSupervised(resource)) {
					uncommittedSet.add((IFile) resource);
				}
			}
			for (IDiff diff : changed) {
				// Only add the diff if it is already contained in the free set
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if (resource instanceof IFile) {
					if (STATUS_CACHE.isClean(resource) || !resource.exists()) {
						uncommittedSet.remove(resource);
					} else {
						uncommittedSet.add((IFile) resource);
					}
				}
			}
		} finally {
			uncommittedSet.endInput(monitor);
			// XXX end of dirty code
		}

		if (csCollector != null) {
			csCollector.handleChange(event);
		}
	}

	@Override
	protected void updateLabels(ISynchronizationContext context, IPath[] paths) {
		super.updateLabels(context, paths);
		ChangeSet[] sets = getSetsShowingPropogatedStateFrom(paths);
		if (sets.length > 0) {
			getTreeViewer().update(sets, null);
		}
	}


	private ChangeSet[] getSetsShowingPropogatedStateFrom(IPath[] paths) {
		Set<ChangeSet> result = new HashSet<ChangeSet>();
		for (IPath path : paths) {
			Set<ChangeSet> sets = getSetsShowingPropogatedStateFrom(path);
			result.addAll(sets);
		}
		return result.toArray(new ChangeSet[result.size()]);
	}

	private Set<ChangeSet> getSetsShowingPropogatedStateFrom(IPath path) {
		Set<ChangeSet> result = new HashSet<ChangeSet>();
		Collection<ChangeSet> allSets = getAllSets();
		for (ChangeSet set : allSets) {
			if (set.contains(path)) {
				result.add(set);
			}
		}
		return result;
	}

	private ChangeSetCapability getChangeSetCapability() {
		ISynchronizeParticipant participant = getConfiguration().getParticipant();
		if (participant instanceof IChangeSetProvider) {
			IChangeSetProvider provider = (IChangeSetProvider) participant;
			return provider.getChangeSetCapability();
		}
		return null;
	}

	boolean isVisibleInViewer(ChangeSet set) {
		TreeItem[] children = getTreeViewer().getTree().getItems();
		for (TreeItem control : children) {
			Object data = control.getData();
			if (set.equals(data)) {
				return true;
			}
		}
		return false;
	}

	private TreeViewer getTreeViewer() {
		return ((TreeViewer)getViewer());
	}

}
