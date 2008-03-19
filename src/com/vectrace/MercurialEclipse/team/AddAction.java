package com.vectrace.MercurialEclipse.team;

import java.util.List;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.exception.HgException;

public class AddAction extends MultipleFilesAction {

	@Override
	protected void run(List<IFile> files) throws HgException {
		HgAddClient.addFiles(files, null);
		DecoratorStatus.refresh();
	}

}
