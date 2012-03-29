/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * john	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.BranchUtils;

/**
 * TODO: use JavaHg
 */
public class HgIdentifyClient extends AbstractClient {

	// expected output for merge:
	// b63617c1e3460bd87eb51d2b8841b37fff1834d6+00838f86e1024072e715d31f477262d5162acd09+ default
	// match second part (the one we merge with)
	// output for "usual" state:
	// b63617c1e3460bd87eb51d2b8841b37fff1834d6+ default
	// OR b63617c1e3460bd87eb51d2b8841b37fff1834d6 hallo branch
	// + after the id is the "dirty" flag - if some files are not committed yet
	//
	// As well in Mercurial 1.6.0 during some rebases the following output:
	// filtering src/nexj/core/admin/platform/websphere/WebSphereInstaller.java through
	// filtering
	// src/nexj/core/meta/j2ee/ibmconfig/cells/defaultCell/applications/defaultApp/deployments/defaultApp/deployment.xml
	// through
	// filtering src/nexj/core/meta/sys/system.chtypes through
	// 02bfc05967b86ba65a0cb990178638e4c491c865+d35923c18f8f564c6205e722119d88c6daa3f56d+ default
	// These leading lines are ignored.
	private static final Pattern ID_MERGE_AND_BRANCH_IGNORE_PATTERN = Pattern
			.compile("^filtering\\s.+\\sthrough\\s*$");

	// group 1 group 2 group 3
	// (first parent, optional dirty flag)(merge parent, optional dirty flag) space (branch name)
	private static final Pattern ID_MERGE_AND_BRANCH_PATTERN = Pattern
			.compile("^([0-9a-z]+\\+?)([0-9a-z]+)?\\+?\\s+(.+)$");

	/**
	 * @param root
	 *            non null
	 * @return current changeset id, merge id and branch name for the working directory
	 * @throws HgException
	 */
	public static String[] getIdMergeAndBranch(HgRoot root) throws HgException {
		AbstractShellCommand command = new HgCommand("id", "Identifying status", root, true); //$NON-NLS-1$
		// Full global IDs + branch name
		command.addOptions("-ib", "--debug"); //$NON-NLS-1$ //$NON-NLS-2$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
		String versionIds = null;
		BufferedReader br = new BufferedReader(new StringReader(command.executeToString().trim()));

		try {
			while ((versionIds = br.readLine()) != null) {
				if (!ID_MERGE_AND_BRANCH_IGNORE_PATTERN.matcher(versionIds).matches()) {
					break;
				}
			}
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
		} finally {
			try {
				br.close();
			} catch (IOException ex) {
			}
		}

		Matcher m = ID_MERGE_AND_BRANCH_PATTERN.matcher((versionIds == null) ? "" : versionIds);
		String mergeId = null;
		String branch = BranchUtils.DEFAULT;
		// current working directory id
		String id = "";
		if (m.matches() && m.groupCount() > 2) {
			id = m.group(1);
			mergeId = m.group(2);
			branch = m.group(3);
		}
		if (id.endsWith("+")) {
			id = id.substring(0, id.length() - 1);
		}
		return new String[] { id, mergeId, branch };
	}

}
