package com.vectrace.MercurialEclipse.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgAddClient {

	public static void addResources(List<IResource> resources,
			IProgressMonitor monitor) throws HgException {
		Map<IProject, List<IResource>> resourcesByProject = HgCommand
				.groupByProject(resources);
		for (IProject project : resourcesByProject.keySet()) {
			if (monitor != null) {
				monitor.subTask("Adding resources from " + project.getName());
			}
			// if there are too many resources, do several calls
			int size = resources.size();
			int delta = HgCommand.MAX_PARAMS-1;
			for (int i = 0; i < size; i += delta) {
				HgCommand command = new HgCommand("add", project, true);
				command.addFiles(resourcesByProject.get(project).subList(
						i,
						Math.min(i + delta, size-i)));
				command.executeToBytes();
			}
		}
	}
}
