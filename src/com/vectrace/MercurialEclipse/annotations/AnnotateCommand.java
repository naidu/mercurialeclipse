/*******************************************************************************
 * Copyright (c) Subclipse and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     StefanC           - remove empty lines, code cleenup
 *     Jérôme Nègre      - make it work
 *     Bastian Doetsch   - refactorings
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.annotations;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.commands.AbstractClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class AnnotateCommand {
	private static final Pattern ANNOTATE = Pattern
			.compile("^\\s*(.+[^ ])\\s+(\\w+)\\s+(\\w+)\\s+(\\w+ \\w+ \\w+ \\w+:\\w+:\\w+ \\w+ [\\+\\-]\\w+).*: (.*)$"); //$NON-NLS-1$

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH); //$NON-NLS-1$

	private final File file;

	public AnnotateCommand(File remoteFile) {
		this.file = remoteFile;
	}

	public AnnotateBlocks execute() throws HgException {
		IFile resource = (IFile) ResourceUtils.convert(file);

		if (!MercurialUtilities.hgIsTeamProviderFor(resource, true)) {
			return null;
		}
		HgRoot root = AbstractClient.getHgRoot(resource);
		String relPath = root.toRelative(resource.getLocation().toFile());
		String launchCmd[] = { MercurialUtilities.getHGExecutable(),
				"annotate", "--follow", "--user", "--number", "--changeset", "--date", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
				"--", relPath }; //$NON-NLS-1$

		String output = MercurialUtilities.executeCommand(launchCmd, root, true);
		if (output == null) {
			return null;
		}
		return createFromStdOut(new StringReader(output));
	}

	protected static AnnotateBlocks createFromStdOut(InputStream contents) {
		return createFromStdOut(new InputStreamReader(contents));
	}

	protected static synchronized AnnotateBlocks createFromStdOut(Reader contents) {
		AnnotateBlocks blocks = new AnnotateBlocks();
		try {
			BufferedReader reader = new BufferedReader(contents);
			int count = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() == 0) {
					// ignore empty lines
					continue;
				}

				Matcher matcher = ANNOTATE.matcher(line);
				matcher.find();
				String author = matcher.group(1);
				int revision = Integer.parseInt(matcher.group(2));
				String changeset = matcher.group(3);
				Date date = DATE_FORMAT.parse(matcher.group(4));
				blocks.add(new AnnotateBlock(
						new HgRevision(changeset, revision), author, date,
						count, count));
				count++;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return blocks;
	}
}
