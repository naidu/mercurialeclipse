/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	   - implementation
 * Philip Graf - Fixed bugs which FindBugs found
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.search.core.text.TextSearchMatchAccess;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.JHgChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchMatchAccess extends TextSearchMatchAccess {
	private final HgRoot root;
	private final JHgChangeSet changeset;
	private final int lineNumber;
	private final String user;
	private final IFile file;
	private final boolean becomesMatch;
	private final String extract;
	private String fileContent;
	private HgFile hgFile;

	/**
	 * Expects a line like filename:rev:linenumber:-|+:username:date
	 *
	 * @param line
	 * @throws HgException
	 */
	public MercurialTextSearchMatchAccess(HgRoot root, String line, boolean all) throws HgException {
		this.root = root;
		try {
			String[] split = line.trim().split(":", all ? 6 : 5);
			int i=0;
			Path path = new Path(root.getAbsolutePath() + File.separator + split[i++]);
			this.file = ResourceUtils.getFileHandle(path);

			this.changeset = LocalChangesetCache.getInstance().get(root, Integer.parseInt(split[i++]));
			this.lineNumber = Integer.parseInt(split[i++]);
			if (all) {
				this.becomesMatch = !"-".equals(split[i++]);
			} else {
				this.becomesMatch = true;
			}
			this.user = split[i++];
			this.extract = split[i++];
		} catch (Exception e) {
			// result is not correctly formed or the line is not a search result entry
			throw new HgException("Failed to parse search result", e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (becomesMatch ? 1231 : 1237);
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + lineNumber;
		result = prime * result + changeset.hashCode();
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MercurialTextSearchMatchAccess other = (MercurialTextSearchMatchAccess) obj;
		if (becomesMatch != other.becomesMatch) {
			return false;
		}
		if (file == null) {
			if (other.file != null) {
				return false;
			}
		} else if (!file.getFullPath().equals(other.file.getFullPath())) {
			return false;
		}
		if (lineNumber != other.lineNumber) {
			return false;
		}
		if (changeset != other.changeset) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
			return false;
		}
		return true;
	}

	public ChangeSet getRev() {
		return changeset;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getUser() {
		return user;
	}

	@Override
	public IFile getFile() {
		return file;
	}

	public boolean isBecomesMatch() {
		return becomesMatch;
	}

	@Override
	public String getFileContent(int offset, int length) {
		String sub = getFileContent();
		if (sub.length() > 0) {
			return sub.substring(offset, offset + length);
		}
		return "";
	}

	/**
	 * @return
	 *
	 */
	private String getFileContent() {
		if (fileContent == null) {
			BufferedReader reader = null;
			try {
				InputStream is = getHgFile().getContents();

				if (is != null) {
					StringBuilder sb = new StringBuilder();
					String line;

					reader = new BufferedReader(
							new InputStreamReader(is, root.getEncoding()));
					while ((line = reader.readLine()) != null) {
						sb.append(line).append("\n");
					}
					this.fileContent = sb.toString();
				}
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			} finally {
				try {
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
				}
			}
		}
		if (fileContent == null) {
			fileContent = "";
		}
		return fileContent;
	}

	@Override
	public char getFileContentChar(int offset) {
		String sub = getFileContent(offset, 1);
		if (sub != null && sub.length() > 0) {
			return sub.charAt(0);
		}
		return ' ';
	}

	@Override
	public int getFileContentLength() {
		return getFileContent().length();
	}

	@Override
	public int getMatchLength() {
		return extract.length();
	}

	@Override
	public int getMatchOffset() {
		return getFileContent().indexOf(extract);
	}

	public HgRoot getRoot() {
		return root;
	}

	public String getExtract() {
		return extract;
	}

	public HgFile getHgFile() {
		if (hgFile == null) {
			hgFile = HgFile.make(changeset, file);
		}
		return hgFile;
	}
}
