/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 * Andrei Loskutov (Intland) - bugfixes
 * Zsolt Koppany (Intland)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ide.ResourceUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author bastian
 * @author Andrei Loskutov
 */
public class ResourceUtils {

	private static final File TMP_ROOT = new File(System.getProperty("java.io.tmpdir"));
	private static long tmpFileSuffix = 0;

	public static File getSystemTempDirectory() {
		return TMP_ROOT;
	}

	/**
	 * @return a newly created temp directory which is located inside the default temp directory
	 */
	public static File createNewTempDirectory() {
		File tmp = getSystemTempDirectory();
		File newTemp = null;
		while (!(newTemp = new File(tmp, "hgTemp_" + tmpFileSuffix)).mkdir()) {
			tmpFileSuffix++;
		}
		return newTemp;
	}

	/**
	 * If "recursive" is false, then this is a single file/directory delete operation. Directory
	 * should be empty before it can be deleted. If "recursive" is true, then all children will be
	 * deleted too.
	 *
	 * @param source
	 * @return true if source was successfully deleted or if it was not existing
	 */
	public static boolean delete(File source, boolean recursive) {
		if (source == null || !source.exists()) {
			return true;
		}
		if (recursive) {
			if (source.isDirectory()) {
				for (File file : source.listFiles()) {
					boolean ok = delete(file, true);
					if (!ok) {
						return false;
					}
				}
			}
		}
		boolean result = source.delete();
		if (!result && !source.isDirectory()) {
			MercurialEclipsePlugin.logWarning("Could not delete file '" + source + "'", null);
		}
		return result;
	}

	/**
	 * Checks which editor is active an determines the IResource that is edited.
	 */
	public static IFile getActiveResourceFromEditor() {
		IEditorPart editorPart = MercurialEclipsePlugin.getActivePage().getActiveEditor();

		if (editorPart != null) {
			IFileEditorInput input = (IFileEditorInput) editorPart.getEditorInput();
			IFile file = ResourceUtil.getFile(input);
			return file;
		}
		return null;
	}

	/**
	 * @param resource
	 *            a handle to possibly non-existing resource
	 * @return a (file) path representing given resource
	 */
	public static File getFileHandle(IResource resource) {
		IPath path = getPath(resource);
		return path.toFile();
	}

	/**
	 * @param path
	 *            a path to possibly non-existing or not mapped resource
	 * @return a (file) representing given resource, may return null if the resource is not in the
	 *         workspace
	 */
	public static IFile getFileHandle(IPath path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return root.getFileForLocation(path);
	}

	/**
	 * @param resource
	 *            a handle to possibly non-existing resource
	 * @return a (file) path representing given resource
	 */
	public static IPath getPath(IResource resource) {
		IPath path = resource.getLocation();
		if (path == null) {
			// file was removed
			IProject project = resource.getProject();
			IPath projectLocation = project.getLocation();
			if (projectLocation == null) {
				// project removed too
				projectLocation = project.getWorkspace().getRoot().getLocation().append(
						project.getName());
			}
			if (project == resource) {
				return projectLocation;
			}
			path = projectLocation.append(resource.getFullPath().removeFirstSegments(1));
		}
		return path;
	}

	/**
	 * Converts a {@link java.io.File} to a workspace resource
	 *
	 * @param file
	 * @return
	 * @throws HgException
	 */
	public static IResource convert(File file) throws HgException {
		String canonicalPath;
		try {
			canonicalPath = file.getCanonicalPath();
		} catch (IOException e) {
			throw new HgException(e.getLocalizedMessage(), e);
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource;
		if (file.isDirectory()) {
			resource = root.getContainerForLocation(new Path(canonicalPath));
		} else {
			resource = root.getFileForLocation(new Path(canonicalPath));
		}
		return resource;
	}

	/**
	 * For a given path, tries to find out first <b>existing</b> parent directory
	 *
	 * @param path
	 *            may be null
	 * @return may return null
	 */
	public static File getFirstExistingDirectory(File path) {
		while (path != null && !path.isDirectory()) {
			path = path.getParentFile();
		}
		return path;
	}

	/**
	 * For a given path, tries to find out first <b>existing</b> parent directory
	 *
	 * @param res
	 *            may be null
	 * @return may return null
	 */
	public static IContainer getFirstExistingDirectory(IResource res) {
		if (res == null) {
			return null;
		}
		IContainer parent = res instanceof IContainer ? (IContainer) res : res.getParent();
		if (parent instanceof IWorkspaceRoot) {
			return null;
		}
		while (parent != null && !parent.exists()) {
			parent = parent.getParent();
			if (parent instanceof IWorkspaceRoot) {
				return null;
			}
		}
		return parent;
	}

	/**
	 * @param resources
	 *            non null
	 * @return never null
	 */
	public static Map<IProject, List<IResource>> groupByProject(Collection<IResource> resources) {
		Map<IProject, List<IResource>> result = new HashMap<IProject, List<IResource>>();
		for (IResource resource : resources) {
			IProject root = resource.getProject();
			List<IResource> list = result.get(root);
			if (list == null) {
				list = new ArrayList<IResource>();
				result.put(root, list);
			}
			list.add(resource);
		}
		return result;
	}

	/**
	 * @return never null, a list with all projects contained by given hg root directory
	 */
	public static Set<IProject> getProjects(HgRoot root) {
		Set<IProject> set = new HashSet<IProject>();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (!project.isAccessible()) {
				continue;
			}
			HgRoot proot = MercurialTeamProvider.hasHgRoot(project);
			if (proot == null) {
				continue;
			}
			if (root.equals(proot)) {
				set.add(project);
			}
		}
		return set;
	}

	/**
	 * @param resources
	 *            non null
	 * @return never null
	 */
	public static Map<HgRoot, List<IResource>> groupByRoot(Collection<? extends IResource> resources) {
		Map<HgRoot, List<IResource>> result = new HashMap<HgRoot, List<IResource>>();
		if (resources != null) {
			for (IResource resource : resources) {
				HgRoot root = MercurialTeamProvider.hasHgRoot(resource);
				if (root == null) {
					continue;
				}
				List<IResource> list = result.get(root);
				if (list == null) {
					list = new ArrayList<IResource>();
					result.put(root, list);
				}
				list.add(resource);
			}
		}
		return result;
	}

	public static void collectAllResources(IContainer root, Set<IResource> children) {
		IResource[] members;
		try {
			members = root.members();
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return;
		}
		children.add(root);
		for (IResource res : members) {
			if (res instanceof IFolder && !res.equals(root)) {
				collectAllResources((IFolder) res, children);
			} else {
				children.add(res);
			}
		}
	}

	/**
	 * @param hgRoot
	 *            non null
	 * @param project
	 *            non null
	 * @param repoRelPath
	 *            path <b>relative</b> to the hg root
	 * @return may return null, if the path is not found in the project
	 */
	public static IResource convertRepoRelPath(HgRoot hgRoot, IProject project, String repoRelPath) {
		// determine absolute path
		IPath path = new Path(hgRoot.getAbsolutePath()).append(repoRelPath);

		// determine project relative path
		int equalSegments = path.matchingFirstSegments(project.getLocation());
		path = path.removeFirstSegments(equalSegments);
		return project.findMember(path);
	}

	public static Set<IResource> getMembers(IResource r) {
		HashSet<IResource> set = new HashSet<IResource>();
		if (r instanceof IContainer && r.isAccessible()) {
			IContainer cont = (IContainer) r;
			try {
				IResource[] members = cont.members();
				if (members != null) {
					for (IResource member : members) {
						if (member instanceof IContainer) {
							set.addAll(getMembers(member));
						} else {
							set.add(member);
						}
					}
				}
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		set.add(r);
		return set;
	}

	/**
	 * @param o
	 *            some object which is or can be adopted to resource
	 * @return given object as resource, may return null
	 */
	public static IResource getResource(Object o) {
		if (o instanceof IResource) {
			return (IResource) o;
		}
		if (o instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) o;
			Set<IFile> files = changeSet.getFiles();
			if (files.size() > 0) {
				IFile file = files.iterator().next();
				if (files.size() == 1) {
					return file;
				}
				return file.getProject();
			}
			return null;
		}
		if (o instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) o;
			IResource adapter = (IResource) adaptable.getAdapter(IResource.class);
			if (adapter != null) {
				return adapter;
			}
			adapter = (IResource) adaptable.getAdapter(IFile.class);
			if (adapter != null) {
				return adapter;
			}
		}
		return null;
	}

	public static void touch(final IResource res) {
		Job job = new Job("Refresh for: " + res.getName()) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// triggers the decoration update
				try {
					if (res.isAccessible()) {
						res.touch(monitor);
					}
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/**
	 * @param selection may be null
	 * @return never null , may be ampty list containing all resources from given selection
	 */
	public static List<IResource> getResources(IStructuredSelection selection) {
		List<IResource> resources = new ArrayList<IResource>();
		List<?> list = selection.toList();
		for (Object object : list) {
			IResource resource = getResource(object);
			if(resource != null && !resources.contains(resource)){
				resources.add(resource);
			}
		}
		return resources;
	}
}
