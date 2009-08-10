/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgAtticClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

/**
 * @author bastian
 *
 */
public class ShelveOperation extends HgOperation {
    private final IProject project;

    /**
     * @param part
     */
    public ShelveOperation(IWorkbenchPart part, IProject p) {
        super(part);
        this.project = p;
    }

    /**
     * @param context
     */
    public ShelveOperation(IRunnableContext context, IProject p) {
        super(context);
        this.project = p;
    }

    /**
     * @param part
     * @param context
     */
    public ShelveOperation(IWorkbenchPart part, IRunnableContext context,
            IProject p) {
        super(part, context);
        this.project = p;
    }

    @Override
    protected String getActionDescription() {
        return Messages.getString("ShelveOperation.shelvingChanges"); //$NON-NLS-1$
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        try {
            // get modified files
            monitor
                    .beginTask(
                            Messages.getString("ShelveOperation.shelving"), 5); //$NON-NLS-1$
            // check if hg > 1.0x and hgattic is available
            if (MercurialUtilities.isCommandAvailable("resolve", // $NON-NLS-1$
                    ResourceProperties.RESOLVE_AVAILABLE, "") // $NON-NLS-1$
                    && MercurialUtilities.isCommandAvailable("attic-shelve",// $NON-NLS-1$
                            ResourceProperties.EXT_HGATTIC_AVAILABLE, "")) { // $NON-NLS-1$

                String output = HgAtticClient.shelve(project.getLocation().toFile(),
                        "MercurialEclipse shelve operation", // $NON-NLS-1$
                        true, MercurialUtilities.getHGUsername(),
                        project.getName());
                monitor.worked(1);
                project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                monitor.worked(1);
                HgClients.getConsole().printMessage(output, null);
            } else {

                monitor.subTask(Messages
                        .getString("ShelveOperation.determiningChanges")); //$NON-NLS-1$
                String[] dirtyFiles = HgStatusClient.getDirtyFiles(project
                        .getLocation().toFile());
                List<IResource> resources = new ArrayList<IResource>();
                HgRoot root = HgClients.getHgRoot(project.getLocation().toFile());
                for (String f : dirtyFiles) {
                    IResource r = MercurialStatusCache.getInstance()
                            .convertRepoRelPath(root, project, f.substring(2));
                    if (r.exists()) {
                        resources.add(r);
                    }
                }
                if (resources.size() == 0) {
                    throw new HgException(Messages
                            .getString("ShelveOperation.error.nothingToShelve")); //$NON-NLS-1$
                }
                monitor.worked(1);
                monitor.subTask(Messages
                        .getString("ShelveOperation.shelvingChanges")); //$NON-NLS-1$

                File shelveDir = new File(root, ".hg" + File.separator //$NON-NLS-1$
                        + "mercurialeclipse-shelve-backups"); //$NON-NLS-1$
                shelveDir.mkdir();
                File shelveFile = new File(shelveDir, project.getName().concat(
                        "-patchfile.patch")); //$NON-NLS-1$
                if (shelveFile.exists()) {
                    throw new HgException(Messages
                            .getString("ShelveOperation.error.shelfNotEmpty")); //$NON-NLS-1$
                }
                HgPatchClient.exportPatch(root, resources, shelveFile,
                        new ArrayList<String>(0));
                monitor.worked(1);
                monitor
                        .subTask(Messages
                                .getString("ShelveOperation.determiningCurrentChangeset")); //$NON-NLS-1$
                String currRev = HgIdentClient.getCurrentChangesetId(root);
                monitor.worked(1);
                monitor.subTask(Messages
                        .getString("ShelveOperation.cleaningDirtyFiles")); //$NON-NLS-1$
                HgUpdateClient.update(project, currRev, true);
                monitor.worked(1);
                monitor.subTask(Messages
                        .getString("ShelveOperation.refreshingResources")); //$NON-NLS-1$
                for (IResource resource : resources) {
                    resource.refreshLocal(IResource.DEPTH_ZERO, monitor);
                }
                monitor.worked(1);
            }
        } catch (Exception e) {
            throw new InvocationTargetException(e, e.getLocalizedMessage());
        } finally {
            monitor.done();
        }

    }

}
