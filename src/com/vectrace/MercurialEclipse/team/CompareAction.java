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
 *     StefanC                   - many updates
 *     Stefan Groschupf          - logError
 *     Jerome Negre              - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author zingo, Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class CompareAction extends SingleFileAction {

	@Override
	public void run(IFile file) {
		openEditor(file, null);
	}
	
	protected void openEditor(IFile file, String changeset) {
		SyncInfoCompareInput compareInput = getCompareInput(file, changeset);
		if (compareInput != null) {
			CompareUI.openCompareEditor(compareInput);
		}
	}

	/**
	 * 
	 * @param file
	 * @param changeset may be null
	 * @return
	 */
	private SyncInfoCompareInput getCompareInput(IFile file, String changeset) {
		try {
			MercurialRepositorySubscriber subscriber = new MercurialRepositorySubscriber();
			SyncInfo syncInfo = subscriber.getSyncInfo(
					file,
					file,
					new IStorageMercurialRevision(file, changeset));
			SyncInfoCompareInput compareInput = new SyncInfoCompareInput(
					"diff", syncInfo);
			return compareInput;
		} catch (TeamException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

}
