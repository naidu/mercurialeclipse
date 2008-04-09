package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIgnoreClient;
import com.vectrace.MercurialEclipse.dialogs.IgnoreDialog;

public class HgIgnoreAction extends SingleResourceAction {

	@Override
	protected void run(IResource resource) throws Exception {
		IgnoreDialog dialog;
		switch(resource.getType()) {
			case IResource.FILE:
				dialog = new IgnoreDialog(getShell(), (IFile)resource);
				break;
			case IResource.FOLDER:
				dialog = new IgnoreDialog(getShell(), (IFolder)resource);
				break;
			default:
				dialog = new IgnoreDialog(getShell());
		}
		
		if(dialog.open() == IDialogConstants.OK_ID) {
			switch(dialog.getResultType()) {
				case FILE:
					HgIgnoreClient.addFile(dialog.getFile());
					break;
				case EXTENSION:
					HgIgnoreClient.addExtension(dialog.getFile());
					break;
				case FOLDER:
					HgIgnoreClient.addFolder(dialog.getFolder());
					break;
				case GLOB:
					HgIgnoreClient.addGlob(resource.getProject(), dialog.getPattern());
					break;
				case REGEXP:
					HgIgnoreClient.addRegexp(resource.getProject(), dialog.getPattern());
					break;
			}
			try {
				MercurialStatusCache.getInstance().refresh(resource.getProject());
			} catch (TeamException e) {
				MercurialEclipsePlugin.logError("Unable to refresh project: ",
						e);
			}
		}
	}

}
