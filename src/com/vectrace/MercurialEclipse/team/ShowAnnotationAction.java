package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.annotations.ShowAnnotationOperation;
import com.vectrace.MercurialEclipse.model.HgFile;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public class ShowAnnotationAction extends SingleFileAction implements IObjectActionDelegate {

	IWorkbenchPart part;
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}

	@Override
	protected void run(IFile file) throws Exception {
		new ShowAnnotationOperation(part, new HgFile(file.getLocation()
                .toFile())).run();
	}

}
