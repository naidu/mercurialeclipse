package com.vectrace.MercurialEclipse.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgAddClient {

	/**
	 * All files must be from the same project
	 * @param files
	 * @param project
	 * @throws HgException
	 */
	public static void addFilesFromProject(List<IFile> files, IProject project) throws HgException {
		HgCommand command = new HgCommand("add", project, true);
		command.addFiles(files);
		command.executeToBytes();
	}

	public static void addResources(List<IResource> resources, IProgressMonitor monitor) throws HgException {
		Map<IProject, List<IResource>> resourcesByProject = HgCommand.groupByProject(resources);
		for(IProject project : resourcesByProject.keySet()) {
			if(monitor!=null) {
				monitor.subTask("Adding resources from "+project.getName());
			}
			HgCommand command = new HgCommand("add", project, true);
			command.addFiles(resourcesByProject.get(project));
			command.executeToBytes();
		}
	}
}
