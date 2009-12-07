/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Bastian Doetsch	implementation
 * 		Andrei Loskutov (Intland) - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * This class helps HgClients to parse the changeset output of hg to Changeset
 * objects.
 *
 * @author Bastian Doetsch
 */
final class ChangesetContentHandler implements ContentHandler {

	private static final String[] EMPTY = new String[0];
	private static final Pattern LT = Pattern.compile("&lt;");
	private static final Pattern GT = Pattern.compile("&gt;");
	private static final Pattern AMP = Pattern.compile("&amp;");
	private static final Pattern NEWLINE_TAB = Pattern.compile("\n\t");
	private static final Pattern WORDS =  Pattern.compile(" ");

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
	private final Set<String> filesModified;
	private final Set<String> filesAdded;
	private final Set<String> filesRemoved;
	private Action action;

	ChangesetContentHandler(IPath res, Direction direction, HgRepositoryLocation repository,
			File bundleFile, HgRoot hgRoot, Map<IPath, Set<ChangeSet>> fileRevisions) {

		this.res = res;
		this.direction = direction;
		this.repository = repository;
		this.bundleFile = bundleFile;
		this.hgRoot = hgRoot;
		this.fileRevisions = fileRevisions;
		filesModified = new TreeSet<String>();
		filesAdded = new TreeSet<String>();
		filesRemoved = new TreeSet<String>();
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

	public void endElement(String uri, String localName, String name) throws SAXException {

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

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	public void processingInstruction(String target, String data) throws SAXException {
	}

	public void setDocumentLocator(Locator locator) {
	}

	public void skippedEntity(String name) throws SAXException {
	}

	public void startDocument() throws SAXException {
	}

	public void startElement(String uri, String localName, String name, Attributes atts)
			throws SAXException {
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