/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgGrepClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchVisitor {

	private TextSearchRequestor requestor;
	private Pattern pattern;

	/**
	 *
	 */
	public MercurialTextSearchVisitor() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param requestor
	 * @param searchPattern
	 */
	public MercurialTextSearchVisitor(TextSearchRequestor requestor,
			Pattern searchPattern) {
		this.requestor = requestor;
		this.pattern = searchPattern;
	}

	/**
	 * @param scope
	 * @param monitor
	 * @return
	 */
	public IStatus search(TextSearchScope scope, IProgressMonitor monitor) {
		IResource[] scopeRoots = scope.getRoots();
		String searchString = pattern.pattern();
		monitor.beginTask("Searching for " + searchString + " with Mercurial",
				scopeRoots.length * 5);
		for (IResource resource : scopeRoots) {
			monitor.subTask("Searching in " + resource);
			HgRoot root = MercurialTeamProvider.hasHgRoot(resource);
			monitor.worked(1);
			if (root != null) {
				try {
					return search(root, monitor);
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
					return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
							e.getLocalizedMessage(), e);
				}
			}
		}
		return new Status(IStatus.INFO, MercurialEclipsePlugin.ID,
				"Nothing found.");
	}

	/**
	 * @param root
	 * @throws CoreException
	 */
	private IStatus search(HgRoot root, IProgressMonitor monitor)
			throws CoreException {
		try {
			requestor.beginReporting();
			monitor.subTask("Calling Mercurial grep command...");
			List<MercurialTextSearchMatchAccess> result = HgGrepClient.grep(root, pattern
					.pattern());
			monitor.worked(1);
			monitor.subTask("Processing Mercurial grep results...");
			for (MercurialTextSearchMatchAccess sr : result) {
				monitor.subTask("Found match in: " + sr.getFile().getName());
				requestor.acceptFile(sr.getFile());
				monitor.worked(1);
				requestor.acceptPatternMatch(sr);
				monitor.worked(1);
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			return new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, e
					.getLocalizedMessage(), e);
		}
		requestor.endReporting();
		return new Status(IStatus.OK, MercurialEclipsePlugin.ID,
				"Mercurial search completed successfully.");
	}

	/**
	 * @param scope
	 * @param monitor
	 * @return
	 */
	public IStatus search(IFile[] scope, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

}
