/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 * Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
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
import com.vectrace.MercurialEclipse.model.HgRoot;
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
	private static final class ChangesetContentHandler implements ContentHandler {

		private static final String[] EMPTY = new String[] {};
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
		private StringBuilder chars;
		private final IPath res;
		private final Direction direction;
		private final HgRepositoryLocation repository;
		private final File bundleFile;
		private final HgRoot hgRoot;
		private final Map<IPath, Set<ChangeSet>> fileRevisions;
		private final Set<String> filesModified = new TreeSet<String>();
		private final Set<String> filesAdded = new TreeSet<String>();
		private final Set<String> filesRemoved = new TreeSet<String>();
		private Action action;
		private static final Pattern LT = Pattern.compile("&lt;");
		private static final Pattern GT = Pattern.compile("&gt;");
		private static final Pattern AMP = Pattern.compile("&amp;");
		private static final Pattern NEWLINE_TAB = Pattern.compile("\n\t");
		private static final Pattern WORDS =  Pattern.compile(" ");

		public ChangesetContentHandler(IPath res, Direction direction,
				HgRepositoryLocation repository, File bundleFile, HgRoot hgRoot,
				Map<IPath, Set<ChangeSet>> fileRevisions) {
			this.res = res;
			this.direction = direction;
			this.repository = repository;
			this.bundleFile = bundleFile;
			this.hgRoot = hgRoot;
			this.fileRevisions = fileRevisions;
		}

		private static String replaceAll(Pattern p, String source, String replacement){
			return p.matcher(source).replaceAll(replacement);
		}

		private static String unescape(String string) {
			String result = replaceAll(LT, string, "<");
			result = replaceAll(GT, result, ">");
			return replaceAll(AMP, result, "&");
		}

		/**
		 * Remove a leading tab on each line in the string.
		 *
		 * @param string
		 * @return
		 */
		private static String untab(String string) {
			return replaceAll(NEWLINE_TAB, string, "\n"); //$NON-NLS-1$
		}


		private static String[] splitWords(String string) {
			if (string == null || string.length() == 0) {
				return EMPTY;
			}
			return WORDS.split(string);
		}

		public void characters(char[] ch, int start, int length) {
			chars.append(ch, start, length);
		}

		public void endDocument() throws SAXException {
		}

		public void endElement(String uri, String localName, String name)
		throws SAXException {

			if (name.equals("de")) { //$NON-NLS-1$
				de = chars.toString();
			} else if (name.equals("cs")) { //$NON-NLS-1$


				ChangeSet.Builder csb = new ChangeSet.Builder(rv, nl, br, di, unescape(au), hgRoot);
				csb.tag(tg);
				csb.nodeShort(ns);
				csb.ageDate(da);
				csb.description(untab(unescape(de)));
				csb.parents(splitWords(pr));

				csb.bundleFile(bundleFile);
				csb.direction(direction);
				csb.repository(repository);

				List<FileStatus> list = new ArrayList<FileStatus>(
						filesModified.size() + filesAdded.size() + filesRemoved.size());
				for (String file : filesModified) {
					list.add(new FileStatus(FileStatus.Action.MODIFIED, file, hgRoot));
				}
				for (String file : filesAdded) {
					list.add(new FileStatus(FileStatus.Action.ADDED, file, hgRoot));
				}
				for (String file : filesRemoved) {
					list.add(new FileStatus(FileStatus.Action.REMOVED, file, hgRoot));
				}
				csb.changedFiles(list.toArray(new FileStatus[list.size()]));

				ChangeSet changeSet = csb.build();

				// changeset to resources & project
				addChangesetToResourceMap(changeSet);
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
			 * <de>{desc|escape|tabindent}</de>
			 */
			chars = new StringBuilder(42);
			if (name.equals("br")) { //$NON-NLS-1$
				br = atts.getValue(0);
			} else if (name.equals("tg")) { //$NON-NLS-1$
				tg = atts.getValue(0);
			} else if (name.equals("rv")) { //$NON-NLS-1$
				rv = Integer.parseInt(atts.getValue(0));
			} else if (name.equals("ns")) { //$NON-NLS-1$
				ns = atts.getValue(0);
			} else if (name.equals("nl")) { //$NON-NLS-1$
				nl = atts.getValue(0);
			} else if (name.equals("di")) { //$NON-NLS-1$
				di = atts.getValue(0);
			} else if (name.equals("da")) { //$NON-NLS-1$
				da = atts.getValue(0);
			} else if (name.equals("au")) { //$NON-NLS-1$
				au = atts.getValue(0);
			} else if (name.equals("pr")) { //$NON-NLS-1$
				pr = atts.getValue(0);
				/*  } else if (name.equals("de")) {
				de = untab(unescape(atts.getValue(0))); */
			} else if (name.equals("fl")) { //$NON-NLS-1$
				action = FileStatus.Action.MODIFIED;
			} else if (name.equals("fa")) { //$NON-NLS-1$
				action = FileStatus.Action.ADDED;
			} else if (name.equals("fd")) { //$NON-NLS-1$
				action = FileStatus.Action.REMOVED;
			} else if (name.equals("f")) { //$NON-NLS-1$
				if (action == Action.ADDED) {
					String value = atts.getValue(0);
					filesAdded.add(value);
					filesModified.remove(value);
				} else if (action == Action.MODIFIED) {
					filesModified.add(atts.getValue(0));
				} else if (action == Action.REMOVED) {
					String value = atts.getValue(0);
					filesRemoved.add(value);
					filesModified.remove(value);
				}
			}
		}

		public void startPrefixMapping(String prefix, String uri) throws SAXException {
		}

		private final void addChangesetToResourceMap(final ChangeSet cs) {

			if (cs.getChangedFiles() != null) {
				for (FileStatus file : cs.getChangedFiles()) {
					IPath fileAbsPath = file.getAbsolutePath();
					Set<ChangeSet> revs = addChangeSetRevisions(cs,	fileAbsPath);
					fileRevisions.put(fileAbsPath, revs);
				}
			}

//			Set<ChangeSet> projectRevs = addChangeSetRevisions(cs, repoPath);
//
//			fileRevisions.put(repoPath, projectRevs);

			Set<ChangeSet> pathRevs = addChangeSetRevisions(cs, res);
			fileRevisions.put(res, pathRevs);
		}

		private Set<ChangeSet> addChangeSetRevisions(ChangeSet cs, IPath path) {
			Set<ChangeSet> fileRevs = fileRevisions.get(path);
			if (fileRevs == null) {
				fileRevs = new HashSet<ChangeSet>();
			}
			fileRevs.add(cs);
			return fileRevs;
		}
	}

	private static final String STYLE_SRC = "/styles/log_style"; //$NON-NLS-1$
	private static final String STYLE = "/log_style"; //$NON-NLS-1$
	private static final String STYLE_WITH_FILES_SRC = "/styles/log_style_with_files"; //$NON-NLS-1$
	private static final String STYLE_WITH_FILES = "/log_style_with_files"; //$NON-NLS-1$
	private static final String STYLE_TEMP_EXTN = ".tmpl"; //$NON-NLS-1$

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

		if (stylefile.canRead() && tmplfile.canRead()) {
			// Already have copies, return the file reference to the style file
			stylefile.deleteOnExit();
			tmplfile.deleteOnExit();
			return stylefile;
		}

		ClassLoader cl = AbstractParseChangesetClient.class.getClassLoader();
		// Need to make copies into the state directory from the jar file.
		// set delete on exit so a new copy is made each time eclipse is started
		// so we don't use stale copies on plugin updates.
		InputStream styleistr = cl.getResourceAsStream(style_src);
		InputStream tmplistr = cl.getResourceAsStream(style_tmpl_src);
		OutputStream styleostr = null;
		OutputStream tmplostr = null;
		try {
			styleostr = new FileOutputStream(stylefile);
			tmplostr = new FileOutputStream(tmplfile);
			tmplfile.deleteOnExit();

			byte buffer[] = new byte[1024];
			int n;
			while ((n = styleistr.read(buffer)) != -1) {
				styleostr.write(buffer, 0, n);
			}
			while ((n = tmplistr.read(buffer)) != -1) {
				tmplostr.write(buffer, 0, n);
			}
			return stylefile;
		} catch (IOException e) {
			throw new HgException("Failed to setup hg style file", e); //$NON-NLS-1$
		} finally {
			try {
				if(styleostr != null) {
					styleostr.close();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
			try {
				if(tmplostr != null) {
					tmplostr.close();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
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
	 * @param withFiles
	 *            Are files included in the log output
	 * @param direction
	 *            Incoming, Outgoing or Local changesets
	 * @param repository
	 * @param bundleFile
	 * @return map where the key is an absolute file path
	 * @throws HgException
	 */
	protected final static Map<IPath, Set<ChangeSet>> createMercurialRevisions(
			IResource res, String input, boolean withFiles,
			Direction direction, HgRepositoryLocation repository,
			File bundleFile) throws HgException {

		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(res);
		IPath path = res.getLocation();

		return createMercurialRevisions(path, input, direction, repository, bundleFile, hgRoot);
	}

	/**
	 * @param path full absolute file path, which MAY NOT EXIST in the local file system
	 *        (because it is the original path of moved or renamed file)
	 *
	 * @return map where the key is an absolute file path
	 * @throws HgException
	 */
	protected static Map<IPath, Set<ChangeSet>> createMercurialRevisions(IPath path, String input, Direction direction,
			HgRepositoryLocation repository, File bundleFile, HgRoot hgRoot)
			throws HgException {
		Map<IPath, Set<ChangeSet>> fileRevisions = new HashMap<IPath, Set<ChangeSet>>();

		if (input == null || input.length() == 0) {
			return fileRevisions;
		}
		String myInput = "<top>" + input + "</top>"; //$NON-NLS-1$ //$NON-NLS-2$
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(getHandler(path, direction, repository,
					bundleFile, hgRoot, fileRevisions));
			reader.parse(new InputSource(new StringReader(myInput)));
		} catch (Exception e) {
			String nextTry = cleanControlChars(myInput);
			try {
				XMLReader reader = XMLReaderFactory.createXMLReader();
				reader.setContentHandler(getHandler(path, direction, repository,
						bundleFile, hgRoot, fileRevisions));
				reader.parse(new InputSource(new StringReader(nextTry)));
			} catch (Exception e1) {
				throw new HgException(e1.getLocalizedMessage(), e);
			}
		}
		return fileRevisions;
	}

	private static ContentHandler getHandler(IPath res,
			Direction direction, HgRepositoryLocation repository,
			File bundleFile, HgRoot hgRoot,
			Map<IPath, Set<ChangeSet>> fileRevisions) {
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
			} else if (ch == '&') {
				buf.append("&amp;");
			} else if (ch == '"') {
				buf.append("\"");
			}else {
				buf.appendCodePoint(ch);
			}
		}
		return buf.toString();
	}
}
