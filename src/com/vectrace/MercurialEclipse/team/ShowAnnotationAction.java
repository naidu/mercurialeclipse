package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.HgFile;
import com.vectrace.MercurialEclipse.annotations.ShowAnnotationOperation;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public class ShowAnnotationAction extends SingleFileAction {

	@Override
	protected void run(IFile file) throws Exception {
		new ShowAnnotationOperation(null, new HgFile(file)).run();
	}

}
