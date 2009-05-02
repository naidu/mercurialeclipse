package com.vectrace.MercurialEclipse.team;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class ActionCommit implements IWorkbenchWindowActionDelegate {

    private IWorkbenchWindow window;
    // private IWorkbenchPart targetPart;
    private IStructuredSelection selection;

    public ActionCommit() {
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
        // System.out.println("ActionRemove:init(window)");
        this.window = w;
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */

    @SuppressWarnings("unchecked")
    public void run(IAction action) {
        IProject proj;
        String Repository;
        // String FullPath;
        Shell shell;
        IWorkbench workbench;

        proj = MercurialUtilities.getProject(selection);
        Repository = MercurialUtilities.getRepositoryPath(proj);
        if (Repository == null) {
            Repository = "."; //never leave this empty add a . to point to current path //$NON-NLS-1$
        }

        // Get shell & workbench
        if ((window != null) && (window.getShell() != null)) {
            shell = window.getShell();
        } else {
            workbench = PlatformUI.getWorkbench();
            shell = workbench.getActiveWorkbenchWindow().getShell();
        }
        
        if (selection.getFirstElement() instanceof IResource) {
            IResource firstElement = (IResource)selection.getFirstElement();
            HgRoot root;
            try {
                root = MercurialTeamProvider.getHgRoot(firstElement);
            } catch (HgException e) {
                return;
            }
            new CommitDialog(shell, root, Arrays.asList(firstElement)).open();
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
