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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgGLogClient;
import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author zingo
 * 
 */
public class MercurialHistory extends FileHistory {

    private static final class ChangeSetComparator implements
            Comparator<ChangeSet> {
        public int compare(ChangeSet o1, ChangeSet o2) {
            int result = o2.getChangesetIndex() - o1.getChangesetIndex();

            // we need to cover the situation when repo-indices are the same
            if (result == 0 && o1.getRealDate() != null
                    && o2.getRealDate() != null) {
                int dateCompare = o1.getRealDate().compareTo(o2.getRealDate());
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
                int dateCompare = o1.getChangeSet().getRealDate().compareTo(
                        o2.getChangeSet().getRealDate());
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
    private List<GChangeSet> gLogChangeSets;

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
        try {
            if (revision instanceof MercurialRevision) {
                MercurialRevision rev = (MercurialRevision) revision;
                String[] parents = rev.getChangeSet().getParents();
                List<MercurialRevision> parentList = new ArrayList<MercurialRevision>(
                        2);
                for (String parent : parents) {
                    ChangeSet parentCs = LocalChangesetCache.getInstance()
                            .getLocalChangeSet(rev.getResource(), parent);

                    if (parentCs != null) {
                        for (GChangeSet gcs : gLogChangeSets) {
                            if (gcs.getIndex() == parentCs.getChangesetIndex()) {
                                parentList.add(new MercurialRevision(parentCs,
                                        null, resource));
                                break;
                            }
                        }

                    }
                }
                return parentList.toArray(new MercurialRevision[parentList
                        .size()]);
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
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

            SortedSet<ChangeSet> localChangeSets = HgLogClient.getProjectLog(
                    resource, logBatchSize, from, false).get(
                    resource.getLocation());

            if (localChangeSets != null) {
                changeSets.addAll(localChangeSets);
            }

            if (revisions == null || revisions.size() == 0
                    || !(revisions.first().getResource().equals(resource))) {
                revisions = new TreeSet<MercurialRevision>(
                        getRevisionComparator());
                gLogChangeSets = new HgGLogClient(resource).update(changeSets)
                        .getChangeSets();
            }

            for (ChangeSet cs : changeSets) {
                revisions.add(new MercurialRevision(cs, gLogChangeSets
                        .get((gLogChangeSets.size() - 1)
                                - cs
                        .getChangesetIndex()),
                        resource));
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
