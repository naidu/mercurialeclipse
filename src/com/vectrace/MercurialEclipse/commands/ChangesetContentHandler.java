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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
import com.vectrace.MercurialEclipse.team.cache.RemoteData;

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

	private String branches;
	private String tags;
	private int rev;
	private String nodeShort;
	private String nodeLong;
	private String dateIso;
	private String dateAge;
	private String author;
	private String parents;
	private String description;
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
	private String prevNodeShort;
	private int prevRev;
	private final String branch;
	private final IPath repoPath;
	private final boolean withFiles;


	ChangesetContentHandler(IPath res, boolean withFiles, Direction direction, HgRepositoryLocation repository,
			File bundleFile, String branch, HgRoot hgRoot) {

		this.res = res;
		this.withFiles = withFiles;
		this.direction = direction;
		this.repository = repository;
		this.bundleFile = bundleFile;
		this.hgRoot = hgRoot;
		repoPath = new Path(hgRoot.getPath());
		this.branch = branch;
		this.fileRevisions = new HashMap<IPath, Set<ChangeSet>>();
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
		return replaceAll(NEWLINE_TAB, string, "\n");
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

		if (name.equals("de")) {
			description = chars.toString();
		} else if (name.equals("cs")) {
			ChangeSet.Builder csb = new ChangeSet.Builder(rev, nodeLong, branches, dateIso, unescape(author), hgRoot);
			csb.tag(tags);
			csb.nodeShort(nodeShort);
			csb.ageDate(dateAge);
			csb.description(untab(unescape(description)));

			addParentsInfo(csb);

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
			prevRev = rev;
			prevNodeShort = nodeShort;
		}
	}

	/**
	 * It seems that hg do not report parents if the parent changeset is printed out in
	 * the same command output. So we guess: if parents are empty, we have to create them
	 * from the revision + short node of the previous run.
	 * @param csb
	 */
	private void addParentsInfo(ChangeSet.Builder csb) {
		String[] myParents = splitWords(parents);
		if(myParents.length == 0 && prevRev == rev - 1 && prevNodeShort != null){
			myParents = new String[]{prevRev + ":" + prevNodeShort};
		}
		csb.parents(myParents);
	}

	/**
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
	 * &lt;f v=&quot;{root relative path}&quot;/&gt;
	 * &lt;/cs&gt;
	 * </pre>
	 * {@inheritDoc}
	 */
	public void startElement(String uri, String localName, String name, Attributes atts)
			throws SAXException {
		/*
		 * <br v="{branches}"/> <tg v="{tags}"/> <rv v="{rev}"/> <ns
		 * v="{node|short}"/> <nl v="{node}"/> <di v="{date|isodate}"/> <da
		 * v="{date|age}"/> <au v="{author|person}"/> <pr v="{parents}"/>
		 * <de>{desc|escape|tabindent}</de>
		 */
		chars = new StringBuilder(42);
		if (name.equals("br")) {
			branches = atts.getValue(0);
		} else if (name.equals("tg")) {
			tags = atts.getValue(0);
		} else if (name.equals("rv")) {
			rev = Integer.parseInt(atts.getValue(0));
		} else if (name.equals("ns")) {
			nodeShort = atts.getValue(0);
		} else if (name.equals("nl")) {
			nodeLong = atts.getValue(0);
		} else if (name.equals("di")) {
			dateIso = atts.getValue(0);
		} else if (name.equals("da")) {
			dateAge = atts.getValue(0);
		} else if (name.equals("au")) {
			author = atts.getValue(0);
		} else if (name.equals("pr")) {
			parents = atts.getValue(0);
		/*  } else if (name.equals("de")) {
			de = untab(unescape(atts.getValue(0)));
		*/
		} else if (name.equals("fl")) {
			action = FileStatus.Action.MODIFIED;
		} else if (name.equals("fa")) {
			action = FileStatus.Action.ADDED;
		} else if (name.equals("fd")) {
			action = FileStatus.Action.REMOVED;
		} else if (name.equals("f")) {
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

	private final void addChangesetToResourceMap(final ChangeSet cs) {
		if(withFiles) {
			if (cs.getChangedFiles() != null) {
				for (FileStatus file : cs.getChangedFiles()) {
					IPath fileAbsPath = file.getAbsolutePath();
					mapPathToChangeset(fileAbsPath, cs);
				}
			}
		} else {
			mapPathToChangeset(repoPath, cs);
		}
		mapPathToChangeset(res, cs);
	}

	private void mapPathToChangeset(IPath path, ChangeSet cs) {
		Set<ChangeSet> fileRevs = fileRevisions.get(path);
		if (fileRevs == null) {
			fileRevs = new HashSet<ChangeSet>();
		}
		fileRevs.add(cs);
		fileRevisions.put(path, fileRevs);
	}

	public Map<IPath, Set<ChangeSet>> getFileRevisions() {
		return fileRevisions;
	}

	public RemoteData createRemoteData() {
		return new RemoteData(repository, hgRoot, direction, branch, fileRevisions);
	}

	//####################################################################################

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

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}
}