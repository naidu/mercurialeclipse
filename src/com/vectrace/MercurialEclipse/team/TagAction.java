package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.TagDialog;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */public class TagAction extends SingleResourceAction {

	@Override
	protected void run(IResource resource) throws Exception {
		IProject project = resource.getProject();
		Tag[] tags = HgTagClient.getTags(project);
		ChangeSet[] changeSets = HgLogClient.getRevisions(project);
		TagDialog dialog = new TagDialog(getShell(), changeSets, tags);
		
		if(dialog.open() == IDialogConstants.OK_ID) {
			HgTagClient.addTag(
					resource,
					dialog.getName(),
					dialog.getTargetRevision(),
					null, //user
					dialog.isLocal(),
					dialog.isForced());
			DecoratorStatus.refresh();
		}
	}

}
