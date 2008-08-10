/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError 
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;

import com.vectrace.MercurialEclipse.commands.HgGLogClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.commands.HgSigsClient;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author zingo
 * 
 */
public class MercurialHistory extends FileHistory {

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    private static final class ChangeSetComparator implements
            Comparator<ChangeSet> {
        public int compare(ChangeSet o1, ChangeSet o2) {
            int result = o2.getChangesetIndex() - o1.getChangesetIndex();

            // we need to cover the situation when repo-indices are the same
            if (result == 0 && o1.getRealDate() != null
                    && o2.getRealDate() != null) {
                int dateCompare = o2.getRealDate().compareTo(o1.getRealDate());
                if (dateCompare != 0) {
                    result = dateCompare;
                }
            }

            return result;
        }
    }

    private static final class RevisionComparator implements
            Comparator<MercurialRevision> {
        public int compare(MercurialRevision o1, MercurialRevision o2) {
            int result = o2.getChangeSet().getChangesetIndex()
                    - o1.getChangeSet().getChangesetIndex();

            // we need to cover the situation when repo-indices are the same
            if (result == 0 && o1.getChangeSet().getRealDate() != null
                    && o2.getChangeSet().getRealDate() != null) {
                int dateCompare = o2.getChangeSet().getRealDate().compareTo(
                        o1.getChangeSet().getRealDate());
                if (dateCompare != 0) {
                    result = dateCompare;
                }
            }

            return result;
        }
    }

    private static ChangeSetComparator csComparator = null;
    private static RevisionComparator revComparator = null;

    private IResource resource;
    protected SortedSet<MercurialRevision> revisions;
    Map<Integer, GChangeSet> gChangeSets;
    private int bottom = 0;

    public MercurialHistory(IResource resource) {
        super();
        this.resource = resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.history.IFileHistory#getContributors(org.eclipse
     * .team.core.history.IFileRevision)
     */
    public IFileRevision[] getContributors(IFileRevision revision) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.history.IFileHistory#getFileRevision(java.lang.
     * String)
     */
    public IFileRevision getFileRevision(String id) {
        if (revisions == null || revisions.size() == 0) {
            return null;
        }

        for (MercurialRevision rev : revisions) {
            if (rev.getContentIdentifier().equals(id)) {
                return rev;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.history.IFileHistory#getFileRevisions()
     */
    public IFileRevision[] getFileRevisions() {
        return revisions.toArray(new MercurialRevision[revisions.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.team.core.history.IFileHistory#getTargets(org.eclipse.team
     * .core.history.IFileRevision)
     */
    public IFileRevision[] getTargets(IFileRevision revision) {
        return new IFileRevision[0];
    }

    public void refresh(IProgressMonitor monitor, int from)
            throws CoreException {
        RepositoryProvider provider = RepositoryProvider.getProvider(resource
                .getProject());
        if (provider != null && provider instanceof MercurialTeamProvider) {
            // We need these to be in order for the GChangeSets to display
            // properly

            SortedSet<ChangeSet> changeSets = new TreeSet<ChangeSet>(
                    getChangeSetComparator());

            int logBatchSize = Integer.parseInt(MercurialUtilities
                    .getPreference(MercurialPreferenceConstants.LOG_BATCH_SIZE,
                            "500"));

            // check if we have reached the bottom (initially = 0)
            if (from == this.bottom) {
                return;
            }
            Map<IPath, SortedSet<ChangeSet>> map = HgLogClient.getProjectLog(
                    resource, logBatchSize, from, false);

            // no result -> bottom reached
            if (map == null) {
                this.bottom = from;
                return;
            }

            // still changesets there -> process
            SortedSet<ChangeSet> localChangeSets = map.get(resource
                    .getLocation());
            if (localChangeSets != null) {
                // get signatures
                File file = resource.getLocation().toFile();
                List<Signature> sigs = HgSigsClient.getSigs(file);
                Map<String, Signature> sigMap = new HashMap<String, Signature>();
                for (Signature signature : sigs) {
                    sigMap.put(signature.getNodeId(), signature);
                }

                changeSets.addAll(localChangeSets);

                if (revisions == null || revisions.size() == 0
                        || !(revisions.first().getResource().equals(resource))) {
                    revisions = new TreeSet<MercurialRevision>(
                            getRevisionComparator());
                    List<GChangeSet> gLogChangeSets = new HgGLogClient(resource)
                            .update(changeSets).getChangeSets();
                    // put glog changesets in map for later referencing
                    gChangeSets = new HashMap<Integer, GChangeSet>(
                            gLogChangeSets.size());
                    for (GChangeSet gs : gLogChangeSets) {
                        if (gs != null) {
                            gChangeSets.put(Integer.valueOf(gs.getRev()), gs);
                        }
                    }
                }
                for (ChangeSet cs : changeSets) {
                    Signature sig = sigMap.get(cs.getChangeset());
                    revisions.add(new MercurialRevision(cs, gChangeSets
                            .get(Integer.valueOf(cs.getChangesetIndex())),
                            resource, sig));
                }
            }
        }
    }

    /**
     * @return
     */
    private ChangeSetComparator getChangeSetComparator() {
        if (csComparator == null) {
            csComparator = new ChangeSetComparator();
        }
        return csComparator;
    }

    private RevisionComparator getRevisionComparator() {
        if (revComparator == null) {
            revComparator = new RevisionComparator();
        }
        return revComparator;
    }

}
