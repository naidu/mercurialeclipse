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

import com.vectrace.MercurialEclipse.commands.HgGLogClient;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
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

    private static ChangeSetComparator comparator = null;
    private IResource resource;
    protected IFileRevision[] revisions;

    public MercurialHistory(IResource resource) {
        super();
        this.resource = resource;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.history.IFileHistory#getContributors(org.eclipse.team.core.history.IFileRevision)
     */
    public IFileRevision[] getContributors(IFileRevision revision) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.history.IFileHistory#getFileRevision(java.lang.String)
     */
    public IFileRevision getFileRevision(String id) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.history.IFileHistory#getFileRevisions()
     */
    public IFileRevision[] getFileRevisions() {
        return revisions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.team.core.history.IFileHistory#getTargets(org.eclipse.team.core.history.IFileRevision)
     */
    public IFileRevision[] getTargets(IFileRevision revision) {
        return null;
    }

    public void refresh(IProgressMonitor monitor) throws CoreException {
        RepositoryProvider provider = RepositoryProvider.getProvider(resource
                .getProject());
        if (provider != null && provider instanceof MercurialTeamProvider) {
            // We need these to be in order for the GChangeSets to display
            // properly

            SortedSet<ChangeSet> changeSets = new TreeSet<ChangeSet>(
                    getChangeSetComparator());

            LocalChangesetCache.getInstance().refreshAllLocalRevisions(
                    resource, false);

            SortedSet<ChangeSet> localChangeSets = LocalChangesetCache.getInstance().getLocalChangeSets(resource);

            if (localChangeSets != null) {
                changeSets.addAll(localChangeSets);
            }
            
//            // only get incoming if changesets are already there (e.g. via Sync Participant).
//            SortedSet<ChangeSet> incomingChangeSets = null;           
//            if (IncomingChangesetCache.getInstance().isIncomingStatusKnown(
//                    resource.getProject())) {
//                incomingChangeSets = IncomingChangesetCache.getInstance()
//                        .getIncomingChangeSets(resource);
//            }
            
            List<GChangeSet> gChangeSets = null;
//            if (incomingChangeSets != null && incomingChangeSets.size() > 0) {
//                changeSets.addAll(incomingChangeSets);
//               gChangeSets = new HgGLogClient(resource).update(changeSets)
//                            .getChangeSets();
//                
//            } else {
                gChangeSets = new HgGLogClient(resource).update(changeSets)
                        .getChangeSets();
//            }

            revisions = new IFileRevision[changeSets.size()];
            int i = 0;
            for (ChangeSet cs : changeSets) {
                revisions[i] = new MercurialRevision(cs, gChangeSets.get(i),
                        resource);
                i++;
            }
        }
    }

    /**
     * @return
     */
    private ChangeSetComparator getChangeSetComparator() {
        if (comparator == null) {
            comparator = new ChangeSetComparator();
        }
        return comparator;
    }

}
