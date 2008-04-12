package com.vectrace.MercurialEclipse.synchronize;

import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;

public class MercurialSynchronizeParticipant extends SubscriberParticipant {
	private String secondaryId;
	private MercurialSynchronizeSubscriber subscriber = null;

	public MercurialSynchronizeParticipant(ISynchronizeScope scope) {
		super(scope);		
		subscriber = new MercurialSynchronizeSubscriber(scope);		
		setSubscriber(subscriber);
		this.secondaryId = new Date().toString();
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
	public IResource[] getResources() {
		return subscriber.roots();
	}

	@Override
	public String getName() {
		return "Mercurial Synchronization for " + getResources();
	}

	@Override
	protected String getLongTaskName(IResource[] resources) {
		return "Mercurial: Refreshing resources for synchronization...";
	}

}
