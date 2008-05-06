package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Bastian Doetsch - implementation
 ******************************************************************************/
public class HgIncomingClient {

    private static final String FILES = "{files}";
    private static final String FILE_ADDS = "{file_adds}";
    private static final String FILE_DELS = "{file_dels}";
    private static final String PARENTS = "{parents}";
    private static final String DESC = "{desc|escape}";
    private static final String AUTHOR_PERSON = "{author|person}";
    private static final String DATE_AGE = "{date|age}";
    private static final String DATE_ISODATE = "{date|isodate}";
    private static final String NODE = "{node}";
    private static final String NODE_SHORT = "{node|short}";
    private static final String REV = "{rev}";
    private static final String TAGS = "{tags}";
    private static final String SEP_CHANGE_SET = ">";
    private static final String SEP_TEMPLATE_ELEMENT = "<";
    private static final String START = "°";
    public static final String TEMPLATE = START + SEP_TEMPLATE_ELEMENT + TAGS
            + SEP_TEMPLATE_ELEMENT + REV + SEP_TEMPLATE_ELEMENT + NODE_SHORT
            + SEP_TEMPLATE_ELEMENT + NODE + SEP_TEMPLATE_ELEMENT + DATE_ISODATE
            + SEP_TEMPLATE_ELEMENT + DATE_AGE + SEP_TEMPLATE_ELEMENT
            + AUTHOR_PERSON + SEP_TEMPLATE_ELEMENT + DESC
            + SEP_TEMPLATE_ELEMENT + PARENTS + SEP_TEMPLATE_ELEMENT + FILES
            + SEP_TEMPLATE_ELEMENT + FILE_ADDS + SEP_TEMPLATE_ELEMENT
            + FILE_DELS + SEP_TEMPLATE_ELEMENT + SEP_CHANGE_SET;

    /**
     * Gets all File Revisions that are incoming and saves them in a bundle
     * file. There can be more than one revision per file as this method obtains
     * all new changesets.
     * 
     * @param proj
     * @param repositories
     * @return Map containing all revisions of the IResources contained in the
     *         Changesets. The sorting is ascending by date.
     * @throws HgException
     */
    public static Map<IResource, SortedSet<ChangeSet>> getHgIncoming(
            IProject proj, HgRepositoryLocation repository) throws HgException {
        HgCommand command = new HgCommand("incoming", proj, false);
        File bundleFile = getBundleFile(proj, repository);
        File temp = new File(bundleFile.getAbsolutePath() + ".temp."
                + System.currentTimeMillis());
        try {
            command.addOptions("--debug", "--template", TEMPLATE, "--bundle",
                    temp.getCanonicalPath(), repository.getUrl());
            String result = command.executeToString();
            if (result.contains("no changes found")) {
                return null;
            }
            Map<IResource, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    result, proj, bundleFile, repository, Direction.INCOMING);
            temp.renameTo(bundleFile);
            return revisions;
        } catch (HgException hg) {
            if (hg.getMessage().contains("return code: 1")) {
                return null;
            }
            throw new HgException(hg.getMessage(), hg);
        } catch (IOException e) {
            throw new HgException(e.getMessage(), e);
        }
    }

    public static File getBundleFile(IProject proj, HgRepositoryLocation loc) {
        String strippedLocation = loc.getUrl().replace('/', '_').replace(':',
                '.');
        return MercurialEclipsePlugin.getDefault().getStateLocation().append(
                MercurialEclipsePlugin.BUNDLE_FILE_PREFIX + "."
                        + proj.getName() + "." + strippedLocation + ".hg")
                .toFile();
    }

    public static Map<IResource, SortedSet<ChangeSet>> createMercurialRevisions(
            String input, IProject proj, File bundleFile,
            HgRepositoryLocation repository, Direction direction) {
        return createMercurialRevisions(input, proj, TEMPLATE, SEP_CHANGE_SET,
                SEP_TEMPLATE_ELEMENT, direction, repository, bundleFile, START);
    }

    private static Map<IResource, SortedSet<ChangeSet>> createMercurialRevisions(
            String input, IProject proj, String templ,
            String changeSetSeparator, String templateElementSeparator,
            Direction direction, HgRepositoryLocation repository,
            File bundleFile, String contentStartMarker) {

        Map<IResource, SortedSet<ChangeSet>> fileRevisions = new HashMap<IResource, SortedSet<ChangeSet>>();

        if (input == null || input.length() == 0
                || input.indexOf(contentStartMarker) == -1) {
            return fileRevisions;
        }

        String content = input.substring(input.indexOf(contentStartMarker));
        String[] changeSetStrings = content.split(changeSetSeparator);

        for (String changeSet : changeSetStrings) {
            ChangeSet cs = getChangeSet(changeSet, templ,
                    templateElementSeparator);

            // add bundle file for being able to look into the bundle.
            cs.setRepository(repository);
            cs.setBundleFile(bundleFile);
            cs.setDirection(direction);

            if (cs.getChangedFiles() != null) {
                for (FileStatus file : cs.getChangedFiles()) {
                    int projectPathStartIndex = 0;
                    try {
                        if (!MercurialTeamProvider.isProjectRootInProject(proj)) {
                             projectPathStartIndex = file.getPath().indexOf(proj.getName());
                        }
                    } catch (CoreException e) {                        
                        MercurialEclipsePlugin.logError(e);
                    }
                    if (projectPathStartIndex >= 0) {
                        IResource res = proj.getFile(file.getPath().substring(
                                projectPathStartIndex));
                        SortedSet<ChangeSet> incomingFileRevs = addChangeSetRevisions(
                                fileRevisions, cs, res);
                        fileRevisions.put(res, incomingFileRevs);
                    }
                }
            }
            SortedSet<ChangeSet> projectRevs = addChangeSetRevisions(
                    fileRevisions, cs, proj);
            fileRevisions.put(proj, projectRevs);
        }
        return fileRevisions;
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
        SortedSet<ChangeSet> incomingFileRevs = fileRevisions.get(res);
        if (incomingFileRevs == null) {
            incomingFileRevs = new TreeSet<ChangeSet>();
        }
        incomingFileRevs.add(cs);
        return incomingFileRevs;
    }

    public static ChangeSet getChangeSet(String changeSet, String templ,
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

        String[] comps = split(changeSet, templateElementSeparator);
        ChangeSet cs = new ChangeSet();
        cs.setTag(getValue(pos, comps, TAGS));
        cs.setChangesetIndex(Integer.parseInt(getValue(pos, comps, REV)));
        cs.setNodeShort(getValue(pos, comps, NODE_SHORT));
        cs.setChangeset(getValue(pos, comps, NODE));
        cs.setDate(getValue(pos, comps, DATE_ISODATE));
        cs.setAgeDate(getValue(pos, comps, DATE_AGE));
        cs.setUser(getValue(pos, comps, AUTHOR_PERSON));
        cs.setDescription(unescape(getValue(pos, comps, DESC)));
        cs.setParents(splitClean(getValue(pos, comps, PARENTS), " "));
        cs.setChangedFiles(getFileStatuses(pos, comps));
        return cs;
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
            String[] comps, String temp) {
        return new HashSet<String>(Arrays.asList(splitClean(getValue(pos,
                comps, temp), " ")));
    }

    private static void addFiles(List<FileStatus> statuses,
            HashSet<String> files, Action action) {
        for (String f : files) {
            statuses.add(new FileStatus(action, f));
        }
    }

    // Split doesn't do a very good of two separators without anything in
    // between
    // Regex is a better way of doing all of this...
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
        if (string.length() == 0) {
            return new String[] {};
        }
        return string.split(sep);
    }

    private static String getValue(Map<String, Integer> templatePositions,
            String[] changeSetComponents, String temp) {
        return changeSetComponents[templatePositions.get(temp).intValue()];
    }

    private static String unescape(String string) {
        return string.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");
    }
}
