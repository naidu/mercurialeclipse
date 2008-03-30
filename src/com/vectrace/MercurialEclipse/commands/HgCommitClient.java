package com.vectrace.MercurialEclipse.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgCommitClient {

	public static void commitResources(List<IResource> resources, String user,
			String message, IProgressMonitor monitor) throws HgException {
		
		Map<IProject, List<IResource>> resourcesByProject = HgCommand
				.groupByProject(resources);
		for (IProject project : resourcesByProject.keySet()) {
			if (monitor != null) {
				monitor.subTask("Committing resources from " + project.getName());
			}
			HgCommand command = new HgCommand("commit", project, true);
			command.addUserName(user);
			command.addOptions("-m", message);
			command.addFiles(resourcesByProject.get(project));
			command.executeToBytes();
		}
	}
}
