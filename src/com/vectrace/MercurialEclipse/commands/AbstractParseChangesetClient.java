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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
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
public abstract class AbstractParseChangesetClient {

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
    private static final String BRANCHES = "{branches}";
    private static final String SEP_CHANGE_SET = "@@@";
    private static final String SEP_TEMPLATE_ELEMENT = "§§§";
    private static final String START = "°°°";
    private static final String TEMPLATE = START + SEP_TEMPLATE_ELEMENT
            + BRANCHES + SEP_TEMPLATE_ELEMENT + TAGS + SEP_TEMPLATE_ELEMENT + REV + SEP_TEMPLATE_ELEMENT
            + NODE_SHORT + SEP_TEMPLATE_ELEMENT + NODE + SEP_TEMPLATE_ELEMENT
            + DATE_ISODATE + SEP_TEMPLATE_ELEMENT + DATE_AGE
            + SEP_TEMPLATE_ELEMENT + AUTHOR_PERSON + SEP_TEMPLATE_ELEMENT
            + DESC + SEP_TEMPLATE_ELEMENT + PARENTS + SEP_TEMPLATE_ELEMENT
            + SEP_CHANGE_SET;

    private static final String TEMPLATE_WITH_FILES = START
            + SEP_TEMPLATE_ELEMENT + BRANCHES + SEP_TEMPLATE_ELEMENT + TAGS + SEP_TEMPLATE_ELEMENT + REV
            + SEP_TEMPLATE_ELEMENT + NODE_SHORT + SEP_TEMPLATE_ELEMENT + NODE
            + SEP_TEMPLATE_ELEMENT + DATE_ISODATE + SEP_TEMPLATE_ELEMENT
            + DATE_AGE + SEP_TEMPLATE_ELEMENT + AUTHOR_PERSON
            + SEP_TEMPLATE_ELEMENT + DESC + SEP_TEMPLATE_ELEMENT + PARENTS
            + SEP_TEMPLATE_ELEMENT + FILES + SEP_TEMPLATE_ELEMENT + FILE_ADDS
            + SEP_TEMPLATE_ELEMENT + FILE_DELS + SEP_CHANGE_SET;

    private static final String STYLE_SRC = "/styles/log_style";
    private static final String STYLE = "/log_style";
    private static final String STYLE_WITH_FILES_SRC = "/styles/log_style_with_files";
    private static final String STYLE_WITH_FILES = "/log_style_with_files";
    private static final String STYLE_TEMP_EXTN = ".tmpl";

    /**
     * Return a File reference to a copy of the required mercurial style file. Two types
     * are available, one that includes  the files and one that doesn't. Using the one
     * with files can be very slow on large repos.
     * <p>
     * These style files are included in the plugin jar file and need to be copied out of there
     * into the plugin state area so a path can be given to the hg command.
     * 
     * @param withFiles return the style that includes the files if true.
     * @return a File reference to an existing file
     */
    protected static File getStyleFile(boolean withFiles) throws HgException {
        String style_src;
        String style;
        
        if(withFiles) {
            style = STYLE;
            style_src = STYLE_SRC;
        }
        else {
            style = STYLE_WITH_FILES;
            style_src = STYLE_WITH_FILES_SRC;
        }
        String style_tmpl = style+STYLE_TEMP_EXTN;
        String style_tmpl_src = style_src+STYLE_TEMP_EXTN;

        IPath sl = MercurialEclipsePlugin.getDefault().getStateLocation();

        File stylefile = sl.append(style).toFile();
        File tmplfile = sl.append(style_tmpl).toFile();

        ClassLoader cl = AbstractParseChangesetClient.class.getClassLoader();

        if(stylefile.canRead()&&tmplfile.canRead()) {
            // Already have copies, return the file reference to the style file
            return stylefile;
        }
        // Need to make copies into the state directory from the jar file.
        // set delete on exit so a new copy is made each time eclipse is started
        // so we don't use stale copies on plugin updates.
        InputStream styleistr = cl.getResourceAsStream(style_src);
        InputStream tmplistr = cl.getResourceAsStream(style_tmpl_src);
        try {
            OutputStream styleostr = new FileOutputStream(stylefile);
            stylefile.deleteOnExit();
            OutputStream tmplostr = new FileOutputStream(tmplfile);
            tmplfile.deleteOnExit();

            byte buffer[] = new byte[1024];
            int n;
            while((n = styleistr.read(buffer))!=-1) {
                styleostr.write(buffer,0,n);
            }
            while((n = tmplistr.read(buffer))!=-1) {
                tmplostr.write(buffer,0,n);
            }
            styleostr.close();
            tmplostr.close();

            return stylefile;
        }
        catch (IOException e) {
            throw new HgException("Failed to setup hg style file",e);
        }
    }

    /**
     * Parse log output into a set of changesets.
     * <p>
     * Format of input is defined in the two style files in /styles and is as follows for
     * each changeset. The changesets are separated by a line of '=' characters "^=+$". 
     * <br><pre>
     * Branches: b2_1_5
     * Tags: tip
     * Rev: 3634
     * NodeShort: 9ace0a054654
     * NodeLong: 9ace0a054654fe893198d3937b49fe6bff48a708
     * DateIso: 2008-04-28 01:06 +0000
     * DateAge: 4 weeks
     * Author: xxxxxx
     * Parents: 3631:22d44005f98fb5b1d794d1e6a93a68393190f50d -1:0000000000000000000000000000000000000000 
     * Description: Update to deployment notes, clarification on database migration for  bookings
     *         and minor formating changes
     * Files:
     *         Products/dev/deployment_notes.txt
     * FileAdds:
     * FileDels:
     * ================
     * </pre><br>
     * 
     * @param input output from the hg log command
     * @param proj
     * @param withFiles Are files included in the log output
     * @param direction Incoming, Outgoing or Local changesets
     * @param repository
     * @param bundleFile
     * @return
     */
    protected static Map<IResource, SortedSet<ChangeSet>> createMercurialRevisions(
            String input, IProject proj, boolean withFiles,
            Direction direction, HgRepositoryLocation repository,
            File bundleFile) {

        Map<IResource, SortedSet<ChangeSet>> fileRevisions = new HashMap<IResource, SortedSet<ChangeSet>>();
        
        String templ;
        if(withFiles) {
            templ = TEMPLATE_WITH_FILES;
        }
        else {
            templ = TEMPLATE;
        }
        if (input == null || input.length() == 0) {
            return fileRevisions;
        }

        String[] changeSetStrings = input.split("^=+$");

        for (String changeSet : changeSetStrings) {
            ChangeSet cs = getChangeSet(changeSet);

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

    /**
     * Parse a changeset as output from the log command (see {@link #createMercurialRevisions()}).
     * 
     * @param changeSet
     * @return
     */
    private static ChangeSet getChangeSet(String changeSet) {
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
