package com.vectrace.MercurialEclipse.team;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class CompareWithAction extends CompareAction {

	public void run(IAction action) {
		try {
			RevisionChooserDialog dialog = new RevisionChooserDialog(
					getShell(),
					"Compare With Revision...",
					HgLogClient.getRevisions(getSelectedFile()));
			int result = dialog.open();
			if(result == IDialogConstants.OK_ID) {
				openEditor(dialog.getRevision());
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

}
