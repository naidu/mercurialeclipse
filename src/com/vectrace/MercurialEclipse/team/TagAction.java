package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.TagDialog;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */public class TagAction extends SingleResourceAction {

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

			try {
				MercurialStatusCache.getInstance().refreshStatus(resource.getProject(), new NullProgressMonitor());
			} catch (TeamException e) {
				MercurialEclipsePlugin.logError(Messages.getString("TagAction.unableToRefresh"), //$NON-NLS-1$
						e);
			}
//=======
//			MercurialEclipsePlugin.refreshProjectFlags(resource.getProject());
//>>>>>>> other
		}
	}

}
