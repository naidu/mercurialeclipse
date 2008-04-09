package com.vectrace.MercurialEclipse.utils;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;

import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

public class CompareUtils {

    public static void openEditor(IResource file, boolean dialog) {
        openEditor(null, new IStorageMercurialRevision(file), dialog);
    }

    public static void openEditor(IResource file, String changeset) {
        openEditor(null, new IStorageMercurialRevision(file, changeset), false);
    }

    public static void openEditor(IStorageMercurialRevision left, IStorageMercurialRevision right,
            boolean dialog) {
        openEditor(getNode(left), getNode(right), dialog);
    }

    public static void openEditor(ResourceNode left, ResourceNode right, boolean dialog) {
        Assert.isNotNull(right);
        CompareEditorInput compareInput = getCompareInput(left, right);
        if (compareInput != null) {
            if (dialog) {
                CompareUI.openCompareDialog(compareInput);
            } else {
                CompareUI.openCompareEditor(compareInput);
            }
        }
    }

    public static CompareEditorInput getCompareInput(ResourceNode left, ResourceNode right) {
        IResource resource = right.getResource();
        return new HgCompareEditorInput(
                new CompareConfiguration(),
                resource,
                left != null ? left : right,
                left != null ? right : new ResourceNode(resource));
    }

    private static RevisionNode getNode(IStorageMercurialRevision rev) {
        return rev == null ? null : new RevisionNode(rev);
    }

}
