/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgMergeClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgIMergeClient;
import com.vectrace.MercurialEclipse.dialogs.RevisionChooserDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.ProjectDataLoader;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.views.MergeView;

public class MergeHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {        
        merge(resource, getShell());
    }

    /**
     * @param resource
     * @throws HgException
     * @throws CoreException
     * @throws PartInitException
     */
    public static String merge(IResource resource, Shell shell) throws HgException, CoreException,
            PartInitException {
        IProject project = resource.getProject();
        DataLoader loader = new ProjectDataLoader(project);

        // can we do the equivalent of plain "hg merge"?
        ChangeSet cs = getOtherHeadInCurrentBranch(project, loader);
        if (cs != null) {
            MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            mb.setText("Merge");
            mb.setMessage(Messages.getString("MergeHandler.mergeWithOtherHead"));
            if (mb.open() == SWT.NO)
                cs = null;
        }
        
        // have to open the dialog
        if (cs == null) {
            RevisionChooserDialog dialog = new RevisionChooserDialog(shell,
                    Messages.getString("MergeHandler.mergeWith"), loader); //$NON-NLS-1$
            dialog.setDefaultShowingHeads(true);
            dialog.setDisallowSelectingParents(true);
            
            if (dialog.open() != IDialogConstants.OK_ID)
                return "";
            
            cs = dialog.getChangeSet();
        }
        
        boolean useExternalMergeTool = Boolean.valueOf(
                HgClients.getPreference(
                        MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
                "false")).booleanValue(); //$NON-NLS-1$
        String result = ""; //$NON-NLS-1$
        boolean useResolve = isHgResolveAvailable();
        if (useResolve) {
            result = HgMergeClient.merge(resource, cs.getRevision().getChangeset(),
                    useExternalMergeTool);
        } else {
            result = HgIMergeClient.merge(project, cs.getRevision().getChangeset());
        }
        
        project.setPersistentProperty(ResourceProperties.MERGING, cs.getChangeset());
        MergeView view = (MergeView) PlatformUI.getWorkbench()
        .getActiveWorkbenchWindow().getActivePage().showView(
                MergeView.ID);
        view.clearView();
        view.setCurrentProject(project);
        // trigger refresh of project decoration
        project.touch(new NullProgressMonitor());
        project.refreshLocal(IResource.DEPTH_INFINITE, null);
        return result;
    }

    private static ChangeSet getOtherHeadInCurrentBranch(IProject project, DataLoader loader) throws HgException {
        ChangeSet[] heads = loader.getHeads();
        // have to be at least two heads total to do easy merge
        if (heads.length < 2)
            return null;
        
        ChangeSet currentRevision = LocalChangesetCache.getInstance().getCurrentWorkDirChangeset(project);
        String branch = currentRevision.getBranch();
        
        ChangeSet candidate = null;
        for (ChangeSet cs : heads) {
            // must match branch
            if (!cs.getBranch().equals(branch))
                continue;
            // can't be the current
            if (cs.equals(currentRevision))
                continue;
            // if we have more than one candidate, then have to ask user anyway.
            if (candidate != null)
                return null;
            candidate = cs;
        }
        

        return candidate;
    }
    
    /**
     * @return
     */
    private static boolean isHgResolveAvailable() throws HgException {
        return HgResolveClient.checkAvailable();
    }

}
