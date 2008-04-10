package com.vectrace.MercurialEclipse.synchronize;

import java.util.Date;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;

public class MercurialSynchronizeParticipant extends SubscriberParticipant {
	private ISynchronizeScope myScope;
	private String secondaryId;
	
	public MercurialSynchronizeParticipant(ISynchronizeScope scope) {
		super(scope);
		this.myScope = scope;
		setSubscriber(new MercurialSynchronizeSubscriber(scope));
		this.secondaryId = getId() + " " + new Date(); 
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
		return myScope.getRoots();
	}

	@Override
	public String getName() {
		return "Mercurial Synchronization Subscriber";
	}

	@Override
	protected String getLongTaskName(IResource[] resources) {
		return super.getLongTaskName(resources) + " (hg status)";
	}

}
