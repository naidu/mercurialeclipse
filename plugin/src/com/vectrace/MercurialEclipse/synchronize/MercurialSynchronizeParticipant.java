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

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope.RepositoryLocationMap;
import com.vectrace.MercurialEclipse.synchronize.actions.MercurialSynchronizePageActionGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetCapability;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetModelProvider;

/**
 * TODO why did we choose the {@link ModelSynchronizeParticipant} as a parent class?
 * Why not {@link SubscriberParticipant}???
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class MercurialSynchronizeParticipant extends ModelSynchronizeParticipant
	implements IChangeSetProvider, ISynchronizationScopeParticipant {

	private String secondaryId;
	private RepositoryLocationMap repositoryLocations;
	private HgChangeSetCapability changeSetCapability;

	public MercurialSynchronizeParticipant() {
		super();
	}

	public MercurialSynchronizeParticipant(HgSubscriberMergeContext ctx,
			RepositoryLocationMap repositoryLocation, RepositorySynchronizationScope scope) {
		super(ctx);
		this.repositoryLocations = repositoryLocation;
		secondaryId = computeSecondaryId(scope, repositoryLocation);
		try {
			ISynchronizeParticipantDescriptor descriptor = TeamUI
				.getSynchronizeManager().getParticipantDescriptor(getId());
			setInitializationData(descriptor);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	private static String computeSecondaryId(RepositorySynchronizationScope scope, RepositoryLocationMap repos) {
		IProject[] projects = scope.getProjects();
		StringBuilder sb = new StringBuilder();
		if(projects.length > 0){
			sb.append("[");
			for (IHgRepositoryLocation repo : repos.getLocations()) {
				sb.append(repo.getLocation()).append(',');
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.append("] ");
		}
		return sb.toString();
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant#init(java.lang.String, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(String secId, IMemento memento) throws PartInitException {
		secondaryId = secId;

		IMemento myMemento = memento.getChild(MercurialSynchronizeParticipant.class.getName());

		try {
			repositoryLocations = new RepositoryLocationMap(myMemento);
		} catch (HgException e) {
			throw new PartInitException(e.getLocalizedMessage(), e);
		} catch (IOException e) {
			throw new PartInitException(e.getLocalizedMessage(), e);
		}

		super.init(secondaryId, memento);
	}


	/**
	 * @see org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento memento) {
		IMemento myMemento = memento
			.createChild(MercurialSynchronizeParticipant.class.getName());
		repositoryLocations.serialize(myMemento);
		super.saveState(memento);
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant#restoreContext(org.eclipse.team.core.mapping.ISynchronizationScopeManager)
	 */
	@Override
	protected MergeContext restoreContext(ISynchronizationScopeManager manager) throws CoreException {
		RepositorySynchronizationScope scope = new RepositorySynchronizationScope(repositoryLocations);
		MercurialSynchronizeSubscriber subscriber = new MercurialSynchronizeSubscriber(scope);
		HgSubscriberScopeManager manager2 = new HgSubscriberScopeManager(scope.getMappings(), subscriber);
		subscriber.setParticipant(this);
		return new HgSubscriberMergeContext(subscriber, manager2);
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant#initializeContext(org.eclipse.team.core.mapping.provider.SynchronizationContext)
	 */
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

	public RepositoryLocationMap getRepositoryLocations() {
		return repositoryLocations;
	}

	public IHgRepositoryLocation getRepositoryLocation(HgRoot root) {
		return repositoryLocations.getLocation(root);
	}

	public IHgRepositoryLocation getRepositoryLocation(IProject project) {
		return repositoryLocations.getRepositoryLocation(project);
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
		//TODO: reevaluate!!!!

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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MercurialSynchronizeParticipant [");
		if (repositoryLocations != null) {
			builder.append("repositoryLocation=");
			builder.append(repositoryLocations);
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
