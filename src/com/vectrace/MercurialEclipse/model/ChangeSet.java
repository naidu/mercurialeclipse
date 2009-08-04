/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Charles O'Farrell         - HgRevision
 *     Bastian Doetsch			 - some more info fields
 *******************************************************************************/

package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.compare.patch.IFilePatch;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class ChangeSet implements Comparable<ChangeSet> {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm Z");
    public static final Date UNKNOWN_DATE = new Date(0);
    public static enum Direction {
        INCOMING, OUTGOING, LOCAL;
    }

    private final HgRevision revision;
    private final int changesetIndex;
    private final String changeset;
    private final String branch;
    private final String user;
    private final String date;
    private String tag;
    private FileStatus[] changedFiles;
    private String description;
    private String ageDate;
    private String nodeShort;
    private String[] parents;
    private Date realDate;
    private File bundleFile;
    private HgRepositoryLocation repository;
    private Direction direction;
    private String summary;
    private HgRoot hgRoot;
    private IFilePatch[] patches;

    /**
     * This class is getting too tangled up with everything else, has a a large
     * amount of fields (17) and worse is that it is not immutable, which makes
     * the entanglement even more dangerous.
     *
     * My plan is to make it immutable by using the builder pattern and remove
     * all setters. FileStatus fetching may(or may not) be feasable to put
     * elsewhere or fetched "on-demand" by this class itself. Currently, it has
     * no operations and it purely a data class which isn't very OO efficent.
     *
     * Secondly, remove getDirection by tester methods (isIncoming, isOutgoing,
     * isLocal)
     *
     */

    public static class Builder {
        private ChangeSet cs;

        public Builder(int revision, String changeSet, String branch,
                String date, String user) {

            this.cs = new ChangeSet(revision, changeSet, user, date, branch == null? "" : branch);
        }

        public Builder tag(String tag) {
            this.cs.tag = tag;
            return this;
        }

        public Builder description(String description) {
            cs.setDescription(description);
            return this;
        }

        public Builder parents(String[] parents) {
            this.cs.setParents(parents);
            return this;
        }

        public Builder direction(Direction direction) {
            this.cs.direction = direction;
            return this;
        }

        public Builder changedFiles(FileStatus[] changedFiles) {
            this.cs.changedFiles = changedFiles;
            return this;
        }

        public Builder bundleFile(File bundleFile) {
            this.cs.bundleFile = bundleFile;
            return this;
        }

        public Builder repository(HgRepositoryLocation repository) {
            this.cs.repository = repository;
            return this;
        }

        public Builder hgRoot(HgRoot hgRoot) {
            this.cs.hgRoot = hgRoot;
            return this;
        }

        // what is ageDate? Can it be derived from date and now()
        public Builder ageDate(String ageDate) {
            this.cs.ageDate = ageDate;
            return this;
        }

        // nodeShort should be first X of changeset, this is superflous
        public Builder nodeShort(String nodeShort) {
            this.cs.nodeShort = nodeShort;
            return this;
        }


        public void patches(IFilePatch[] patches) {
            this.cs.patches = patches;
        }

        public ChangeSet build() {
            ChangeSet result = this.cs;
            this.cs = null;
            return result;
        }
    }

    private ChangeSet(int changesetIndex, String changeSet, String tag,
            String branch, String user, String date, String description,
            String[] parents) {
        this.changesetIndex = changesetIndex;
        this.changeset = changeSet;
        this.revision = new HgRevision(changeset, changesetIndex);
        this.tag = tag;
        this.branch = branch;
        this.user = user;
        this.date = date;
        setDescription(description);
        setParents(parents);
    }

    private ChangeSet(int changesetIndex, String changeSet, String user,
            String date, String branch) {
        this(changesetIndex, changeSet, null, branch, user, date, "", null); //$NON-NLS-1$
    }

    public int getChangesetIndex() {
        return changesetIndex;
    }

    public String getChangeset() {
        return changeset;
    }

    public String getTag() {
        if ("tip".equals(tag) && bundleFile != null) { //$NON-NLS-1$
            StringBuilder builder = new StringBuilder(tag).append(" [ ").append(repository.toString()).append(" ]"); //$NON-NLS-1$ //$NON-NLS-2$
            tag = builder.toString();
        }
        return tag;
    }

    public String getBranch() {
        return branch;
    }

    public String getUser() {
        return user;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public HgRevision getRevision() {
        return revision;
    }

    @Override
    public String toString() {
        if (nodeShort != null) {
            return this.changesetIndex + ":" + this.nodeShort; //$NON-NLS-1$
        }
        return this.changesetIndex + ":" + this.changeset; //$NON-NLS-1$

    }

    /**
     * @return the changedFiles
     */
    public FileStatus[] getChangedFiles() {
        if (changedFiles != null) {
            // Don't let clients manipulate the array in-place
            return changedFiles.clone();
        }
        return new FileStatus[0];
    }

    /**
     * @return the ageDate
     */
    public String getAgeDate() {
        return ageDate;
    }

    /**
     * @return the nodeShort
     */
    public String getNodeShort() {
        return nodeShort;
    }

    public int compareTo(ChangeSet o) {
        if (o.getChangeset().equals(this.getChangeset())) {
            return 0;
        }
        if (getRealDate() != UNKNOWN_DATE && o.getRealDate() != UNKNOWN_DATE) {
            int dateCompare = getRealDate().compareTo(o.getRealDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
        }
        return this.getChangesetIndex() - o.getChangesetIndex();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChangeSet) {
            ChangeSet other = (ChangeSet) obj;
            if(getChangeset().equals(other.getChangeset())){
                return true;
            }
            if (date != null && date.equals(other.getDate())) {
                return true;
            }
            return getChangesetIndex() == other.getChangesetIndex();
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
        + ((changeset == null) ? 0 : changeset.hashCode());
        return result;
    }

    /**
     * @return never returns null. Returns {@link ChangeSet#UNKNOWN_DATE} if the date
     * can't be parsed
     */
    public Date getRealDate() {
        try {
            if (realDate == null) {
                if (date != null) {
                    realDate = SIMPLE_DATE_FORMAT.parse(date);
                } else {
                    realDate = UNKNOWN_DATE;
                }
            }
        } catch (ParseException e) {
            realDate = UNKNOWN_DATE;
        }
        return realDate;
    }

    /**
     * @return the bundleFile
     */
    public File getBundleFile() {
        return bundleFile;
    }

    public String[] getParents() {
        return parents;
    }

    private void setParents(String[] parents) {
        // filter null parents (hg uses -1 to signify a null parent)
        if (parents != null) {
            List<String> temp = new ArrayList<String>(parents.length);
            for (int i = 0; i < parents.length; i++) {
                String parent = parents[i];
                if (parent.charAt(0) != '-') {
                    temp.add(parent);
                }
            }
            this.parents = temp.toArray(new String[temp.size()]);
        }
    }

    private void setDescription(String description) {
        if (description != null) {
            int i = description.indexOf('\n');
            if (i > 0) {
                this.summary = description.substring(0, i >= 0 ? i
                        : description.length());
            } else {
                this.summary = description;
            }
            this.description = description;
        }
    }

    public String getSummary() {
        return summary;
    }

    /**
     * @return the repository
     */
    public HgRepositoryLocation getRepository() {
        return repository;
    }

    /**
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * @return the hgRoot file (always as <b>canonical path</b>)
     * @see File#getCanonicalPath()
     */
    public HgRoot getHgRoot() {
        return hgRoot;
    }

    /**
     * @return the patch
     */
    public IFilePatch[] getPatches() {
        return patches;
    }
}
