package com.vectrace.MercurialEclipse.utils;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.internal.CompareDialog;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

@SuppressWarnings("restriction")
public class CompareUtils {

    public static void openEditor(IResource file, ChangeSet changeset,
            boolean dialog) {
        openEditor(file, new IStorageMercurialRevision(file, changeset
                .getChangesetIndex()
                + "", changeset.getChangeset(), changeset), dialog);
    }

    public static void openEditor(IResource file, ChangeSet changeset) {
        openEditor(file, new IStorageMercurialRevision(file, changeset
                .getChangesetIndex()
                + "", changeset.getChangeset(), changeset), false);
    }

    public static void openEditor(IResource file,
            IStorageMercurialRevision right, boolean dialog) {
        ResourceNode leftNode = null;
        if (file != null) {
            leftNode = new ResourceNode(file);
        }
        openEditor(leftNode, getNode(right), dialog, null, false);
    }

    public static void openEditor(IStorageMercurialRevision left,
            IStorageMercurialRevision right, boolean dialog) {
        openEditor(getNode(left), getNode(right), dialog, null, false);
    }

    public static void openEditor(ResourceNode left, ResourceNode right,
            boolean dialog, Shell shell, boolean localEditable) {
        Assert.isNotNull(right);

        if (dialog) {
            openCompareDialog(left, right, shell, localEditable);
        } else {
            CompareEditorInput compareInput = getCompareInput(left, right,
                    localEditable);
            if (compareInput != null) {
                CompareUI.openCompareEditor(compareInput);
            }
        }
    }

    /**
     * Opens a compare dialog and returns it. Unfortunately the dialog is
     * internal Eclipse API, so it might change in future Eclipse versions.
     * 
     * @param left
     *            the left ResourceNode to determine the compare editor input
     * @param right
     *            the right ResourceNode to determine the compare editor input
     * @param shell
     *            the shell to use for opening the dialog
     */
    public static int openCompareDialog(ResourceNode left, ResourceNode right,
            Shell shell, boolean localEditable) {
        Shell myShell = shell;
        if (shell == null) {
            myShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
        }
        CompareEditorInput compareInput = getCompareInput(left, right,
                localEditable);
        return openCompareDialog(myShell, compareInput);
    }

    /**
     * Opens a compare dialog using the given input. This method uses 
     * internal Eclipse API, so it might break in future revisions.
     * @param myShell
     * @param compareInput
     * @return
     */
    public static int openCompareDialog(Shell myShell,
            CompareEditorInput compareInput) {
        if (CompareUIPlugin.getDefault().compareResultOK(compareInput, null)) {
            CompareDialog dialog = new CompareDialog(myShell, compareInput);
            return dialog.open();
        }
        return Window.CANCEL;
    }

    public static CompareEditorInput getCompareInput(ResourceNode left,
            ResourceNode right, boolean localEditable) {
        IResource resource = right.getResource();
        ResourceNode leftNode;

        // switch left to right if left is null and put local to left
        if (left != null) {
            leftNode = left;
        } else {
            leftNode = right;
        }
        ResourceNode rightNode;
        if (left != null) {
            rightNode = right;
        } else {
            rightNode = new ResourceNode(resource);
        }

        return new HgCompareEditorInput(new CompareConfiguration(), resource,
                leftNode, rightNode, localEditable);
    }

    private static RevisionNode getNode(IStorageMercurialRevision rev) {
        return rev == null ? null : new RevisionNode(rev);
    }

}
