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
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * This class helps HgClients to parse the changeset output of hg to Changeset
 * objects.
 * 
 * @author Bastian Doetsch
 * 
 */
public abstract class AbstractParseChangesetClient extends AbstractClient {

    private static final String STYLE_SRC = "/styles/log_style";
    private static final String STYLE = "/log_style";
    private static final String STYLE_WITH_FILES_SRC = "/styles/log_style_with_files";
    private static final String STYLE_WITH_FILES = "/log_style_with_files";
    private static final String STYLE_TEMP_EXTN = ".tmpl";

    /**
     * Return a File reference to a copy of the required mercurial style file.
     * Two types are available, one that includes the files and one that
     * doesn't. Using the one with files can be very slow on large repos.
     * <p>
     * These style files are included in the plugin jar file and need to be
     * copied out of there into the plugin state area so a path can be given to
     * the hg command.
     * 
     * @param withFiles
     *            return the style that includes the files if true.
     * @return a File reference to an existing file
     */
    protected static File getStyleFile(boolean withFiles) throws HgException {
        String style_src;
        String style;

        if (!withFiles) {
            style = STYLE;
            style_src = STYLE_SRC;
        } else {
            style = STYLE_WITH_FILES;
            style_src = STYLE_WITH_FILES_SRC;
        }
        String style_tmpl = style + STYLE_TEMP_EXTN;
        String style_tmpl_src = style_src + STYLE_TEMP_EXTN;

        IPath sl = MercurialEclipsePlugin.getDefault().getStateLocation();

        File stylefile = sl.append(style).toFile();
        File tmplfile = sl.append(style_tmpl).toFile();

        ClassLoader cl = AbstractParseChangesetClient.class.getClassLoader();

        if (stylefile.canRead() && tmplfile.canRead()) {
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
            while ((n = styleistr.read(buffer)) != -1) {
                styleostr.write(buffer, 0, n);
            }
            while ((n = tmplistr.read(buffer)) != -1) {
                tmplostr.write(buffer, 0, n);
            }
            styleostr.close();
            tmplostr.close();

            return stylefile;
        } catch (IOException e) {
            throw new HgException("Failed to setup hg style file", e);
        }
    }

    /**
     * Parse log output into a set of changesets.
     * <p>
     * Format of input is defined in the two style files in /styles and is as
     * follows for each changeset. The changesets are separated by a line of '='
     * characters "^=+$". <br>
     * 
     * <pre>
     * &lt;cs&gt;
     * &lt;br&gt;b2_1_5&lt;/br&gt;
     * &lt;tg&gt;&lt;/tg&gt;
     * &lt;rv&gt;3624&lt;/rv&gt;
     * &lt;ns&gt;209208fad980&lt;/ns&gt;
     * &lt;nl&gt;209208fad980102b98ca438b79b78c64e03b6c29&lt;/nl&gt;
     * &lt;di&gt;2008-04-24 05:04 +0000&lt;/di&gt;
     * &lt;da&gt;4 weeks&lt;/da&gt;
     * &lt;au&gt;abuckley&lt;/au&gt;
     * &lt;pr&gt;3621:dc5cbf08f0d2640c334855b3ce1a38002850d65b -1:0000000000000000000000000000000000000000 &lt;/pr&gt;
     * &lt;de&gt;Updated document mapping file with custom validation items
     * Sent updated document mapping file and configuration files to customer&lt;/de&gt;
     * &lt;fl&gt;
     * &lt;f&gt;Products/overlay/config/application/documentmapping.csv&lt;/f&gt;
     * &lt;/fl&gt;
     * &lt;fa&gt;
     * &lt;/fa&gt;
     * &lt;fd&gt;
     * &lt;/fd&gt;
     * &lt;/cs&gt;
     * ================
     * </pre>
     * 
     * <br>
     * 
     * @param input
     *            output from the hg log command
     * @param proj
     * @param withFiles
     *            Are files included in the log output
     * @param direction
     *            Incoming, Outgoing or Local changesets
     * @param repository
     * @param bundleFile
     * @return
     * @throws HgException
     *             TODO
     */
    protected final static Map<IPath, SortedSet<ChangeSet>> createMercurialRevisions(
            IResource res, String input, boolean withFiles,
            Direction direction, HgRepositoryLocation repository,
            File bundleFile) throws HgException {

        Map<IPath, SortedSet<ChangeSet>> fileRevisions = new HashMap<IPath, SortedSet<ChangeSet>>();

        if (input == null || input.length() == 0) {
            return fileRevisions;
        }

        /*
         * Would be nice to do this as a single XML document using the SAX
         * parser but I haven't worked out how to get a mercurial style file to
         * create a valid XML document (cannot get the closing element output)
         */
        String[] changeSetStrings = input.split("\n====+\n");
        File hgRoot = HgRootClient.getHgRoot(res.getLocation().toFile());
        for (String changeSet : changeSetStrings) {
            ChangeSet cs;
            cs = getChangeSet(changeSet);

            // add bundle file for being able to look into the bundle.
            cs.setRepository(repository);
            cs.setBundleFile(bundleFile);
            cs.setDirection(direction);
            cs.setHgRoot(hgRoot);

            // changeset to resources & project
            addChangesetToResourceMap(res.getLocation(), fileRevisions, cs);
        }
        return fileRevisions;
    }

    /**
     * @param proj
     * @param fileRevisions
     * @param cs
     * @throws HgException
     * @throws IOException
     */
    protected final static void addChangesetToResourceMap(IPath path,
            Map<IPath, SortedSet<ChangeSet>> fileRevisions, ChangeSet cs)
            throws HgException {
        try {
            if (cs.getChangedFiles() != null) {
                for (FileStatus file : cs.getChangedFiles()) {
                    IPath hgRoot = new Path(cs.getHgRoot().getCanonicalPath());
                    IPath fileRelPath = new Path(file.getPath());
                    IPath fileAbsPath = hgRoot.append(fileRelPath);

                    SortedSet<ChangeSet> revs = addChangeSetRevisions(
                            fileRevisions, cs, fileAbsPath);
                    fileRevisions.put(fileAbsPath, revs);

                }
            }

            // hg root
            IPath repoPath = new Path(cs.getHgRoot().getCanonicalPath());

            SortedSet<ChangeSet> projectRevs = addChangeSetRevisions(
                    fileRevisions, cs, repoPath);

            fileRevisions.put(repoPath, projectRevs);

            // given path
            SortedSet<ChangeSet> pathRevs = addChangeSetRevisions(
                    fileRevisions, cs, path);
            fileRevisions.put(path, pathRevs);

        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @param fileRevisions
     * @param cs
     * @param res
     * @return
     */
    private static SortedSet<ChangeSet> addChangeSetRevisions(
            Map<IPath, SortedSet<ChangeSet>> fileRevisions, ChangeSet cs,
            IPath path) {
        SortedSet<ChangeSet> fileRevs = fileRevisions.get(path);
        if (fileRevs == null) {
            fileRevs = new TreeSet<ChangeSet>();
        }
        fileRevs.add(cs);
        return fileRevs;
    }

    private static FileStatus[] getFileStatuses(Element csn) {
        HashSet<String> files = getFilesValue(csn, "fl");
        HashSet<String> adds = getFilesValue(csn, "fa");
        HashSet<String> del = getFilesValue(csn, "fd");

        files.removeAll(adds);
        files.removeAll(del);

        List<FileStatus> statuses = new ArrayList<FileStatus>(files.size());
        addFiles(statuses, files, Action.MODIFIED);
        addFiles(statuses, adds, Action.ADDED);
        addFiles(statuses, del, Action.REMOVED);

        return statuses.toArray(new FileStatus[statuses.size()]);
    }

    private static HashSet<String> getFilesValue(Element csn, String name) {
        NodeList nl = csn.getElementsByTagName(name);
        if (nl.getLength() == 0) {
            return new HashSet<String>(0);
        }
        NodeList files = ((Element) nl.item(0)).getElementsByTagName("f");
        HashSet<String> ret = new HashSet<String>(files.getLength());
        for (int i = 0; i < files.getLength(); i++) {
            ret.add(files.item(i).getTextContent());
        }

        return ret;
    }

    private static void addFiles(List<FileStatus> statuses,
            HashSet<String> files, Action action) {
        for (String f : files) {
            statuses.add(new FileStatus(action, f));
        }
    }

    private static String[] splitClean(String string, String sep) {
        if (string == null || string.length() == 0) {
            return new String[] {};
        }
        return string.split(sep);
    }

    private static String unescape(String string) {
        return string.replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&");
    }

    /**
     * Remove a leading tab on each line in the string.
     * 
     * @param string
     * @return
     */
    private static String untab(String string) {
        return string.replaceAll("\n\t", "\n");
    }
    
    /**
     * Clean the string of special chars that might be invalid for the
     * XML parser.  Return the cleaned string (special chars replaced by
     * ordinary spaces).
     * 
     * @param str the string to clean
     * @return the cleaned string
     */
    private static String cleanControlChars(String str) {
        final StringBuilder buf = new StringBuilder();
        final int len = str.length();
        for (int i = 0; i < len; i++) {
            final int ch = str.codePointAt(i);
            if (ch == '\r' || ch == '\n' || ch == '\t') {
                buf.appendCodePoint(ch);
            } else if (Character.isISOControl(ch)) {
                buf.append(' ');
            } else {
                buf.appendCodePoint(ch);
            }
        }
        return buf.toString();
    }

    /**
     * Parse a changeset as output from the log command (see
     * {@link #createMercurialRevisions()}).
     * 
     * @param changeSet
     * @return
     */
    private static ChangeSet getChangeSet(String changeSet) throws HgException {

        if (changeSet == null) {
            return null;
        }

        String outputString = changeSet.substring(changeSet.indexOf("<cs>"));
        
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc;
            Reader ir = null;
            try {
                ir = new StringReader(outputString);
                doc = docBuilder.parse(new InputSource(ir));
            } catch (SAXException e) {
                outputString = cleanControlChars(outputString);
                ir = new StringReader(outputString);
                doc = docBuilder.parse(new InputSource(ir));
            } finally {
                if (ir != null) {
                    ir.close();
                }
            }

            // normalize text representation
            doc.getDocumentElement().normalize();

            NodeList csnl = doc.getElementsByTagName("cs");
            int totalCs = csnl.getLength();
            if (totalCs != 1) {
                // Something screwy going on, should have 1 and 1 only.
                throw new HgException(
                        "Cannot parse changeset, bad log output?: " + changeSet);
            }
            Element csn = (Element) csnl.item(0);

            ChangeSet cs = new ChangeSet();

            cs.setTag(getValue(csn, "tg"));
            cs.setBranch(getValue(csn, "br"));
            cs.setChangesetIndex(Integer.parseInt(getValue(csn, "rv")));
            cs.setNodeShort(getValue(csn, "ns"));
            cs.setChangeset(getValue(csn, "nl"));
            cs.setDate(getValue(csn, "di"));
            cs.setAgeDate(getValue(csn, "da"));
            cs.setUser(getValue(csn, "au"));
            cs.setDescription(untab(unescape(getValue(csn, "de"))));
            cs.setParents(splitClean(getValue(csn, "pr"), " "));
            cs.setChangedFiles(getFileStatuses(csn));
            return cs;
        } catch (ParserConfigurationException e) {
            throw new HgException("Changeset parser Configuration error", e);
        } catch (SAXException e) {
            throw new HgException("Changeset parsing error for \"" + changeSet
                    + "\"", e);
        } catch (IOException e) {
            throw new HgException("Error parsing changeset \"" + changeSet
                    + "\"", e);
        }
    }

    /**
     * @param csn
     * @return
     */
    private static String getValue(Element csn, String name) {
        return csn.getElementsByTagName(name).item(0).getTextContent();
    }

}
