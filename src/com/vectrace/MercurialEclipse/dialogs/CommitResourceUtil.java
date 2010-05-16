/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Jérôme Nègre              - some fixes
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.Team;

import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

public final class CommitResourceUtil {


	private CommitResourceUtil() {
		// static utility
	}

	/**
	 * @return never null
	 */
	public static CommitResource[] getCommitResources(List<IResource> inResources) throws HgException {
		if(inResources.size() == 0){
			return new CommitResource[0];
		}
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(inResources);
		Set<CommitResource> toCommit = new HashSet<CommitResource>();
		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot hgRoot = mapEntry.getKey();
			String result = HgStatusClient.getStatusForCommit(hgRoot, inResources);
			if(!StringUtils.isEmpty(result)) {
				toCommit.addAll(spliceStatusResult(result, hgRoot));
			}
		}
		return toCommit.toArray(new CommitResource[0]);
	}

	/**
	 * Splice the output of the status result and build the CommitResources from that
	 * @param statusOutput The output string of the Mercurial status action
	 * @param root
	 * @return The Commit-resources
	 */
	private static List<CommitResource> spliceStatusResult(String statusOutput, HgRoot root) {

		ArrayList<CommitResource> list = new ArrayList<CommitResource>();
		StringTokenizer st = new StringTokenizer(statusOutput);
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

		// Tokens are always in pairs as lines are in the form "A
		// TEST_FOLDER\test_file2.c"
		// where the first token is the status and the 2nd is the path relative
		// to the root.
		while (st.hasMoreTokens()) {
			String status = st.nextToken(" ").trim();
			String fileName = st.nextToken("\n").trim();
//            if(status.startsWith("?")){
//                continue;
//            }
			Path path = new Path(new File(root, fileName).getAbsolutePath());
			IResource statusResource = workspaceRoot.getFileForLocation(path);
			if (!Team.isIgnoredHint(statusResource)) {
				// file is allready managed or file is not in "ignore list"
				list.add(new CommitResource(status, statusResource, new File(fileName)));
			}
		}

		return list;
	}

	/**
	 * Filter a list of commit-resources to contain only tracked ones (which are already tracked by Mercurial).
	 */
	public static List<CommitResource> filterForTracked(CommitResource[] commitResources) {
		List<CommitResource> tracked = new ArrayList<CommitResource>();
		for (CommitResource commitResource : commitResources) {
			if (MercurialStatusCache.CHAR_UNKNOWN != commitResource.getStatus()) {
				tracked.add(commitResource);
			}
		}
		return tracked;
	}

	/**
	 * Filter a list of commit-resources to contain only those which are equal with a set of IResources
	 * @param commitResources
	 * @param resources
	 * @return The commit resources
	 */
	public static List<CommitResource> filterForResources(List<CommitResource> commitResources, List<IResource> resources) {
		List<CommitResource> result = new ArrayList<CommitResource>();
		if (resources == null || resources.isEmpty()) {
			return result;
		}
		Set<IResource> resourceSet = new HashSet<IResource>();
		resourceSet.addAll(resources);

		for (CommitResource commitResource : commitResources) {
			IResource res = commitResource.getResource();
			if (res != null && resourceSet.contains(res)) {
				result.add(commitResource);
			}
		}
		return result;
	}

}
