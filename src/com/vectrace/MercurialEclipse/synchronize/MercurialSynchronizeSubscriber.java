package com.vectrace.MercurialEclipse.synchronize;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariantComparator;

public class MercurialSynchronizeSubscriber extends Subscriber {
	

	public MercurialSynchronizeSubscriber() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String getName() {
		return "Mercurial Repository Watcher";
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return MercurialResourceVariantComparator.getInstance();
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		return null;
	}

	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		// TODO Auto-generated method stub

	}

	@Override
	public IResource[] roots() {
		// TODO Auto-generated method stub
		return null;
	}

}
