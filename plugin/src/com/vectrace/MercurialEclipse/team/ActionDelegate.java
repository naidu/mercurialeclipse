/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * svetlana.daragatch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.model.PathFromChangeSet;
import com.vectrace.MercurialEclipse.model.UncommittedChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author svetlana.daragatch
 *
 */
public abstract class ActionDelegate implements IWorkbenchWindowActionDelegate {
	protected IFile projectSelection;
	protected IStructuredSelection selection;

	private static final Class JEL;

	static
	{
		Class c;

		try {
			c = Class.forName("org.eclipse.jdt.core.IJavaElement");
		} catch (Exception e){
			c = null;
		}

		JEL = c;
	}



	protected static IResource getFile(Object o) {
		IResource resource = null;

		if (o instanceof IFile) {
			resource = (IResource) o;
		} else if (o instanceof FileFromChangeSet) {
			resource = ((FileFromChangeSet) o).getFile();
		} else if (JEL != null && JEL.isInstance(o)) {
			resource = invokeJEL(o, "getResource");
		}

		return resource;
	}

	protected static Object getFileResource(Object o) {
		Object resource = null;

		if (o instanceof IFile) {
			resource = o;
		} else if (o instanceof FileFromChangeSet) {
			resource = o;
		}

		return resource;
	}

	protected static HgRoot getHgRoot(Object o) {
		HgRoot hgRoot = null;

		if (o instanceof ChangesetGroup) {
			hgRoot = ((ChangesetGroup) o).getRepositoryChangesetGroup().getRoot();
		} else if (o instanceof IResource) {
			hgRoot = MercurialTeamProvider.getHgRoot((IResource) o);
		} else if (o instanceof JHgChangeSet) {
			hgRoot = ((JHgChangeSet) o).getHgRoot();
		} else if (o instanceof PathFromChangeSet) {
			hgRoot = getSynchronizeViewHgRoot(((PathFromChangeSet) o).getParent());
		} else if (o instanceof FileFromChangeSet) {
			hgRoot = getSynchronizeViewHgRoot(((FileFromChangeSet) o).getChangeset());
		} else if (o instanceof UncommittedChangeSet) {
			hgRoot = getSynchronizeViewHgRoot(o);
		} else if (o instanceof IFile) {
			hgRoot = MercurialTeamProvider.getHgRoot((IResource) o);
		} else if (o instanceof MercurialRevision) {
			hgRoot = MercurialTeamProvider.getHgRoot(((MercurialRevision) o).getResource());
		} else if (JEL != null && JEL.isInstance(o)) {
			try {
				hgRoot = MercurialTeamProvider.getHgRoot(invokeJEL(o, "getCorrespondingResource"));

				if (hgRoot == null) {
					hgRoot = getHgRoot(invokeJEL(o, "getJavaProject"));
				}
			} catch (Exception e) {
			}
		} else if (o instanceof IAdaptable) {
			IWorkbenchAdapter resource = ((IWorkbenchAdapter)((IAdaptable) o).getAdapter(IWorkbenchAdapter.class));

			if (resource != null) {
				hgRoot = getHgRoot(resource.getParent(o));
			}
		}

		return hgRoot;
	}

	protected static IResource getResource(Object o) {
		if (o instanceof IResource) {
			return (IResource) o;
		} else if (o instanceof PathFromChangeSet) {
			return (IResource) ((PathFromChangeSet) o).getAdapter(IResource.class);
		} else if (JEL != null && JEL.isInstance(o)) {
			return invokeJEL(o, "getResource");
		}
		// in theory, we can define IResources even from IFiles and IJavaElement if it's ever necessary

		return null;
	}

	private static HgRoot getSynchronizeViewHgRoot(Object parent) {
		HgRoot hgRoot = null;

		if (parent instanceof UncommittedChangeSet) {
			hgRoot = ((UncommittedChangeSet)parent).getHgRoot();

			if (hgRoot == null) {
				IProject[] projects = ((UncommittedChangeSet)parent).getProjects();
				if (projects != null && projects.length > 0) {
					hgRoot = MercurialTeamProvider.getHgRoot(projects[0]);
				}
			}
		} else if (parent instanceof JHgChangeSet) {
			hgRoot = ((JHgChangeSet) parent).getHgRoot();
		}

		return hgRoot;
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 *
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	protected List<IResource> getSelectedHgProjects() {
		List<IResource> result = new ArrayList<IResource>();

		List<HgRoot> hgRoots = getSelectedHgRoots();

		if (hgRoots != null && hgRoots.size() > 0) {
			for (HgRoot hgRoot : hgRoots) {
				List<IProject> hgProjects = MercurialTeamProvider.getKnownHgProjects(hgRoot);

				if (hgProjects != null && hgProjects.size() > 0) {
					result.addAll(hgProjects);
				}
			}
		}

		return result;
	}

	protected List<HgRoot> getSelectedHgRoots() {
		List<HgRoot> result = new ArrayList<HgRoot>();
		HgRoot hgRoot;

		if (selection != null) {
			for (Object o : selection.toList()) {
				hgRoot = getHgRoot(o);

				if (hgRoot != null) {
					result.add(hgRoot);
				}
			}
		} else if (projectSelection != null) {
			hgRoot = getHgRoot(projectSelection.getProject());

			if (hgRoot != null) {
				result.add(hgRoot);
			}
		}

		return result;
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 *
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow w) {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * @throws HgException
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public abstract void run(IAction action);

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 *
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection inSelection) {
		if (inSelection != null) {
			if (inSelection instanceof IStructuredSelection) {
				selection = (IStructuredSelection) inSelection;
				projectSelection = null;
			} else {
				selection = null;
				projectSelection = ResourceUtils.getActiveResourceFromEditor();
			}
		}
	}

	private static IResource invokeJEL(Object o, String method) {
		try {
			return (IResource) JEL.getMethod(method).invoke(o);
		} catch (SecurityException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (NoSuchMethodException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (IllegalArgumentException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (IllegalAccessException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (ClassCastException e) {
			MercurialEclipsePlugin.logError(e);
		}

		return null;
	}
}
