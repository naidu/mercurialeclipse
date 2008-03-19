package com.vectrace.MercurialEclipse.team;

import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.exception.HgException;

public class AddAction extends MultipleResourcesAction {

	@Override
	protected void run(List<IResource> resources) throws HgException {
		HgAddClient.addResources(resources, null);
		DecoratorStatus.refresh();
	}

}
