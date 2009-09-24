package com.vectrace.MercurialEclipse.commands.extensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet.RowCount;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgGLogClient extends HgCommand {
    private final List<GChangeSet> sets = new ArrayList<GChangeSet>();

    public HgGLogClient(IResource resource) throws HgException {
        this(resource, -1, -1);
    }

    public HgGLogClient(IResource resource, int batchSize, int startRev) throws HgException {
        super("glog", ResourceUtils.getFirstExistingDirectory(ResourceUtils.getFileHandle(resource)), false);
        addOptions("--config", "extensions.graphlog="); //$NON-NLS-1$ //$NON-NLS-2$
        addOptions("--template", "*{rev}\\n"); // Removes everything //$NON-NLS-1$ //$NON-NLS-2$
        // Do not limit for files: glog limit is counted based on repository revision.
        if (resource.getType() == IResource.PROJECT) {
            addOptions("--limit", (batchSize > 0) ? String.valueOf(batchSize) + "" : HgLogClient.NOLIMIT); //$NON-NLS-1$
        }
        if (startRev >= 0 && startRev != Integer.MAX_VALUE) {
            int last = Math.max(startRev - batchSize, 0);
            addOptions("-r");
            addOptions(startRev + ":" + last);
        }

        if (resource.getType() != IResource.PROJECT) {
           addOptions(ResourceUtils.getFileHandle(resource).getAbsolutePath());
        }
        setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        load(executeToString());
    }

    protected HgGLogClient(String text) {
        super("", true); //$NON-NLS-1$
        load(text);
    }

    public void load(String s) {
        String[] split = s.split("\n"); //$NON-NLS-1$
        // real changeset count as glog inserts a line between two changesets
        int length = split.length / 2;
        int lengthp1 = length;
        RowCount rowCount = new RowCount();
        GChangeSet last = null;
        for (int i = 0; i < lengthp1; i++) {
            // adjust index for spacing
            int changeset = i * 2;
            // add current changeset and next line
            sets.add(last = new GChangeSet(rowCount, i, split[changeset],
                    i != length ? split[changeset + 1] : "").clean(last)); //$NON-NLS-1$
        }
    }

    public List<GChangeSet> getChangeSets() {
        return sets;
    }

    // TODO This is a (very) temporary fix to ensure we have enough elements
    public HgGLogClient update(Collection<ChangeSet> changeSets) {
        int diff = changeSets.size() - sets.size();
        if (diff > 0) {
            sets.addAll(Collections.nCopies(diff, (GChangeSet) null));
        }
        return this;
    }
}
