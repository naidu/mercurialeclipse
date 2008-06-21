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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class ChangeSet implements Comparable<ChangeSet> {
    public static enum Direction {
        INCOMING, OUTGOING, LOCAL;
    }

    private int changesetIndex;
    private String changeset;
    private String tag;
    private String branch;
    private String user;
    private String date;
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
    private File hgRoot;

    public ChangeSet() {
        super();
    }

    public ChangeSet(int changesetIndex, String changeSet, String tag,
            String branch, String user, String date, String description, String[] parents) {
        this.changesetIndex = changesetIndex;
        this.changeset = changeSet;
        this.tag = tag;
        this.branch = branch;
        this.user = user;
        this.date = date;
        setDescription(description);
        setParents(parents);
        try {
            if (date != null) {
                this.realDate = new SimpleDateFormat("yyyy-MM-dd hh:mm Z")
                        .parse(date);
            }
        } catch (Exception e) {
            this.realDate = null;
        }
    }

    public ChangeSet(int changesetIndex, String changeSet, String user,
            String date, String branch) {
        this(changesetIndex, changeSet, null, branch, user, date, "", null);
    }

    public int getChangesetIndex() {
        return changesetIndex;
    }

    public String getChangeset() {
        return changeset;
    }

    public String getTag() {
        if (tag != null && tag.equals("tip") && bundleFile != null) {
            tag = tag.concat(" [ ").concat(repository.toString()).concat(" ]");
        }
        return tag;
    }
    
    public String getBranch() {
        if(branch==null) {
            return "";
        }
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
        return new HgRevision(changeset, changesetIndex);
    }

    @Override
    public String toString() {
        if (nodeShort != null) {
            return this.changesetIndex + ":" + this.nodeShort;
        }
        return this.changesetIndex + ":" + this.changeset;

    }

    /**
     * @return the changedFiles
     */
    public FileStatus[] getChangedFiles() {
        if( changedFiles != null) {
            // Don't let clients manipulate the array in-place
            return changedFiles.clone();
        }
        return new FileStatus[0];
    }

    /**
     * @param changedFiles
     *            the changedFiles to set
     */
    public void setChangedFiles(FileStatus[] changedFiles) {
        this.changedFiles = changedFiles;
    }

    /**
     * @return the ageDate
     */
    public String getAgeDate() {
        return ageDate;
    }

    /**
     * @param ageDate
     *            the ageDate to set
     */
    public void setAgeDate(String ageDate) {
        this.ageDate = ageDate;
    }

    /**
     * @return the nodeShort
     */
    public String getNodeShort() {
        return nodeShort;
    }

    /**
     * @param nodeShort
     *            the nodeShort to set
     */
    public void setNodeShort(String nodeShort) {
        this.nodeShort = nodeShort;
    }

    public int compareTo(ChangeSet o) {
        if (o.getChangeset().equals(this.getChangeset())) {
            return 0;
        }

        if (realDate != null && o.getRealDate() != null) {
            int dateCompare = this.getRealDate().compareTo(o.getRealDate());
            if (dateCompare != 0) {
                return dateCompare;
            }
        }
        return this.getChangesetIndex() - o.getChangesetIndex();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof ChangeSet) {
            return this.compareTo((ChangeSet) obj) == 0;
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

    public Date getRealDate() {
        return this.realDate;
    }

    /**
     * @return the bundleFile
     */
    public File getBundleFile() {
        return bundleFile;
    }

    /**
     * @param bundleFile
     *            the bundleFile to set
     */
    public void setBundleFile(File bundleFile) {
        this.bundleFile = bundleFile;
    }

    public String[] getParents() {
        return parents;
    }

    public void setParents(String[] parents) {
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

    public void setChangesetIndex(int changesetIndex) {
        this.changesetIndex = changesetIndex;
    }

    public void setChangeset(String changeset) {
        this.changeset = changeset;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setDescription(String description) {
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

    public void setRealDate(Date realDate) {
        this.realDate = realDate;
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
     * @param repositoryLocation
     *            the repository to set
     */
    public void setRepository(HgRepositoryLocation repositoryLocation) {
        this.repository = repositoryLocation;
    }

    /**
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * @param direction
     *            the direction to set
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * @return the hgRoot
     */
    public File getHgRoot() {
        return hgRoot;
    }

    /**
     * @param hgRoot the hgRoot to set
     */
    public void setHgRoot(File hgRoot) {
        this.hgRoot = hgRoot;
    }
}
