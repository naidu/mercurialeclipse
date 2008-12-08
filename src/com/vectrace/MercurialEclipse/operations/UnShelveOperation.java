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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 * 
 */
public class UnShelveOperation extends HgOperation {
    private IProject project;

    /**
     * @param part
     */
    public UnShelveOperation(IWorkbenchPart part, IProject p) {
        super(part);
        this.project = p;
    }

    /**
     * @param context
     */
    public UnShelveOperation(IRunnableContext context, IProject p) {
        super(context);
        this.project = p;
    }

    /**
     * @param part
     * @param context
     */
    public UnShelveOperation(IWorkbenchPart part, IRunnableContext context,
            IProject p) {
        super(part, context);
        this.project = p;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.actions.HgOperation#getActionDescription()
     */
    @Override
    protected String getActionDescription() {
        return Messages.getString("UnShelveOperation.UnshelvingChanges"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.actions.HgOperation#run(org.eclipse.core
     * .runtime.IProgressMonitor)
     */
    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        try {
            // get modified files
            monitor.beginTask(Messages
                    .getString("UnShelveOperation.Unshelving"), 4); //$NON-NLS-1$
            monitor.subTask(Messages.getString("UnShelveOperation.GettingChanges")); //$NON-NLS-1$
            File root = HgClients.getHgRoot(project.getLocation().toFile());
            File shelveDir = new File(root, ".hg" + File.separator //$NON-NLS-1$
                    + "mercurialeclipse-shelve-backups"); //$NON-NLS-1$
            
            if (shelveDir.exists()) {
                File shelveFile = new File(shelveDir, project.getName().concat(
                        "-patchfile.patch")); //$NON-NLS-1$
                if (shelveFile.exists()) {
                    monitor.worked(1);
                    monitor.subTask(Messages.getString("UnShelveOperation.applyingChanges")); //$NON-NLS-1$
                    HgPatchClient.importPatch(project, shelveFile,
                            new ArrayList<String>(0));
                    monitor.worked(1);
                    monitor.subTask(Messages.getString("UnShelveOperation.emptyingShelf")); //$NON-NLS-1$
                    boolean deleted = shelveFile.delete();
                    monitor.worked(1);
                    monitor.subTask(Messages.getString("UnShelveOperation.refreshingProject")); //$NON-NLS-1$
                    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    monitor.worked(1);                    
                    if (!deleted) {
                        throw new HgException(shelveFile.getName()+" could not be deleted.");
                    }
                } else {
                    throw new HgException(
                            Messages.getString("UnShelveOperation.error.ShelfEmpty")); //$NON-NLS-1$
                }
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            throw new InvocationTargetException(e, e.getLocalizedMessage());
        } finally {
            monitor.done();
        }

    }

}
