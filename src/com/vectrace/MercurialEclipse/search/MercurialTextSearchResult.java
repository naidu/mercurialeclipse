/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
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
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchResult extends TextSearchMatchAccess {
	private final HgRoot root;
	private int rev;
	private int lineNumber;
	private String user;
	private String date;
	private IFile file;
	private boolean becomesMatch;
	private final String extract;
	private String fileContent;

	/**
	 * Expects a line like filename:rev:linenumber:-|+:username:date
	 *
	 * @param line
	 */
	public MercurialTextSearchResult(HgRoot root, String line) {
		this.root = root;
		String[] split = line.trim().split(":");
		Path path = new Path(root.getAbsolutePath() + File.separator + split[0]);
		this.file = ResourceUtils.getFileHandle(path);
		this.rev = Integer.parseInt(split[1]);
		this.lineNumber = Integer.parseInt(split[2]);
		this.becomesMatch = split[3].equals("-") ? false : true;
		this.user = split[4];
		this.date = split[5];
		this.extract = split[6];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (becomesMatch ? 1231 : 1237);
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + lineNumber;
		result = prime * result + rev;
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
		MercurialTextSearchResult other = (MercurialTextSearchResult) obj;
		if (becomesMatch != other.becomesMatch) {
			return false;
		}
		if (date == null) {
			if (other.date != null) {
				return false;
			}
		} else if (!date.equals(other.date)) {
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
		if (rev != other.rev) {
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

	public int getRev() {
		return rev;
	}

	public void setRev(int rev) {
		this.rev = rev;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	@Override
	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}

	public boolean isBecomesMatch() {
		return becomesMatch;
	}

	public void setBecomesMatch(boolean becomesMatch) {
		this.becomesMatch = becomesMatch;
	}

	@Override
	public String getFileContent(int offset, int length) {
		getFileContent();
		return fileContent.substring(offset, length);
	}

	/**
	 * @return
	 *
	 */
	private String getFileContent() {
		if (fileContent == null) {
			MercurialRevisionStorage mrs = new MercurialRevisionStorage(file,
					rev);
			InputStream is = null;
			try {
				is = mrs.getContents();

				if (is != null) {
					StringBuilder sb = new StringBuilder();
					String line;

					BufferedReader reader = new BufferedReader(
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
					if (is != null) {
						is.close();
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
		return getFileContent(offset, 1).charAt(0);
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

}
