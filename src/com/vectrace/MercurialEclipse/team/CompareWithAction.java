package com.vectrace.MercurialEclipse.team;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class CompareWithAction extends CompareAction {

	public void run(IAction action) {
		RevisionChooserDialog dialog = new RevisionChooserDialog(
				getShell(),
				"Compare With Revision...");
		int result = dialog.open();
		if(result == IDialogConstants.OK_ID) {
			openEditors(dialog.getRevision());
		}
	}

}
