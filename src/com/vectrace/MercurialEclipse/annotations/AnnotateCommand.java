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
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgFile;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class AnnotateCommand {
    private static final Pattern ANNOTATE = Pattern
            .compile("^\\s*(.+[^ ])\\s+(\\w+)\\s+(\\w+)\\s+(\\w+ \\w+ \\w+ \\w+:\\w+:\\w+ \\w+ [\\+\\-]\\w+).*: (.*)$"); //$NON-NLS-1$
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat(
            "EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH); //$NON-NLS-1$

    private final HgFile file;

    public AnnotateCommand(HgFile file) {
        this.file = file;
    }

    public AnnotateBlocks execute() throws HgException {
        IFile resource = (IFile) MercurialUtilities.convert(file);

        if (!MercurialUtilities.hgIsTeamProviderFor(resource, true)) {
            return null;
        }
        File workingDir = MercurialUtilities.getWorkingDir(resource);
        String FullPath = MercurialUtilities.getResourceName(resource);
        String launchCmd[] = { MercurialUtilities.getHGExecutable(),
                "annotate", "--user", "--number", "--changeset", "--date", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                "--", FullPath }; //$NON-NLS-1$

        String output = MercurialUtilities.executeCommand(launchCmd,
                workingDir, true);
        if (output == null) {
            return null;
        }
        return createFromStdOut(new StringReader(output));
    }

    protected static AnnotateBlocks createFromStdOut(InputStream contents) {
        return createFromStdOut(new InputStreamReader(contents));
    }

    protected static AnnotateBlocks createFromStdOut(Reader contents) {
        AnnotateBlocks blocks = new AnnotateBlocks();
        String line = null;
        try {
            BufferedReader reader = new BufferedReader(contents);
            int count = 0;
            for (line = reader.readLine(); line != null; line = reader
                    .readLine()) {
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
