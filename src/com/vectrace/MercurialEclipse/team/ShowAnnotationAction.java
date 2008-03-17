package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.commands.HgAnnotateClient;
import com.vectrace.MercurialEclipse.dialogs.MultiLineDialog;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public class ShowAnnotationAction extends SingleFileAction {

	@Override
	protected void run(IFile file) throws HgException {
		new MultiLineDialog(
				getShell(),
				"Mercurial Eclipse Annotate - "+file.getName(),
				HgAnnotateClient.getAnnotation(file)
				).open();
	}

}
