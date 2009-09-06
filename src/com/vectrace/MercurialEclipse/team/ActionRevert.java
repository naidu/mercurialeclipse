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
 *     Charles O'Farrell         - fix revert open file
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.dialogs.RevertDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class ActionRevert implements IWorkbenchWindowActionDelegate {
    private IWorkbenchWindow window;
    private IStructuredSelection selection;

    public ActionRevert() {
        super();
    }

    /**
     * We can use this method to dispose of any system resources we previously
     * allocated.
     *
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }

    /**
     * We will cache window object in order to be able to provide parent shell
     * for the message dialog.
     *
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow w) {
        this.window = w;
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     *
     * @see IWorkbenchWindowActionDelegate#run
     */

    public void run(IAction action) {
        if(window == null){
            window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        }
        List<IResource> resources = new ArrayList<IResource>();
        boolean mergeIsRunning;
        try {
            mergeIsRunning = collectResourcesToRevert(resources);
        } catch (CoreException e) {
            MercurialEclipsePlugin.logError(e);
            MercurialEclipsePlugin.showError(e);
            return;
        }
        if (resources.size() > 0 && !mergeIsRunning) {
            openRevertDialog(resources, false);
        } else {
            if(mergeIsRunning){
                boolean doRevert = MessageDialog
                    .openConfirm(
                        getShell(),
                        Messages.getString("ActionRevert.HgRevert"), //$NON-NLS-1$
                        Messages.getString("ActionRevert.mergeIsRunning")); //$NON-NLS-1$
                if(doRevert){
                    openRevertDialog(resources, true);
                }
            } else {
                MessageDialog
                    .openInformation(
                        getShell(),
                        Messages.getString("ActionRevert.HgRevert"), //$NON-NLS-1$
                        Messages.getString("ActionRevert.noFilesToRevert")); //$NON-NLS-1$
            }
        }
    }

    private Shell getShell() {
        Shell shell;
        // Get shell
        if (window != null && window.getShell() != null) {
            shell = window.getShell();
        } else {
            shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        }
        return shell;
    }

    private void openRevertDialog(List<IResource> resources, final boolean cleanAfterMerge) {
        final List<IResource> result;
        final List<IResource> untracked;
        if(!cleanAfterMerge){
            RevertDialog chooser = new RevertDialog(Display.getCurrent().getActiveShell());
            chooser.setFiles(resources);
            if (chooser.open() != Window.OK) {
                return;
            }
            result = chooser.getSelectionForHgRevert();
            untracked = chooser.getUntrackedSelection();
        } else {
            result = resources;
            // TODO allow to revert untracked files after the merge too
            untracked = new ArrayList<IResource>();
        }

        new SafeWorkspaceJob(Messages.getString("ActionRevert.revertFiles")) { //$NON-NLS-1$
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    doRevert(monitor, result, untracked, cleanAfterMerge);
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    return e.getStatus();
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private boolean collectResourcesToRevert(List<IResource> resources) throws CoreException, HgException {
        boolean mergeIsRunning = false;
        for (Object obj : selection.toList()) {
            if (obj instanceof IResource) {
                IResource resource = (IResource) obj;
                boolean merging = resource.getProject()
                        .getPersistentProperty(ResourceProperties.MERGING) != null;
                if(merging){
                    mergeIsRunning = true;
                }
                boolean supervised = MercurialUtilities.hgIsTeamProviderFor(resource, false);

                if (supervised) {
                    resources.add(resource);
                }
            }
        }
        return mergeIsRunning;
    }

    public void doRevert(IProgressMonitor monitor, List<IResource> resources,
            List<IResource> untracked, boolean cleanAfterMerge) throws HgException {

        monitor.beginTask(Messages.getString("ActionRevert.revertingResources"), resources.size() * 2); //$NON-NLS-1$

        Map<IProject, List<IResource>> filesToRevert = ResourceUtils.groupByProject(resources);
        Set<IProject> projects = filesToRevert.keySet();

        // cleanup untracked files first
        deleteUntrackedFiles(untracked, monitor);

        // collect removed file state NOW
        MercurialStatusCache cache = MercurialStatusCache.getInstance();

        Map<IProject, Set<IResource>> removedFilesBefore = getFiles(MercurialStatusCache.BIT_REMOVED, projects);
        Map<IProject, Set<IResource>> addedFilesBefore = getFiles(MercurialStatusCache.BIT_ADDED,
                removedFilesBefore.keySet());

        // keep track only for projects where both add AND remove is reported
        removedFilesBefore.keySet().retainAll(addedFilesBefore.keySet());

        // perform revert
        if(cleanAfterMerge) {
            for (Entry<IProject, List<IResource>> entry : filesToRevert.entrySet()) {
                performRevertAfterMerge(monitor, entry.getKey(), entry.getValue());
            }
        } else {
            Map<HgRoot, List<IResource>> rootToFiles = ResourceUtils.groupByRoot(resources);
            for (Entry<HgRoot, List<IResource>> entry : rootToFiles.entrySet()) {
                performRegularRevert(monitor, entry.getKey(), entry.getValue());
            }
        }

        for (IResource resource : resources) {
            monitor.subTask(Messages.getString("ActionRevert.refreshing") + resource.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
            try {
                if(cache.isAdded(ResourceUtils.getPath(resource))){
                    // added files didn't change content after we un-add them, so we have
                    // give Eclipse a hint to start some extra refresh work.
                    resource.touch(monitor);
                }
                // we still need to trigger a refresh to avoid confusing editors opened on
                // these files. Without refresh, they complain that the files are changed but not refreshed
                resource.refreshLocal(IResource.DEPTH_ONE, monitor);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
            monitor.worked(1);
        }

        for (Entry<IProject, Set<IResource>> entry : removedFilesBefore.entrySet()) {
            cache.refreshStatus(entry.getKey(), monitor);
        }

        // we are looking for files, which are NOT in the "reverted" files list
        // and which are NOT marked as deleted in the latest cached state
        // BUT which was marked as deleted before the revert operation.

        // These "deleted before, do not reverted and missing in the status now" files are
        // the source files recreated after the revert of a "move (rm + add)" operation.
        // We have to tell Eclipse, that the files are "living" again in the resources tree

        Map<IProject, Set<IResource>> filesToUpdate = new HashMap<IProject, Set<IResource>>();
        Map<IProject, Set<IResource>> removedFilesAfter = getFiles(
                MercurialStatusCache.BIT_REMOVED, removedFilesBefore.keySet());

        for (Entry<IProject, Set<IResource>> entry : removedFilesBefore.entrySet()) {
            IProject projBefore = entry.getKey();
            Set<IResource> removedBefore = entry.getValue();
            Set<IResource> removedAfter = removedFilesAfter.get(projBefore);
            if(removedAfter != null && !removedAfter.isEmpty()) {
                removedBefore.removeAll(removedAfter);
            }
            List<IResource> reverted = filesToRevert.get(projBefore);
            if(reverted != null && !reverted.isEmpty()) {
                removedBefore.removeAll(reverted);
            }
            if(!removedBefore.isEmpty()){
                filesToUpdate.put(projBefore, removedBefore);
            }
        }

        for (Entry<IProject, Set<IResource>> entry : filesToUpdate.entrySet()) {
            Set<IResource> removed = entry.getValue();
            for (IResource resource : removed) {
                try {
                    resource.refreshLocal(IResource.DEPTH_ONE, monitor);
                } catch (CoreException e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }
        }
        monitor.done();
    }

    /**
     *  Deletes given resources and cleans up the cache state for them
     */
    private void deleteUntrackedFiles(List<IResource> untracked, IProgressMonitor monitor) {
        MercurialStatusCache cache = MercurialStatusCache.getInstance();
        for (IResource resource : untracked) {
            try {
                IContainer parent = null;
                if(resource instanceof IFile){
                    parent = resource.getParent();
                    if(parent instanceof IProject && ".project".equals(resource.getName())){
                        MercurialEclipsePlugin.logInfo(
                                "Will NOT delete .project file from project " + parent.getName(),
                                null);
                        // do not revert .project file....
                        continue;
                    }
                    resource.delete(IResource.FORCE | IResource.KEEP_HISTORY, monitor);
                } else if(!(resource instanceof IProject)){
                    resource.delete(IResource.KEEP_HISTORY, monitor);
                }
                deleteEmptyDirs(parent, monitor);
                cache.clearStatusCache(resource);
            } catch (CoreException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
    }

    /**
     *  Recursive deletes empty directories, starting with given one
     */
    private void deleteEmptyDirs(IContainer dir, IProgressMonitor monitor) throws CoreException {
        int memberFlags = IContainer.INCLUDE_HIDDEN | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS;
        if (dir != null && !(dir instanceof IProject) && dir.members(memberFlags).length == 0) {
            IContainer parent = dir.getParent();
            dir.delete(false, monitor);
            deleteEmptyDirs(parent, monitor);
        }
    }

    /**
     * @param projects
     * @param statusBit one of {@link MercurialStatusCache} status bits
     * @return a map where the files with the specified state are grouped by the project.
     * Projects with no files of given state are not included into the map
     */
    private Map<IProject, Set<IResource>> getFiles(int statusBit, Set<IProject> projects) {
        MercurialStatusCache cache = MercurialStatusCache.getInstance();
        Map<IProject, Set<IResource>> resources = new HashMap<IProject, Set<IResource>>();
        for (IProject project : projects) {
            Set<IResource> removed = cache.getResources(statusBit, project);
            if(!removed.isEmpty()) {
                resources.put(project, removed);
            }
        }
        return resources;
    }

    private void performRegularRevert(IProgressMonitor monitor, HgRoot root, List<IResource> resources) throws HgException {
        // the last argument will be replaced with a path
        HgCommand command = new HgCommand("revert", root, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.UPDATE_TIMEOUT);
        command.addOptions("--no-backup");
        command.addFiles(resources);
        monitor.subTask(Messages.getString("ActionRevert.reverting") + root.getName() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
        command.executeToString();
        monitor.worked(1);
    }

    private void performRevertAfterMerge(IProgressMonitor monitor, IProject root, List<IResource> resources) {
        // see http://mercurial.selenic.com/wiki/FAQ#FAQ.2BAC8-CommonProblems.hg_status_shows_changed_files_but_hg_diff_doesn.27t.21
        // To completely undo the uncommitted merge and discard all local modifications,
        // you will need to issue a hg update -C -r . (note the "dot" at the end of the command).
        try {
            UpdateHandler update = new UpdateHandler();
            update.setCleanEnabled(true);
            update.setRevision(".");
            update.setShell(getShell());
            update.run(root);
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }

    }

    /**
     * Selection in the workbench has been changed. We can change the state of
     * the 'real' action here if we want, but this can only happen after the
     * delegate has been created.
     *
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection in_selection) {
        if (in_selection != null
                && in_selection instanceof IStructuredSelection) {
            selection = (IStructuredSelection) in_selection;
        }
    }

}
