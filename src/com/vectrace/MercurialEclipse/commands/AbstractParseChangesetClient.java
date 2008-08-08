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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * This class helps HgClients to parse the changeset output of hg to Changeset
 * objects.
 * 
 * @author Bastian Doetsch
 * 
 */
abstract class AbstractParseChangesetClient extends AbstractClient {

    /**
     * @author bastian
     * 
     */
    private static final class ChangesetContentHandler implements
            ContentHandler {

        private String br;
        private String tg;
        private int rv;
        private String ns;
        private String nl;
        private String di;
        private String da;
        private String au;
        private String pr;
        private String de;
        private static IResource res;
        private Direction direction;
        private HgRepositoryLocation repository;
        private File bundleFile;
        private File hgRoot;
        private static Map<IPath, SortedSet<ChangeSet>> fileRevisions;
        private Set<String> filesModified = new TreeSet<String>();
        private Set<String> filesAdded = new TreeSet<String>();
        private Set<String> filesRemoved = new TreeSet<String>();
        private Action action;

        /**
         * @param res
         * @param direction
         * @param repository
         * @param bundleFile
         * @param hgRoot
         * @param fileRevisions
         */
        public ChangesetContentHandler(IResource res, Direction direction,
                HgRepositoryLocation repository, File bundleFile, File hgRoot,
                Map<IPath, SortedSet<ChangeSet>> fileRevisions) {
            ChangesetContentHandler.res = res;
            this.direction = direction;
            this.repository = repository;
            this.bundleFile = bundleFile;
            this.hgRoot = hgRoot;
            ChangesetContentHandler.fileRevisions = fileRevisions;
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

        private static String[] splitClean(String string, String sep) {
            if (string == null || string.length() == 0) {
                return new String[] {};
            }
            return string.split(sep);
        }

        public void characters(char[] ch, int start, int length)
                throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void endElement(String uri, String localName, String name)
                throws SAXException {
            if (name.equals("cs")) {

                ChangeSet.Builder csb = new ChangeSet.Builder(rv, nl, br, di,
                        au);
                csb.tag(tg);
                csb.nodeShort(ns);
                csb.ageDate(da);
                csb.description(untab(unescape(de)));
                csb.parents(splitClean(pr, " "));

                csb.hgRoot(hgRoot).bundleFile(bundleFile)
                        .repository(repository).direction(direction);

                csb.bundleFile(bundleFile);
                csb.direction(direction);
                csb.hgRoot(hgRoot);
                csb.repository(repository);
                
                List<FileStatus> list = new ArrayList<FileStatus>();
                for (String file : filesModified) {
                    list.add(new FileStatus(FileStatus.Action.MODIFIED, file));
                }
                for (String file : filesAdded) {
                    list.add(new FileStatus(FileStatus.Action.ADDED, file));
                }
                for (String file : filesRemoved) {
                    list.add(new FileStatus(FileStatus.Action.REMOVED, file));
                }
                csb.changedFiles(list.toArray(new FileStatus[list.size()]));
                
                ChangeSet changeSet = csb.build();

                // changeset to resources & project
                try {
                    addChangesetToResourceMap(changeSet);
                } catch (HgException e) {
                    throw new SAXException(e);
                }
                filesModified.clear();
                filesAdded.clear();
                filesRemoved.clear();
            }
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
        }

        public void processingInstruction(String target, String data)
                throws SAXException {
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void skippedEntity(String name) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startElement(String uri, String localName, String name,
                Attributes atts) throws SAXException {
            /*
             * <br v="{branches}"/> <tg v="{tags}"/> <rv v="{rev}"/> <ns
             * v="{node|short}"/> <nl v="{node}"/> <di v="{date|isodate}"/> <da
             * v="{date|age}"/> <au v="{author|person}"/> <pr v="{parents}"/>
             * <de v="{desc|escape|tabindent}"/>
             */
            if (name.equals("br")) {
                this.br = atts.getValue(0);
            } else if (name.equals("tg")) {
                this.tg = atts.getValue(0);
            } else if (name.equals("rv")) {
                this.rv = Integer.parseInt(atts.getValue(0));
            } else if (name.equals("ns")) {
                this.ns = atts.getValue(0);
            } else if (name.equals("nl")) {
                this.nl = atts.getValue(0);
            } else if (name.equals("di")) {
                this.di = atts.getValue(0);
            } else if (name.equals("da")) {
                this.da = atts.getValue(0);
            } else if (name.equals("au")) {
                this.au = atts.getValue(0);
            } else if (name.equals("pr")) {
                this.pr = atts.getValue(0);
            } else if (name.equals("de")) {
                this.de = untab(unescape(atts.getValue(0)));
            } else if (name.equals("fl")) {
                this.action = FileStatus.Action.MODIFIED;
            } else if (name.equals("fa")) {
                this.action = FileStatus.Action.ADDED;
            } else if (name.equals("fd")) {
                this.action = FileStatus.Action.REMOVED;
            } else if (name.equals("f")) {
                if (this.action == Action.ADDED) {
                    filesAdded.add(atts.getValue(0));
                    filesModified.remove(atts.getValue(0));
                } else if (this.action == Action.MODIFIED) {
                    filesModified.add(atts.getValue(0));                    
                } else if (this.action == Action.REMOVED) {
                    filesRemoved.add(atts.getValue(0));
                    filesModified.remove(atts.getValue(0));
                }
            }
        }

        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
        }

        /**
         * @param proj
         * @param fileRevisions
         * @param cs
         * @throws HgException
         * @throws IOException
         */
        private final static void addChangesetToResourceMap(ChangeSet cs)
                throws HgException {
            try {
                if (cs.getChangedFiles() != null) {
                    for (FileStatus file : cs.getChangedFiles()) {
                        IPath hgRoot = new Path(cs.getHgRoot()
                                .getCanonicalPath());
                        IPath fileRelPath = new Path(file.getPath());
                        IPath fileAbsPath = hgRoot.append(fileRelPath);

                        SortedSet<ChangeSet> revs = addChangeSetRevisions(cs,
                                fileAbsPath);
                        fileRevisions.put(fileAbsPath, revs);

                    }
                }

                // hg root
                IPath repoPath = new Path(cs.getHgRoot().getCanonicalPath());

                SortedSet<ChangeSet> projectRevs = addChangeSetRevisions(cs,
                        repoPath);

                fileRevisions.put(repoPath, projectRevs);

                // given path
                IPath path = res.getLocation();
                SortedSet<ChangeSet> pathRevs = addChangeSetRevisions(cs, path);
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
        private static SortedSet<ChangeSet> addChangeSetRevisions(ChangeSet cs,
                IPath path) {
            SortedSet<ChangeSet> fileRevs = fileRevisions.get(path);
            if (fileRevs == null) {
                fileRevs = new TreeSet<ChangeSet>();
            }
            fileRevs.add(cs);
            return fileRevs;
        }

    }

    private static final String STYLE_SRC = "/styles/log_style";
    private static final String STYLE = "/log_style";
    private static final String STYLE_WITH_FILES_SRC = "/styles/log_style_with_files";
    private static final String STYLE_WITH_FILES = "/log_style_with_files";
    private static final String STYLE_TEMP_EXTN = ".tmpl";

    private static ContentHandler handler;

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
     * follows for each changeset.
     * 
     * <pre>
     * &lt;cs&gt;
     * &lt;br v=&quot;{branches}&quot;/&gt;
     * &lt;tg v=&quot;{tags}&quot;/&gt;
     * &lt;rv v=&quot;{rev}&quot;/&gt;
     * &lt;ns v=&quot;{node|short}&quot;/&gt;
     * &lt;nl v=&quot;{node}&quot;/&gt;
     * &lt;di v=&quot;{date|isodate}&quot;/&gt;
     * &lt;da v=&quot;{date|age}&quot;/&gt;
     * &lt;au v=&quot;{author|person}&quot;/&gt;
     * &lt;pr v=&quot;{parents}&quot;/&gt;
     * &lt;de v=&quot;{desc|escape|tabindent}&quot;/&gt;
     * &lt;fl v=&quot;{files}&quot;/&gt;
     * &lt;fa v=&quot;{file_adds}&quot;/&gt;
     * &lt;fd v=&quot;{file_dels}&quot;/&gt;
     * &lt;/cs&gt;
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

        File hgRoot = MercurialTeamProvider.getHgRoot(res);
        String myInput = "<top>".concat(input).concat("</top>");
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(getHandler(res, direction, repository,
                    bundleFile, hgRoot, fileRevisions));
            reader.parse(new InputSource(new ByteArrayInputStream(myInput
                    .getBytes())));
        } catch (Exception e) {
            String nextTry = cleanControlChars(myInput);
            try {
                XMLReader reader = XMLReaderFactory.createXMLReader();
                reader.setContentHandler(getHandler(res, direction, repository,
                        bundleFile, hgRoot, fileRevisions));
                reader.parse(new InputSource(new ByteArrayInputStream(nextTry
                        .getBytes())));
            } catch (Exception e1) {
                throw new HgException(e1.getLocalizedMessage(), e);
            }
        }
        return fileRevisions;
    }

    /**
     * @param hgRoot
     * @param bundleFile
     * @param repository
     * @param direction
     * @param res
     * @param fileRevisions
     * @return
     */
    private static ContentHandler getHandler(IResource res,
            Direction direction, HgRepositoryLocation repository,
            File bundleFile, File hgRoot,
            Map<IPath, SortedSet<ChangeSet>> fileRevisions) {
        handler = new ChangesetContentHandler(res, direction, repository,
                bundleFile, hgRoot, fileRevisions);
        return handler;
    }

    /**
     * Clean the string of special chars that might be invalid for the XML
     * parser. Return the cleaned string (special chars replaced by ordinary
     * spaces).
     * 
     * @param str
     *            the string to clean
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
}
