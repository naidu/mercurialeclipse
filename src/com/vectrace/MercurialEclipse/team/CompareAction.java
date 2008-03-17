/*******************************************************************************
 * Copyright (c) 2008 Vectrace (Zingo Andersen) 
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
		IProject proj = file.getProject();

		try {
			MercurialRepositorySubscriber subscriber = new MercurialRepositorySubscriber();
			SyncInfo syncInfo = subscriber.getSyncInfo(
					file,
					file,
					new IStorageMercurialRevision(proj, file, changeset));
			SyncInfoCompareInput compareInput = new SyncInfoCompareInput(
					"diff", syncInfo);
			return compareInput;
		} catch (TeamException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

}
