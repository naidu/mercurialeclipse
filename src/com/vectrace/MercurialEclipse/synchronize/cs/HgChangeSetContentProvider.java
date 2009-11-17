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
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.subscribers.ActiveChangeSetManager;
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
						// TODO: Hide/Show uncommited?
						/* if (!isVisibleInMode(uncommittedSet)) {
							treeViewer.remove(uncommittedSet);
						} else if (!isVisibleInViewer(uncommittedSet)) {
							Object input1 = treeViewer.getInput();
							treeViewer.add(input1, uncommittedSet);
						} else */{
							treeViewer.refresh(uncommittedSet);
						}
					}
				}, getTreeViewer());
			}
		}
	}

	private final class CollectorListener implements IChangeSetChangeListener, BatchingChangeSetManager.IChangeSetCollectorChangeListener {

		public void setAdded(final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			if (isVisibleInMode(set)) {
				Utils.syncExec(new Runnable() {
					public void run() {
						if(((ChangeSet)set).getDirection() == Direction.INCOMING){
							incoming.getChangesets().add((ChangeSet) set);
							getTreeViewer().refresh(incoming, true);
						} else {
							outgoing.getChangesets().add((ChangeSet) set);
							getTreeViewer().refresh(outgoing, true);
						}
					}
				}, getTreeViewer());
			}
		}

		public void defaultSetChanged(final org.eclipse.team.internal.core.subscribers.ChangeSet previousDefault,
				final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			if (isVisibleInMode(set) || isVisibleInMode(previousDefault)) {
				Utils.asyncExec(new Runnable() {
					public void run() {
						TreeViewer treeViewer = getTreeViewer();
						if (set == null) {
							// unset default changeset
							treeViewer.update(previousDefault, null);
						} else if (previousDefault != null) {
							treeViewer.update(new Object[] {previousDefault, set}, null);
						} else {
							// when called for the first time previous default change set is null
							treeViewer.update(set, null);
						}
					}
				}, getTreeViewer());
			}
		}

		public void setRemoved(final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			if (isVisibleInMode(set)) {
				Utils.syncExec(new Runnable() {
					public void run() {
						if(((ChangeSet)set).getDirection() == Direction.INCOMING){
							incoming.getChangesets().remove(set);
							getTreeViewer().refresh(incoming, true);
						} else {
							outgoing.getChangesets().remove(set);
							getTreeViewer().refresh(outgoing, true);
						}
					}
				}, getTreeViewer());
			}
		}

		public void nameChanged(final org.eclipse.team.internal.core.subscribers.ChangeSet set) {
			if (isVisibleInMode(set)) {
				Utils.asyncExec(new Runnable() {
					public void run() {
						getTreeViewer().update(set, null);
					}
				}, getTreeViewer());
			}
		}

		public void resourcesChanged(final org.eclipse.team.internal.core.subscribers.ChangeSet set, final IPath[] paths) {
			if(!(set instanceof WorkingChangeSet)){
				return;
			}
			if (isVisibleInMode(set)) {
				Utils.syncExec(new Runnable() {
					public void run() {
						TreeViewer treeViewer = getTreeViewer();
						if (hasChildrenInContext((ChangeSet) set)) {
							treeViewer.refresh(set, true);
						}
					}
				}, getTreeViewer());
			}
			handleSetChange((WorkingChangeSet) set, paths);
		}

		private void handleSetChange(final WorkingChangeSet set, final IPath[] paths) {
			try {
				uncommittedSet.beginInput();
				IPath root = ResourcesPlugin.getWorkspace().getRoot().getLocation();
				for (IPath path : paths) {
					path = root.append(path);
					boolean isContained = set.contains(path);
					if (isContained) {
						uncommittedSet.remove(ResourceUtils.getFileHandle(path));
					} else {
						IDiff diff = getContext().getDiffTree().getDiff(path);
						if (diff != null && !isContainedInSet(diff)) {
							IResource resource = ResourceDiffTree.getResourceFor(diff);
							if(resource instanceof IFile) {
								uncommittedSet.add((IFile) resource);
							}
						}
					}
				}
			} finally {
				uncommittedSet.endInput(null);
			}
		}

		private void addSets(TreeViewer treeViewer, ChangeSet[] sets){
			boolean in = false, out = false;
			for (ChangeSet set : sets) {
				if(set.getDirection() == Direction.INCOMING){
					incoming.getChangesets().add(set);
					in = true;
				} else {
					outgoing.getChangesets().add(set);
					out = true;
				}
			}
			if(in){
				treeViewer.refresh(incoming, true);
			}
			if(out){
				treeViewer.refresh(outgoing, true);
			}
		}

		private void removeSets(TreeViewer treeViewer, ChangeSet[] sets){
			boolean in = false, out = false;
			for (ChangeSet set : sets) {
				if(set.getDirection() == Direction.INCOMING){
					incoming.getChangesets().remove(set);
					in = true;
				} else {
					outgoing.getChangesets().remove(set);
					out = true;
				}
			}
			if(in){
				treeViewer.refresh(incoming, true);
			}
			if(out){
				treeViewer.refresh(outgoing, true);
			}
		}

		public void changeSetChanges(final CollectorChangeEvent event, IProgressMonitor monitor) {
			Set<ChangeSet> addedSets = getChanges(event.getAddedSets());
			final ChangeSet[] visibleAddedSets = getVisibleSets(addedSets);
			Set<ChangeSet> removedSets = getChanges(event.getRemovedSets());
			final ChangeSet[] visibleRemovedSets = getVisibleSets(removedSets);
			Set<ChangeSet> changedSets = getChanges(event.getChangedSets());
			final ChangeSet[] visibleChangedSets = getVisibleSets(changedSets);
			if (visibleAddedSets.length > 0 || visibleRemovedSets.length > 0 || visibleChangedSets.length > 0) {
				Utils.syncExec(new Runnable() {
					public void run() {
						TreeViewer treeViewer = getTreeViewer();
						try {
							treeViewer.getControl().setRedraw(false);
							if (visibleAddedSets.length > 0) {
								addSets(treeViewer, visibleAddedSets);
							}
							if (visibleRemovedSets.length > 0) {
								removeSets(treeViewer, visibleRemovedSets);
							}
							for (int i = 0; i < visibleChangedSets.length; i++) {
								ChangeSet set = visibleChangedSets[i];
								treeViewer.refresh(set, true);
							}
						} finally {
							treeViewer.getControl().setRedraw(true);
						}
					}
				}, getTreeViewer());
			}
			try {
				uncommittedSet.beginInput();

				for (ChangeSet set: changedSets) {
					IPath[] paths = event.getChangesFor(set);
					if (set instanceof WorkingChangeSet && event.getSource().contains(set)) {
						handleSetChange((WorkingChangeSet) set, paths);
					} else {
						try {
							uncommittedSet.beginInput();
							for (int j = 0; j < paths.length; j++) {
								IPath path = paths[j];
								IDiff diff = getContext().getDiffTree().getDiff(path);
								if (diff != null && !isContainedInSet(diff)) {
									IResource resource = ResourceDiffTree.getResourceFor(diff);
									if(resource instanceof IFile) {
										uncommittedSet.add((IFile) resource);
									}
								}
							}
						} finally {
							uncommittedSet.endInput(null);
						}
					}
				}
			} finally {
				uncommittedSet.endInput(monitor);
			}
		}


		private Set<ChangeSet> getChanges(
				org.eclipse.team.internal.core.subscribers.ChangeSet[] sets) {
			Set<ChangeSet> csets = new HashSet<ChangeSet>();
			for (Object cset : sets) {
				csets.add((ChangeSet) cset);
			}
			return csets;
		}

		private ChangeSet[] getVisibleSets(Set<ChangeSet> addedSets) {
			Set<ChangeSet> result = new HashSet<ChangeSet>();
			for (ChangeSet set : addedSets) {
				if (isVisibleInMode(set)) {
					result.add(set);
				}
			}
			return result.toArray(new ChangeSet[0]);
		}
	}

	private final WorkingChangeSet uncommittedSet;
	private boolean firstDiffChange;
	private HgChangesetsCollector checkedInCollector;
	private boolean collectorInitialized;

	/**
	 * Listener that reacts to changes made to the active change set collector
	 */
	private final IChangeSetChangeListener collectorListener;

	private final IPropertyChangeListener uncommittedSetListener;
	private final ChangesetGroup incoming;
	private final ChangesetGroup outgoing;

	public HgChangeSetContentProvider() {
		super();
		firstDiffChange = true;
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

	private boolean isVisibleInMode(ChangeSet set) {
		final Object input = getViewer().getInput();
		if (input instanceof HgChangeSetModelProvider) {
			if (set.getDirection() == Direction.INCOMING) {
				return isIncomingVisible();
			}
			if (set.getDirection() == Direction.OUTGOING || set.getDirection() == Direction.LOCAL) {
				return isOutgoingVisible();
			}
		}
		return false;
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
		if (parent == getModelProvider()) {
			return getRootElements();
		}
		return super.getElements(parent);
	}

	private Object[] getRootElements() {
		if (!collectorInitialized) {
			initializeChangeSets(getChangeSetCapability());
			collectorInitialized = true;
		}
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
			if(showOutgoing && (direction == Direction.OUTGOING || direction == Direction.LOCAL)){
				outgoing.getChangesets().add(set);
			}
			if(showIncoming && direction == Direction.INCOMING){
				incoming.getChangesets().add(set);
			}
		}

		return new Object[]{uncommittedSet, outgoing, incoming};
	}

	private void addAllUnassignedToUnassignedSet() {
		IResourceDiffTree allChanges = getContext().getDiffTree();
		final List<IDiff> diffs = new ArrayList<IDiff>();
		allChanges.accept(ResourcesPlugin.getWorkspace().getRoot().getFullPath(), new IDiffVisitor() {
			public boolean visit(IDiff diff) {
				if (!isContainedInSet(diff)) {
					diffs.add(diff);
				}
				return true;
			}
		}, IResource.DEPTH_INFINITE);

		for (IDiff diff : diffs) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if(resource instanceof IFile) {
				uncommittedSet.add((IFile) resource);
			}
		}
	}

	/**
	 * Return whether the given diff is contained in a set other than
	 * the unassigned set.
	 * @param diff the diff
	 * @return whether the given diff is contained in a set other than
	 * the unassigned set
	 */
	private boolean isContainedInSet(IDiff diff) {
		Collection<ChangeSet> sets = getAllSets();
		for (ChangeSet set : sets) {
			if (set.contains(ResourceDiffTree.getResourceFor(diff))) {
				return true;
			}
		}
		return false;
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
		Object parent = parentPath.getFirstSegment();
		if (!isVisibleInMode(parent)) {
			return new Object[0];
		}
		Object child = parentPath.getLastSegment();
		if (child instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) child;
			return group.getChangesets().toArray();
		} else if (child instanceof ChangeSet) {
			ChangeSet set = (ChangeSet) child;
			return set.getResources();
		} else if(child instanceof IFile){
			return new Object[0];
		}
		return new Object[0];
	}

	private boolean isVisibleInMode(Object first) {
		if (first instanceof ChangeSet) {
			ChangeSet cs = (ChangeSet) first;
			int mode = getConfiguration().getMode();
			switch (mode) {
			case ISynchronizePageConfiguration.BOTH_MODE:
				return true;
			case ISynchronizePageConfiguration.CONFLICTING_MODE:
				return containsConflicts(cs);
			case ISynchronizePageConfiguration.INCOMING_MODE:
				return  hasIncomingChanges(cs);
			case ISynchronizePageConfiguration.OUTGOING_MODE:
				return hasConflicts(cs) || (isUnassignedSet(cs) && hasOutgoingChanges(cs));
			default:
				break;
			}
		}
		return true;
	}

	private boolean hasIncomingChanges(ChangeSet cs) {
		// XXX
		return cs.getDirection() == Direction.INCOMING;
	}

	private boolean hasOutgoingChanges(ChangeSet cs) {
		// XXX
		return cs.getDirection() == Direction.OUTGOING || cs.getDirection() == Direction.LOCAL;
	}

	private boolean isUnassignedSet(ChangeSet cs) {
		return cs == uncommittedSet;
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
				if (isOutgoingVisible()
						&& (direction == Direction.OUTGOING || direction == Direction.LOCAL)) {
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
		// XXX Andrei: filtering for project?
//		IResource[] resources = set.getResources();
//		for (int i = 0; i < resources.length; i++) {
//			IResource resource = resources[i];
//			if (getContext().getDiffTree().getDiff(resource) != null) {
//				return true;
//			}
//		}
		return true;
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
		ChangeSetCapability csc = getChangeSetCapability();
		if (csc.supportsActiveChangeSets()) {
			ActiveChangeSetManager collector = csc.getActiveChangeSetManager();
			org.eclipse.team.internal.core.subscribers.ChangeSet[] sets = collector.getSets();
			for (org.eclipse.team.internal.core.subscribers.ChangeSet set : sets) {
				result.add((ChangeSet) set);
			}
		}
		if (checkedInCollector != null) {
			org.eclipse.team.internal.core.subscribers.ChangeSet[] sets = checkedInCollector.getSets();
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
		ChangeSetCapability csc = getChangeSetCapability();
		if (csc.supportsActiveChangeSets()) {
			ActiveChangeSetManager collector = csc.getActiveChangeSetManager();
			collector.addListener(collectorListener);
		}
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
			checkedInCollector = ((HgChangeSetCapability)csc).createSyncInfoSetChangeSetCollector(getConfiguration());
			checkedInCollector.getSets();
			checkedInCollector.addListener(collectorListener);
			addAllUnassignedToUnassignedSet();
			uncommittedSet.addListener(uncommittedSetListener);
			// XXX Andrei: disabled temporarily
//			checkedInCollector.add(((ResourceDiffTree)getContext().getDiffTree()).getDiffs());
		}
	}

	@Override
	public void dispose() {
		ChangeSetCapability csc = getChangeSetCapability();
		if (csc.supportsActiveChangeSets()) {
			csc.getActiveChangeSetManager().removeListener(collectorListener);
		}
		if (checkedInCollector != null) {
			checkedInCollector.removeListener(collectorListener);
			checkedInCollector.dispose();
		}
		uncommittedSet.removeListener(uncommittedSetListener);
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
		// Only adjust the set of the rest. The others will be handled by the collectors
		try {
			uncommittedSet.beginInput();
			for (int i = 0; i < removed.length; i++) {
				IPath path = removed[i];
				uncommittedSet.remove(ResourceUtils.getFileHandle(root.append(path)));
			}
			for (IDiff diff : added) {
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if(resource instanceof IFile && !STATUS_CACHE.isClean(resource)) {
					uncommittedSet.add((IFile) resource);
				}
			}
			for (IDiff diff : changed) {
				// Only add the diff if it is already contained in the free set
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if ((resource instanceof IFile) && !STATUS_CACHE.isClean(resource)) {
					uncommittedSet.add((IFile) resource);
				}
			}
		} finally {
			uncommittedSet.endInput(monitor);
		}
		if (checkedInCollector != null) {
			checkedInCollector.handleChange(event);
		}
		if (firstDiffChange) {
			// One the first diff event, refresh the viewer to ensure outgoing change sets appear
			firstDiffChange = false;
			Utils.asyncExec(new Runnable() {
				public void run() {
					getTreeViewer().refresh(uncommittedSet, true);
				}
			}, getTreeViewer());
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
