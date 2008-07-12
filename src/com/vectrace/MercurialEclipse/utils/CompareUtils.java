package com.vectrace.MercurialEclipse.utils;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.window.Window;

import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

@SuppressWarnings("restriction")
public class CompareUtils {

    public static void openEditor(IResource file, ChangeSet changeset,
            boolean dialog, boolean localEditable) {
        openEditor(file, new IStorageMercurialRevision(file, changeset
                .getChangesetIndex()
                + "", changeset.getChangeset(), changeset), dialog, localEditable);
    }

    public static void openEditor(IResource file, ChangeSet changeset, boolean localEditable) {
        openEditor(file, new IStorageMercurialRevision(file, changeset
                .getChangesetIndex()
                + "", changeset.getChangeset(), changeset), false, localEditable);
    }

    public static void openEditor(IResource file,
            IStorageMercurialRevision right, boolean dialog, boolean localEditable) {
        ResourceNode leftNode = null;
        if (file != null) {
            leftNode = new ResourceNode(file);
        }
        openEditor(leftNode, getNode(right), dialog, localEditable);
    }

    public static void openEditor(IStorageMercurialRevision left,
            IStorageMercurialRevision right, boolean dialog, boolean localEditable) {
        openEditor(getNode(left), getNode(right), dialog, localEditable);
    }

    public static void openEditor(ResourceNode left, ResourceNode right,
            boolean dialog, boolean localEditable) {
        Assert.isNotNull(right);

        if (dialog) {
            openCompareDialog(left, right, localEditable);
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
            boolean localEditable) {
        CompareEditorInput compareInput = getCompareInput(left, right,
                localEditable);
        return openCompareDialog(compareInput);
    }

    /**
     * Opens a compare dialog using the given input.
     * 
     * @param myShell
     * @param compareInput
     * @return
     */
    public static int openCompareDialog(CompareEditorInput compareInput) {
        CompareUI.openCompareDialog(compareInput);
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
