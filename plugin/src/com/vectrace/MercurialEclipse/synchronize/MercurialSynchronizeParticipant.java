/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Andrei Loskutov              - bug fixes
 *     Martin Olsen (Schantz)  -  Synchronization of Multiple repositories
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipant;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.internal.ui.synchronize.IRefreshSubscriberListener;
import org.eclipse.team.internal.ui.synchronize.RefreshModelParticipantJob;
import org.eclipse.team.internal.ui.synchronize.RefreshParticipantJob;
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
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.synchronize.actions.MercurialSynchronizePageActionGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetCapability;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetModelProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * TODO why did we choose the {@link ModelSynchronizeParticipant} as a parent class?
 * Why not {@link SubscriberParticipant}???
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class MercurialSynchronizeParticipant extends ModelSynchronizeParticipant
	implements IChangeSetProvider, ISynchronizationScopeParticipant {

	private static final String REPOSITORY_LOCATION = "REPOSITORY_LOCATION"; //$NON-NLS-1$
	private static final String PROJECTS = "PROJECTS";

	private String secondaryId;
	private Set<IHgRepositoryLocation> repositoryLocationSet;
	private Set<IProject> restoredProjects;
	private HgChangeSetCapability changeSetCapability;

	public MercurialSynchronizeParticipant() {
		super();
	}

	public MercurialSynchronizeParticipant(HgSubscriberMergeContext ctx, Set<IHgRepositoryLocation> repositoryLocation, RepositorySynchronizationScope scope) {
		super(ctx);
		this.repositoryLocationSet = repositoryLocation;
		secondaryId = computeSecondaryId(scope, repositoryLocation);
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI.getSynchronizeManager().getParticipantDescriptor(getId());
			setInitializationData(descriptor);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public static String computeSecondaryId(RepositorySynchronizationScope scope, Set<IHgRepositoryLocation> repos) {
		IProject[] projects = scope.getProjects();
		StringBuilder sb = new StringBuilder();
		if(projects.length > 0){
			sb.append("[");
			for (IHgRepositoryLocation repo : repos) {
				sb.append(repo.getLocation()).append(',');
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("]");
		}
		return sb.toString();
	}

	@Override
	public void init(String secId, IMemento memento) throws PartInitException {
		secondaryId = secId;

		IMemento myMemento = memento.getChild(MercurialSynchronizeParticipant.class.getName());
		String uri = myMemento.getString(REPOSITORY_LOCATION);

		try {
			repositoryLocationSet = new HashSet<IHgRepositoryLocation>();
			String[] split = uri.split(",");
			for (String url : split) {
				repositoryLocationSet.add(MercurialEclipsePlugin.getRepoManager().getRepoLocation(url));
			}
		} catch (HgException e) {
			throw new PartInitException(e.getLocalizedMessage(), e);
		}
		restoreScope(myMemento);
		super.init(secondaryId, memento);
	}

	private void restoreScope(IMemento memento) {
		String encodedProjects = memento.getString(PROJECTS);
		if(encodedProjects == null){
			return;
		}
		String[] projectNames = encodedProjects.split(",");
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Set<IProject> repoProjects = getRepoProjects();
		restoredProjects = new HashSet<IProject>();
		for (String pName : projectNames) {
			if(pName.length() == 0){
				continue;
			}
			IProject project = root.getProject(pName);
			if(project != null && (repoProjects.contains(project) || (!project.isOpen() && project.exists()))){
				restoredProjects.add(project);
			}
		}

		boolean addRoot = MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getBoolean(MercurialPreferenceConstants.PREF_SYNC_ALL_PROJECTS_IN_REPO);
		if(restoredProjects.isEmpty() && addRoot) {
			Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(repoProjects);
			Set<HgRoot> roots = byRoot.keySet();
			for (HgRoot hgRoot : roots) {
				restoredProjects.add(hgRoot.getResource());
			}
		}
	}

	private Set<IProject> getRepoProjects() {
		Set<IProject> repoProjects = new HashSet<IProject>();
		for(IHgRepositoryLocation repos : repositoryLocationSet) {
			 repoProjects.addAll(MercurialEclipsePlugin.getRepoManager().getAllRepoLocationProjects(repos));
		}
		return repoProjects;
	}

	@Override
	public void saveState(IMemento memento) {
		IMemento myMemento = memento.createChild(MercurialSynchronizeParticipant.class.getName());
		myMemento.putString(REPOSITORY_LOCATION, repositoryLocationToString());
		saveCurrentScope(myMemento);
		super.saveState(memento);
	}

	private void saveCurrentScope(IMemento memento){
		IProject[] projects = getContext().getScope().getProjects();
		Set<IProject> repoProjects = getRepoProjects();
		StringBuilder sb = new StringBuilder();
		for (IProject project : projects) {
			if(repoProjects.contains(project) || !project.isOpen()) {
				sb.append(project.getName()).append(",");
			}
		}
		memento.putString(PROJECTS, sb.toString());
	}

	@Override
	protected MergeContext restoreContext(ISynchronizationScopeManager manager) throws CoreException {
		Set<IProject> repoProjects = null;
		if (restoredProjects != null && !restoredProjects.isEmpty()) {
			repoProjects = restoredProjects;
		} else {
			repoProjects = new TreeSet<IProject>();
			if (repositoryLocationSet != null) {
				for (IHgRepositoryLocation repo : repositoryLocationSet) {
					Set<IProject> locationProjects = MercurialEclipsePlugin.getRepoManager().getAllRepoLocationProjects(repo);
					repoProjects.addAll(locationProjects);
				}
			}
		}
		RepositorySynchronizationScope scope = new RepositorySynchronizationScope(repositoryLocationSet, repoProjects.toArray(new IProject[0]));
		MercurialSynchronizeSubscriber subscriber = new MercurialSynchronizeSubscriber(scope);
		HgSubscriberScopeManager manager2 = new HgSubscriberScopeManager(scope.getMappings(), subscriber);
		subscriber.setParticipant(this);
		return new HgSubscriberMergeContext(subscriber, manager2);
	}

	@Override
	protected void initializeContext(SynchronizationContext context) {
		if(context != null) {
			super.initializeContext(context);
		}
	}

	@Override
	public void dispose() {
		if(getContext() != null) {
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
	public Set<IHgRepositoryLocation> getRepositoryLocation() {
		return repositoryLocationSet;
	}

	public IHgRepositoryLocation getRepositoryLocation(HgRoot root) {
		// TODO: this is wrong - Eg repos could sync with each other or share remote. why is this not known already?
		Set<IHgRepositoryLocation> locs = new HashSet<IHgRepositoryLocation>(MercurialEclipsePlugin.getRepoManager().getAllRepoLocations(root));

		for (Iterator<IHgRepositoryLocation> it = locs.iterator(); it.hasNext();) {
			IHgRepositoryLocation repoLoc = it.next();

			for (Iterator<? extends IHgRepositoryLocation> it2 = repositoryLocationSet.iterator(); it2.hasNext();) {
				IHgRepositoryLocation scopeLoc = it2.next();

				if (scopeLoc.compareTo(repoLoc) == 0) {
					return scopeLoc;
				}
			}
		}

		return null;
	}

	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup() {
		// allows us to contribute our own actions to the synchronize view via java code
		return new MercurialSynchronizePageActionGroup();
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		if(!isMergingEnabled()) {
			// add our action group in any case
			configuration.addActionContribution(createMergeActionGroup());
		}
		// Set changesets mode as default
		configuration.setProperty(ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER,	HgChangeSetModelProvider.ID);
	}

	@Override
	public boolean hasCompareInputFor(Object object) {
		if(object instanceof IFile || object instanceof FileFromChangeSet){
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
//		super.run(part);
		IRefreshSubscriberListener listener = new MercurialRefreshUserNotificationPolicy(this);

		ResourceMapping[] mappings = getContext().getScope().getMappings();
		String jobName = null;
		String taskName = null;
		jobName = getShortTaskName();
		taskName = getLongTaskName(mappings);
		Job.getJobManager().cancel(this);
		RefreshParticipantJob job = new RefreshModelParticipantJob(this, jobName, taskName, mappings, listener);
		job.setUser(true);
		//job.setProperty(IProgressConstants2.SHOW_IN_TASKBAR_ICON_PROPERTY, Boolean.TRUE);
		Utils.schedule(job, null);

		// Remember the last participant synchronized
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT, getId());
		TeamUIPlugin.getPlugin().getPreferenceStore().setValue(IPreferenceIds.SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID, getSecondaryId());
	}


	public ChangeSetCapability getChangeSetCapability() {
		if(changeSetCapability == null) {
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

	private String repositoryLocationToString() {
		StringBuilder sb = new StringBuilder();
		IHgRepositoryLocation[] array = repositoryLocationSet.toArray(new IHgRepositoryLocation[]{});
		for(int i=0; i < array.length; i++) {
			sb.append(array[i].toString());
			if(i >= 0 && i != array.length-1) {
				sb.append(",");
			}
		}
		return sb.toString().trim();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MercurialSynchronizeParticipant [");
		if (repositoryLocationSet != null) {
			builder.append("repositoryLocation=");
			builder.append(repositoryLocationSet);
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

	public Set<IProject> getRestoredProjects() {
		return restoredProjects;
	}
}
