/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Ilya Ivanov (Intland) - modifications
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipant;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.compare.MercurialCompareInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.ChangeSet.ParentChangeSet;
import com.vectrace.MercurialEclipse.synchronize.actions.MercurialSynchronizePageActionGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetCapability;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetModelProvider;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.NullRevision;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;

/**
 * TODO why did we choose the {@link ModelSynchronizeParticipant} as a parent class? Why not
 * {@link SubscriberParticipant}???
 *
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class MercurialSynchronizeParticipant extends ModelSynchronizeParticipant implements
		IChangeSetProvider, ISynchronizationScopeParticipant {

	private static final String REPOSITORY_LOCATION = "REPOSITORY_LOCATION"; //$NON-NLS-1$
	private static final String PROJECTS = "PROJECTS";

	private String secondaryId;
	private IHgRepositoryLocation repositoryLocation;
	private Set<IProject> restoredProjects;
	private HgChangeSetCapability changeSetCapability;

	public MercurialSynchronizeParticipant() {
		super();
	}

	public MercurialSynchronizeParticipant(HgSubscriberMergeContext ctx,
			IHgRepositoryLocation repositoryLocation, RepositorySynchronizationScope scope) {
		super(ctx);
		this.repositoryLocation = repositoryLocation;
		secondaryId = computeSecondaryId(scope, repositoryLocation);
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager()
					.getParticipantDescriptor(getId());
			setInitializationData(descriptor);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	private String computeSecondaryId(RepositorySynchronizationScope scope,
			IHgRepositoryLocation repo) {
		IProject[] projects = scope.getProjects();
		StringBuilder sb = new StringBuilder();
		if (projects.length > 0) {
			sb.append("[");
			for (IProject project : projects) {
				sb.append(project.getName()).append(',');
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("] ");
		}
		sb.append(repo.getLocation());
		if (sb.charAt(sb.length() - 1) == '/') {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	@Override
	public void init(String secId, IMemento memento) throws PartInitException {
		secondaryId = secId;

		IMemento myMemento = memento.getChild(MercurialSynchronizeParticipant.class.getName());
		String uri = myMemento.getString(REPOSITORY_LOCATION);

		try {
			repositoryLocation = MercurialEclipsePlugin.getRepoManager().getRepoLocation(uri);
		} catch (HgException e) {
			throw new PartInitException(e.getLocalizedMessage(), e);
		}
		restoreScope(myMemento);
		super.init(secondaryId, memento);
	}

	private void restoreScope(IMemento memento) {
		String encodedProjects = memento.getString(PROJECTS);
		if (encodedProjects == null) {
			return;
		}
		String[] projectNames = encodedProjects.split(",");
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Set<IProject> repoProjects = MercurialEclipsePlugin.getRepoManager()
				.getAllRepoLocationProjects(repositoryLocation);
		restoredProjects = new HashSet<IProject>();
		for (String pName : projectNames) {
			if (pName.length() == 0) {
				continue;
			}
			IProject project = root.getProject(pName);
			if (project != null && (repoProjects.contains(project) || !project.isOpen())) {
				restoredProjects.add(project);
			}
		}
	}

	@Override
	public void saveState(IMemento memento) {
		IMemento myMemento = memento.createChild(MercurialSynchronizeParticipant.class.getName());
		myMemento.putString(REPOSITORY_LOCATION, repositoryLocation.getLocation());
		saveCurrentScope(myMemento);
		super.saveState(memento);
	}

	private void saveCurrentScope(IMemento memento) {
		IProject[] projects = getContext().getScope().getProjects();
		Set<IProject> repoProjects = MercurialEclipsePlugin.getRepoManager()
				.getAllRepoLocationProjects(repositoryLocation);
		StringBuilder sb = new StringBuilder();
		for (IProject project : projects) {
			if (repoProjects.contains(project) || !project.isOpen()) {
				sb.append(project.getName()).append(",");
			}
		}
		memento.putString(PROJECTS, sb.toString());
	}

	@Override
	protected MergeContext restoreContext(ISynchronizationScopeManager manager)
			throws CoreException {
		Set<IProject> repoProjects;
		if (restoredProjects != null && !restoredProjects.isEmpty()) {
			repoProjects = restoredProjects;
		} else {
			repoProjects = MercurialEclipsePlugin.getRepoManager().getAllRepoLocationProjects(
					repositoryLocation);
		}
		RepositorySynchronizationScope scope = new RepositorySynchronizationScope(
				repositoryLocation, repoProjects.toArray(new IProject[0]));
		MercurialSynchronizeSubscriber subscriber = new MercurialSynchronizeSubscriber(scope);
		HgSubscriberScopeManager manager2 = new HgSubscriberScopeManager(scope.getMappings(),
				subscriber);
		subscriber.setParticipant(this);
		return new HgSubscriberMergeContext(subscriber, manager2);
	}

	@Override
	protected void initializeContext(SynchronizationContext context) {
		if (context != null) {
			super.initializeContext(context);
		}
	}

	@Override
	public void dispose() {
		if (getContext() != null) {
			super.dispose();
		}
	}

	@Override
	public String getId() {
		return getClass().getName();
	}

	@Override
	public String getSecondaryId() {
		return secondaryId;
	}

	@Override
	public String getName() {
		return secondaryId;
	}

	/**
	 * @return the repositoryLocation
	 */
	public IHgRepositoryLocation getRepositoryLocation() {
		return repositoryLocation;
	}

	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		// allows us to contribute our own actions to the synchronize view via java code
		return new MercurialSynchronizePageActionGroup();
	}

	@Override
	public ICompareInput asCompareInput(Object object) {
		if (object instanceof FileFromChangeSet) {
			FileFromChangeSet fcs = (FileFromChangeSet) object;

			final IFile file = fcs.getFile();
			if (file == null) {
				// TODO this can happen, if the file was modified but is OUTSIDE Eclipse workspace
				MessageDialog.openInformation(null, "Compare",
						"Diff for files external to Eclipse workspace is not supported yet!");
				return null;
			}

			ChangeSet cs = fcs.getChangeset();

			if (cs instanceof ParentChangeSet) {
				return null;
			} else if (cs instanceof WorkingChangeSet) {
				// local workspace version
				ResourceNode thisNode = new ResourceNode(file);
				// mercurial version
				RevisionNode parentNode = new RevisionNode(new MercurialRevisionStorage(file));

				return new MercurialCompareInput(null, thisNode, parentNode, getContext(), fcs);
			}

			if (cs.getDirection() == Direction.OUTGOING) {
				String[] parents = cs.getParents();
				MercurialRevisionStorage thisRev = new MercurialRevisionStorage(file, cs.getChangeset());
				MercurialRevisionStorage parentRev;
				if (parents.length == 0) {
					// TODO for some reason, we do not always have right parent info in the changesets
					// if we are on the different branch then the changeset. So simply enforce the parents resolving
					try {
						parents = HgParentClient.getParentNodeIds(file, cs);
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
				if (cs.getRevision().getRevision() == 0 || parents.length == 0) {
					parentRev = new NullRevision(file, cs);
				} else {
					parentRev = new MercurialRevisionStorage(file, parents[0]);
				}

				if (parents.length != 1) {
					return null;
				}

				RevisionNode thisNode = new RevisionNode(thisRev);
				RevisionNode parentNode = new RevisionNode(parentRev);

				return new MercurialCompareInput(null, thisNode, parentNode, getContext(), fcs);
			} else if (cs.getDirection() == Direction.INCOMING) {
				MercurialRevisionStorage thisRev = new MercurialRevisionStorage(
						file, cs.getChangesetIndex(), cs.getChangeset(), cs);
				MercurialRevisionStorage parentRev;
				String[] parents = cs.getParents();
				if (cs.getRevision().getRevision() == 0 || parents.length == 0) {
					parentRev = new NullRevision(file, cs);
				} else {
					ChangeSet parentCs = null;
					String parentId = parents[0];

					Set<ChangeSet> changesets;
					try {
						changesets = IncomingChangesetCache.getInstance().getChangeSets(file,
								getRepositoryLocation(), null);
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
						return null;
					}

					for (ChangeSet cset : changesets) {
						if (parentId.endsWith(cset.getChangeset())
								&& parentId.startsWith("" + cset.getChangesetIndex())) {
							parentCs = cset;
							break;
						}
					}
					if (parentCs == null) {
						parentCs = new ParentChangeSet(parentId, cs);
					}
					parentRev = new MercurialRevisionStorage(file, parentCs.getChangesetIndex(),
							parentCs.getChangeset(), parentCs);

				}
				RevisionNode thisNode = new RevisionNode(thisRev);
				RevisionNode parentNode = new RevisionNode(parentRev);

				return new MercurialCompareInput(null, thisNode, parentNode, getContext(), fcs);
			}
		}
		return super.asCompareInput(object);
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		if (!isMergingEnabled()) {
			// add our action group in any case
			configuration.addActionContribution(createMergeActionGroup());
		}
		// Set changesets mode as default
		configuration.setProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER,
				HgChangeSetModelProvider.ID);
	}

	@Override
	public boolean hasCompareInputFor(Object object) {
		if (object instanceof IFile || object instanceof FileFromChangeSet) {
			// always allow "Open in Compare Editor" menu to be shown
			return true;
		}
		return super.hasCompareInputFor(object);
	}

	@Override
	public boolean isMergingEnabled() {
		// do not sow Eclipse default "merge" action, which only breaks "native" hg merge
		return false;
	}

	@Override
	protected boolean isViewerContributionsSupported() {
		// allows us to contribute our own actions to the synchronize view via plugin.xml
		return true;
	}

	@Override
	public boolean doesSupportSynchronize() {
		return true;
	}

	@Override
	public void run(IWorkbenchPart part) {
		super.run(part);
	}

	public ChangeSetCapability getChangeSetCapability() {
		if (changeSetCapability == null) {
			changeSetCapability = new HgChangeSetCapability();
		}
		return changeSetCapability;
	}

	@Override
	public ModelProvider[] getEnabledModelProviders() {
		ModelProvider[] providers = getContext().getScope().getModelProviders();

		return providers;
	}

	public ResourceMapping[] handleContextChange(ISynchronizationScope scope,
			IResource[] resources, IProject[] projects) {
		return new ResourceMapping[0];
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MercurialSynchronizeParticipant [");
		if (repositoryLocation != null) {
			builder.append("repositoryLocation=");
			builder.append(repositoryLocation);
			builder.append(", ");
		}
		if (restoredProjects != null) {
			builder.append("restoredProjects=");
			builder.append(restoredProjects);
			builder.append(", ");
		}
		if (secondaryId != null) {
			builder.append("secondaryId=");
			builder.append(secondaryId);
		}
		builder.append("]");
		return builder.toString();
	}
}
