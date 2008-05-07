package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.TagDialog;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */public class TagHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		IProject project = resource.getProject();
		TagDialog dialog = new TagDialog(getShell(), project);
		
		if(dialog.open() == IDialogConstants.OK_ID) {
			HgTagClient.addTag(
					resource,
					dialog.getName(),
					dialog.getTargetRevision(),
					null, //user
					dialog.isLocal(),
					dialog.isForced());
			MercurialStatusCache.getInstance().refreshStatus(resource, null);
		}
	}

}
