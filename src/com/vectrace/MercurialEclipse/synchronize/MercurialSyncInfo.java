/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.BitSet;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;

/**
 * @author bastian
 * 
 */
public class MercurialSyncInfo extends SyncInfo {
    private String branchName;

    // private final static Differencer DIFFERENCER = new Differencer();

    /**
     * @param local
     * @param base
     * @param remote
     * @param comparator
     */
    public MercurialSyncInfo(IResource local, IResourceVariant base,
            IResourceVariant remote, IResourceVariantComparator comparator,
            String branchName) {
        super(local, base, remote, comparator);
        this.branchName = branchName;
    }

    @Override
    protected int calculateKind() throws TeamException {
        int description = IN_SYNC;
        IResourceVariantComparator comparator = getComparator();
        IResource local = getLocal();
        MercurialResourceVariant outgoing = (MercurialResourceVariant) getBase();
        MercurialResourceVariant incoming = (MercurialResourceVariant) getRemote();

        SortedSet<ChangeSet> oChangeSets = null;
        if (outgoing != null) {
            oChangeSets = OutgoingChangesetCache.getInstance()
                    .getOutgoingChangeSets(local,
                            outgoing.getRev().getChangeSet().getRepository());
        }

        SortedSet<ChangeSet> iChangeSets = null;
        if (incoming != null) {
            iChangeSets = IncomingChangesetCache.getInstance()
                    .getIncomingChangeSets(local,
                            incoming.getRev().getChangeSet().getRepository());
        }
        boolean localExists = local.exists();
        int status = MercurialStatusCache.BIT_CLEAN;
        BitSet bitSet = MercurialStatusCache.getInstance().getStatus(local);
        if (bitSet != null) {
            status = bitSet.length() - 1;
        }
        if (oChangeSets == null) {
            if (iChangeSets == null) {
                if (comparator.compare(local, incoming)) {
                    description = IN_SYNC;
                } else {

                    if (status == MercurialStatusCache.BIT_ADDED
                            || status == MercurialStatusCache.BIT_UNKNOWN) {
                        description = OUTGOING | ADDITION;
                    } else if (status == MercurialStatusCache.BIT_REMOVED) {
                        description = OUTGOING | DELETION;
                    } else if (status == MercurialStatusCache.BIT_CLEAN) {
                        description = IN_SYNC;
                    } else {
                        description = OUTGOING | CHANGE;
                    }
                }
            } else {
                description = IN_SYNC;
                Action highestAction = null;
                for (ChangeSet changeSet : iChangeSets) {
                    FileStatus[] fStatus = changeSet.getChangedFiles();
                    for (FileStatus fileStatus : fStatus) {
                        if (local.getLocation().toOSString().endsWith(
                                fileStatus.getPath())
                                && branchName.equals(changeSet.getBranch())) {
                            if (fileStatus.getAction() != Action.MODIFIED) {
                                highestAction = fileStatus.getAction();
                            } else {
                                highestAction = Action.MODIFIED;
                            }
                            break;
                        }
                    }
                }
                if (highestAction == null) {
                    description = IN_SYNC; // different branch
                } else if (!localExists) {
                    if (highestAction == Action.REMOVED) {
                        description = IN_SYNC;
                    } else {
                        description = INCOMING | ADDITION;
                    }
                } else {
                    if (status == MercurialStatusCache.BIT_CLEAN) {
                        if (highestAction == Action.ADDED) {
                            description = IN_SYNC;
                        } else if (highestAction == Action.MODIFIED) {
                            description = INCOMING | CHANGE;
                        } else {
                            description = INCOMING | DELETION;
                        }
                    } else {
                        description = CONFLICTING | CHANGE;
                    }
                }

            }
        } else {
            Action highestAction = null;
            for (ChangeSet changeSet : oChangeSets) {
                FileStatus[] fStatus = changeSet.getChangedFiles();
                for (FileStatus fileStatus : fStatus) {
                    if (local.getLocation().toOSString().endsWith(
                            fileStatus.getPath())
                            && branchName.equals(changeSet.getBranch())) {
                        if (fileStatus.getAction() != Action.MODIFIED) {
                            highestAction = fileStatus.getAction();
                        } else {
                            highestAction = Action.MODIFIED;
                        }
                        break;
                    }
                }
            }
            if (highestAction == null) {
                description = IN_SYNC; // different branch
            } else if (iChangeSets == null) {
                if (highestAction == FileStatus.Action.MODIFIED) {
                    description = OUTGOING | CHANGE;
                } else if (highestAction == FileStatus.Action.ADDED) {
                    description = OUTGOING | ADDITION;
                } else {
                    description = OUTGOING | DELETION;
                }

            } else {
                // check for branches in incoming. if different branch => desc
                // IN_SYNC
                description = IN_SYNC;
                for (ChangeSet changeSet : iChangeSets) {
                    if (changeSet.getBranch().equals(branchName)) {
                        description = CONFLICTING | CHANGE;
                        break;
                    }
                }
            }
        }

        if (description == (CONFLICTING | CHANGE)) {
            // add resource conflict to status cache
            MercurialStatusCache.getInstance().addConflict(getLocal());
        }
        return description;
    }

}
