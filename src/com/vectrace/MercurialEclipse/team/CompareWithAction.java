package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class CompareWithAction extends CompareAction {

	@Override
	public void run(IFile file) {
		try {
			RevisionChooserDialog dialog = new RevisionChooserDialog(
					getShell(),
					"Compare With Revision...",
					HgLogClient.getRevisions(file),
					HgTagClient.getTags(file.getProject()));
			int result = dialog.open();
			if(result == IDialogConstants.OK_ID) {
				openEditor(file, dialog.getRevision());
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

}
