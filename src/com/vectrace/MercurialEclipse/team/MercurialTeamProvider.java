/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - some updates
 *     Bastian Doetsch           - new qualified name for project sets
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland)     - Fix encoding
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.team.IMoveDeleteHook;
import org.eclipse.core.resources.team.ResourceRuleFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.ui.IPropertyListener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.HgRootContainer;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author zingo
 */
public class MercurialTeamProvider extends RepositoryProvider {

	public static final String ID = "com.vectrace.MercurialEclipse.team.MercurialTeamProvider"; //$NON-NLS-1$

	/**
	 * The value is one element array, which single element is NON null if the project has
	 * a hg root
	 */
	private static final Map<IProject, HgRoot[]> HG_ROOTS = new ConcurrentHashMap<IProject, HgRoot[]>();

	/** key is project, value is the *current* branch */
	private static final Map<IProject, String> BRANCH_MAP = new ConcurrentHashMap<IProject, String>();

	private MercurialHistoryProvider FileHistoryProvider;

	private static final ListenerList branchListeners = new ListenerList(ListenerList.IDENTITY);

	/** @see #getRuleFactory() */
	private IResourceRuleFactory resourceRuleFactory;

	public MercurialTeamProvider() {
		super();
	}

	public static SortedSet<HgRoot> getKnownHgRoots(){
		SortedSet<HgRoot> roots = new TreeSet<HgRoot>();
		Collection<HgRoot[]> values = HG_ROOTS.values();
		for (HgRoot[] hgRoots : values) {
			for (HgRoot hgRoot : hgRoots) {
				roots.add(hgRoot);
			}
		}
		return roots;
	}

	public static List<IProject> getKnownHgProjects(){
		List<IProject> projects = new ArrayList<IProject>();
		IProject[] iProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : iProjects) {
			if(isHgTeamProviderFor(project)){
				projects.add(project);
			}
		}
		return projects;
	}

	public static List<IProject> getKnownHgProjects(HgRoot hgRoot){
		List<IProject> hgProjects = getKnownHgProjects();
		List<IProject> projects = new ArrayList<IProject>();
		for (IProject project : hgProjects) {
			if(hgRoot.equals(hasHgRoot(project))){
				projects.add(project);
			}
		}
		return projects;
	}

	@Override
	public void setProject(IProject project) {
		super.setProject(project);
		HgRoot hgRoot = getHgRoot(project);
		if(hgRoot != null){
			return;
		}
		try {
			configureProject();
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	@Override
	public void configureProject() throws CoreException {
		IProject project = getProject();
		HgRoot hgRoot = getAndStoreHgRoot(project);
		HG_ROOTS.put(project, new HgRoot[] { hgRoot });
		// try to find .hg directory to set it as private member
		IResource hgDir = project.getFolder(".hg"); //$NON-NLS-1$
		if (hgDir != null) {
			if(!hgDir.exists()){
				hgDir.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			}
			if(hgDir.exists()) {
				hgDir.setTeamPrivateMember(true);
				hgDir.setDerived(true);
			}
		}
		if(!MercurialStatusCache.getInstance().isStatusKnown(project)) {
			new RefreshStatusJob("Initializing hg cache for: " + hgRoot.getName(),  hgRoot).schedule(200);
		}
	}

	public void deconfigure() throws CoreException {
		IProject project = getProject();
		Assert.isNotNull(project);
		// cleanup
		HG_ROOTS.put(project, new HgRoot[]{null});
		BRANCH_MAP.remove(project);
//		project.setPersistentProperty(ResourceProperties.HG_ROOT, null);
		project.setSessionProperty(ResourceProperties.HG_BRANCH, null);
		HgStatusClient.clearMergeStatus(project);
	}

	/**
	 * Checks if the given project is controlled by MercurialEclipse.
	 *
	 * @return true, if MercurialEclipse provides team functions to this
	 *         project, false otherwise.
	 */
	public static boolean isHgTeamProviderFor(IProject project){
		Assert.isNotNull(project);
		HgRoot[] result = HG_ROOTS.get(project);
		if(result == null){
			return RepositoryProvider.getProvider(project, ID) != null;
		}
		return result[0] != null;
	}

	public static void addBranchListener(IPropertyListener listener){
		branchListeners.add(listener);
	}

	public static void removeBranchListener(IPropertyListener listener){
		branchListeners.remove(listener);
	}

	/**
	 * Determines if the resources hg root is known. If it isn't known, Mercurial is called to determine it. The result
	 * will be saved as project session property on the resource's project with the qualified name
	 * {@link ResourceProperties#HG_ROOT}.
	 *
	 * This property can be used to create a {@link java.io.File}.
	 *
	 * @param resource
	 *            the resource to get the hg root for, not null
	 * @return the canonical file path of the HgRoot
	 * @throws HgException
	 */
	private static HgRoot getAndStoreHgRoot(IResource resource) throws HgException {
		IProject project = resource.getProject();
		if (project == null || !resource.exists()) {
			return AbstractClient.getHgRoot(resource);
		}
		HgRoot[] rootElt = HG_ROOTS.get(project);
		if(rootElt == null){
			rootElt = new HgRoot[1];
			HG_ROOTS.put(project, rootElt);
		}
		HgRoot hgRoot = rootElt[0]; // (HgRoot) project.getSessionProperty(ResourceProperties.HG_ROOT);
		if (hgRoot == null) {
			hgRoot = AbstractClient.getHgRoot(resource);
			rootElt[0] = hgRoot;
			setRepositoryEncoding(project, hgRoot);
			// project.setSessionProperty(ResourceProperties.HG_ROOT, hgRoot);
		}
		return hgRoot;
	}

	private static void setRepositoryEncoding(IProject project, HgRoot hgRoot) {
		if (project != null) {
			hgRoot.setEncoding(Charset.forName(MercurialEclipsePlugin.getDefaultEncoding(project)));
		}
	}

	private static HgRoot getHgRootFile(File file) throws HgException {
		return HgRootClient.getHgRoot(file);
	}

	/**
	 * Gets the hg root of a resource as {@link java.io.File}.
	 *
	 * @param resource
	 *            the resource to get the hg root for, not null
	 * @return the {@link java.io.File} referencing the hg root directory
	 * @throws HgException
	 *             if an error occurred (e.g. no root could be found)
	 */
	public static HgRoot getHgRoot(IResource resource) throws HgException {
		if(resource == null || resource instanceof IWorkspaceRoot){
			return null;
		}
		if(resource instanceof HgRootContainer){
			HgRootContainer rootContainer = (HgRootContainer) resource;
			return rootContainer.getHgRoot();
		}
		IProject project = resource.getProject();
		if (project == null && resource.getLocation() != null) {
			// can it happen at all????
			return AbstractClient.getHgRoot(resource);
		}
		return getHgRoot(project);
	}

	/**
	 * Gets the hg root of a resource as {@link java.io.File}.
	 *
	 * @param project
	 *            the project to get the hg root for, not null
	 * @return the {@link java.io.File} referencing the hg root directory
	 */
	public static HgRoot getHgRoot(IProject project) {
		HgRoot[] rootElt = HG_ROOTS.get(project);
		if(rootElt == null){
			rootElt = new HgRoot[1];
			HG_ROOTS.put(project, rootElt);
		}
		return rootElt[0];
	}

	/**
	 * Gets the hg root of a resource as {@link java.io.File}.
	 *
	 * @param resource
	 *            the resource to get the hg root for
	 * @return the {@link java.io.File} referencing the hg root directory, or null if no
	 * hg root can't be found
	 */
	public static HgRoot hasHgRoot(IResource resource) {
		if(resource == null || resource instanceof IWorkspaceRoot){
			return null;
		}
		IProject project = resource.getProject();
		HgRoot[] result = HG_ROOTS.get(project);
		if(result != null && result[0] != null){
			return result[0];
		}
		HgRoot hgRoot = HgRootClient.hasHgRoot(ResourceUtils.getFileHandle(resource));
		if(hgRoot != null && (RepositoryProvider.getProvider(project, ID) != null)) {
			HG_ROOTS.put(project, new HgRoot[]{hgRoot});
		}
		return hgRoot;
	}

	/**
	 * Gets the hg root of a resource as {@link java.io.File}.
	 *
	 * @param file
	 *            a {@link java.io.File}, not null
	 * @return the file object of the root.
	 * @throws HgException
	 */
	public static HgRoot getHgRoot(File file) throws HgException {
		IResource resource = ResourceUtils.convert(file);
		if (resource != null) {
			return getHgRoot(resource);
		}
		return getHgRootFile(file);
	}

	/**
	 * @param res non null
	 * @return current resource branch, never null.
	 */
	public static String getCurrentBranch(IResource res){
		Assert.isNotNull(res);
		IProject project = res.getProject();
		String branch = BRANCH_MAP.get(project);
		if(branch == null){
			try {
				branch = (String) project.getSessionProperty(ResourceProperties.HG_BRANCH);
				branch = branch == null? Branch.DEFAULT : branch;
				BRANCH_MAP.put(project, branch);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
				return Branch.DEFAULT;
			}
		}
		return branch;
	}

	public static void setCurrentBranch(String branch, IProject project){
		String oldBranch = null;
		if(branch != null){
			oldBranch = BRANCH_MAP.put(project, branch);
		} else {
			BRANCH_MAP.remove(project);
		}
		try {
			if(project.isAccessible()) {
				project.setSessionProperty(ResourceProperties.HG_BRANCH, branch);
			}
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
		}
		if(branch != null && !Branch.same(branch, oldBranch)){
			Object[] listeners = branchListeners.getListeners();
			for (int i = 0; i < listeners.length; i++) {
				IPropertyListener listener = (IPropertyListener) listeners[i];
				listener.propertyChanged(project, 0);
			}
		}
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public IMoveDeleteHook getMoveDeleteHook() {
		return new HgMoveDeleteHook();
	}

	@Override
	public IFileHistoryProvider getFileHistoryProvider() {
		if (FileHistoryProvider == null) {
			FileHistoryProvider = new MercurialHistoryProvider();
		}
		// System.out.println("getFileHistoryProvider()");
		return FileHistoryProvider;
	}

	@Override
	public boolean canHandleLinkedResources() {
		return true;
	}

	@Override
	public boolean canHandleLinkedResourceURI() {
		return canHandleLinkedResources();
	}

	/**
	 * Overrides the default pessimistic resource rule factory which locks the workspace for all
	 * operations. This causes problems when opening a project. This method returns the default
	 * non-pessimistic resource rule factory which locks on a finer level.
	 */
	@Override
	public IResourceRuleFactory getRuleFactory() {
		if (resourceRuleFactory == null) {
			resourceRuleFactory = new ResourceRuleFactory() {};
		}
		return resourceRuleFactory;
	}
}
