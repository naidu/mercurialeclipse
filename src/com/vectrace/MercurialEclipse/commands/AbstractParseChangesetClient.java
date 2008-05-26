/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * This class helps HgClients to parse the changeset output of hg to Changeset
 * objects.
 * 
 * @author Bastian Doetsch
 * 
 */
public abstract class AbstractParseChangesetClient extends AbstractClient {

    protected static final String FILES = "{files}";
    protected static final String FILE_ADDS = "{file_adds}";
    protected static final String FILE_DELS = "{file_dels}";
    protected static final String PARENTS = "{parents}";
    protected static final String DESC = "{desc|escape}";
    protected static final String AUTHOR_PERSON = "{author|person}";
    protected static final String DATE_AGE = "{date|age}";
    protected static final String DATE_ISODATE = "{date|isodate}";
    protected static final String NODE = "{node}";
    protected static final String NODE_SHORT = "{node|short}";
    protected static final String REV = "{rev}";
    protected static final String TAGS = "{tags}";
    protected static final String BRANCHES = "{branches}";
    protected static final String SEP_CHANGE_SET = "@@@";
    protected static final String SEP_TEMPLATE_ELEMENT = "§§§";
    protected static final String START = "°°°";
    protected static final String TEMPLATE = START + SEP_TEMPLATE_ELEMENT
            + BRANCHES + SEP_TEMPLATE_ELEMENT + TAGS + SEP_TEMPLATE_ELEMENT + REV + SEP_TEMPLATE_ELEMENT
            + NODE_SHORT + SEP_TEMPLATE_ELEMENT + NODE + SEP_TEMPLATE_ELEMENT
            + DATE_ISODATE + SEP_TEMPLATE_ELEMENT + DATE_AGE
            + SEP_TEMPLATE_ELEMENT + AUTHOR_PERSON + SEP_TEMPLATE_ELEMENT
            + DESC + SEP_TEMPLATE_ELEMENT + PARENTS + SEP_TEMPLATE_ELEMENT
            + SEP_CHANGE_SET;

    protected static final String TEMPLATE_WITH_FILES = START
            + SEP_TEMPLATE_ELEMENT + BRANCHES + SEP_TEMPLATE_ELEMENT + TAGS + SEP_TEMPLATE_ELEMENT + REV
            + SEP_TEMPLATE_ELEMENT + NODE_SHORT + SEP_TEMPLATE_ELEMENT + NODE
            + SEP_TEMPLATE_ELEMENT + DATE_ISODATE + SEP_TEMPLATE_ELEMENT
            + DATE_AGE + SEP_TEMPLATE_ELEMENT + AUTHOR_PERSON
            + SEP_TEMPLATE_ELEMENT + DESC + SEP_TEMPLATE_ELEMENT + PARENTS
            + SEP_TEMPLATE_ELEMENT + FILES + SEP_TEMPLATE_ELEMENT + FILE_ADDS
            + SEP_TEMPLATE_ELEMENT + FILE_DELS + SEP_CHANGE_SET;

    protected static Map<IResource, SortedSet<ChangeSet>> createMercurialRevisions(
            String input, IProject proj, File bundleFile,
            HgRepositoryLocation repository, Direction direction) {
        return createMercurialRevisions(input, proj, TEMPLATE, SEP_CHANGE_SET,
                SEP_TEMPLATE_ELEMENT, direction, repository, bundleFile, START);
    }

    protected static Map<IResource, SortedSet<ChangeSet>> createMercurialRevisions(
            String input, IProject proj, String templ,
            String changeSetSeparator, String templateElementSeparator,
            Direction direction, HgRepositoryLocation repository,
            File bundleFile, String contentStartMarker) {

        Map<IResource, SortedSet<ChangeSet>> fileRevisions = new HashMap<IResource, SortedSet<ChangeSet>>();

        if (input == null || input.length() == 0
                || input.indexOf(contentStartMarker) == -1) {
            return fileRevisions;
        }

        String content = input.substring(input.indexOf(contentStartMarker)+contentStartMarker.length());
        String[] changeSetStrings = content.split(changeSetSeparator);

        for (String changeSet : changeSetStrings) {
            ChangeSet cs = getChangeSet(changeSet, templ,
                    templateElementSeparator);

            // add bundle file for being able to look into the bundle.
            cs.setRepository(repository);
            cs.setBundleFile(bundleFile);
            cs.setDirection(direction);

            // changeset to resources & project
            addChangesetToResourceMap(proj, fileRevisions, cs);
        }
        return fileRevisions;
    }

    /**
     * @param proj
     * @param fileRevisions
     * @param cs
     */
    protected static void addChangesetToResourceMap(IProject proj,
            Map<IResource, SortedSet<ChangeSet>> fileRevisions, ChangeSet cs) {
        if (cs.getChangedFiles() != null) {
            for (FileStatus file : cs.getChangedFiles()) {
                IResource res = MercurialUtilities.getIResource(proj, file
                        .getPath());
                SortedSet<ChangeSet> incomingFileRevs = addChangeSetRevisions(
                        fileRevisions, cs, res);
                fileRevisions.put(res, incomingFileRevs);

            }
        }
        SortedSet<ChangeSet> projectRevs = addChangeSetRevisions(fileRevisions,
                cs, proj);
        fileRevisions.put(proj, projectRevs);
    }

    /**
     * @param fileRevisions
     * @param cs
     * @param res
     * @return
     */
    private static SortedSet<ChangeSet> addChangeSetRevisions(
            Map<IResource, SortedSet<ChangeSet>> fileRevisions, ChangeSet cs,
            IResource res) {
        SortedSet<ChangeSet> fileRevs = fileRevisions.get(res);
        if (fileRevs == null) {
            fileRevs = new TreeSet<ChangeSet>();
        }
        fileRevs.add(cs);
        return fileRevs;
    }

    private static FileStatus[] getFileStatuses(Map<String, Integer> pos,
            String[] comps) {
        HashSet<String> files = getFilesValue(pos, comps, FILES);
        HashSet<String> adds = getFilesValue(pos, comps, FILE_ADDS);
        HashSet<String> del = getFilesValue(pos, comps, FILE_DELS);

        files.removeAll(adds);
        files.removeAll(del);

        List<FileStatus> statuses = new ArrayList<FileStatus>(files.size());
        addFiles(statuses, files, Action.MODIFIED);
        addFiles(statuses, adds, Action.ADDED);
        addFiles(statuses, del, Action.REMOVED);

        return statuses.toArray(new FileStatus[statuses.size()]);
    }

    private static HashSet<String> getFilesValue(Map<String, Integer> pos,
            String[] comps, String templateTag) {
        String value = getValue(pos, comps, templateTag);
        String[] splitCleanValues = splitClean(value, " ");
        return new HashSet<String>(Arrays.asList(splitCleanValues));
    }

    private static void addFiles(List<FileStatus> statuses,
            HashSet<String> files, Action action) {
        for (String f : files) {
            statuses.add(new FileStatus(action, f));
        }
    }

    private static String[] split(String templ, String sep) {
        List<String> l = new ArrayList<String>();
        int j = 0;
        for (int i = templ.indexOf(sep); i > -1; i = templ.indexOf(sep, i)) {
            l.add(templ.substring(j, i));
            i += sep.length();
            j = i;
        }
        return l.toArray(new String[l.size()]);
    }

    private static String[] splitClean(String string, String sep) {
        if (string == null || string.length() == 0) {
            return new String[] {};
        }
        return string.split(sep);
    }

    private static String getValue(Map<String, Integer> templatePositions,
            String[] changeSetComponents, String temp) {
        Integer valuePosition = templatePositions.get(temp);
        String returnValue = null;
        if (valuePosition != null) {
            returnValue = changeSetComponents[valuePosition.intValue()];
        }
        return returnValue;
    }

    private static String unescape(String string) {
        return string.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");
    }

    private static ChangeSet getChangeSet(String changeSet, String templ,
            String templateElementSeparator) {
        if (changeSet == null) {
            return null;
        }
        String[] templateElements = templ.split(templateElementSeparator);
        Map<String, Integer> pos = new HashMap<String, Integer>(
                templateElements.length);

        int i = 0;
        for (String elem : templateElements) {
            pos.put(elem, Integer.valueOf(i));
            i++;
        }

        String[] stringComponents = split(changeSet, templateElementSeparator);
        ChangeSet cs = new ChangeSet();
        cs.setTag(getValue(pos, stringComponents, TAGS));
        cs.setBranch(getValue(pos, stringComponents, BRANCHES));
        cs.setChangesetIndex(Integer.parseInt(getValue(pos, stringComponents,
                REV)));
        cs.setNodeShort(getValue(pos, stringComponents, NODE_SHORT));
        cs.setChangeset(getValue(pos, stringComponents, NODE));
        cs.setDate(getValue(pos, stringComponents, DATE_ISODATE));
        cs.setAgeDate(getValue(pos, stringComponents, DATE_AGE));
        cs.setUser(getValue(pos, stringComponents, AUTHOR_PERSON));
        cs.setDescription(unescape(getValue(pos, stringComponents, DESC)));
        cs
                .setParents(splitClean(
                        getValue(pos, stringComponents, PARENTS), " "));
        cs.setChangedFiles(getFileStatuses(pos, stringComponents));
        return cs;
    }

}
