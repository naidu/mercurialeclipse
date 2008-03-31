/*******************************************************************************
 * Copyright (c)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     StefanC           - remove empty lines, code cleenup
 *     Jérôme Nègre      - make it work
 *******************************************************************************/

package com.vectrace.MercurialEclipse.annotations;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.HgFile;
import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class AnnotateCommand
{
  private static final Pattern ANNOTATE = Pattern
      .compile("^\\s*(.+[^ ])\\s+(\\w+)\\s+(\\w+)\\s+(\\w+ \\w+ \\w+ \\w+:\\w+:\\w+ \\w+ [\\+\\-]\\w+).*: (.*)$");
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
      "EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);

  private final HgFile file;
  private final AnnotateBlocks blocks = new AnnotateBlocks();

  public AnnotateCommand(HgFile file)
  {
    this.file = file;
  }

  public AnnotateBlocks execute() throws HgException
  {
    run();
    return blocks;
  }

  private void run() throws HgException
  {
    IFile resource = file.getFile();
    if (!MercurialUtilities.isResourceInReposetory(resource, true)) {
        return;
    }
    File workingDir = MercurialUtilities.getWorkingDir(resource);
    String FullPath = MercurialUtilities.getResourceName(resource);
    String launchCmd[] =
    { MercurialUtilities.getHGExecutable(), "annotate", "--user", "--number",
        "--changeset", "--date", "--", FullPath };

    String output = MercurialUtilities.ExecuteCommand(launchCmd, workingDir, true);
    if (output == null) {
        return;
    }
    try
    {
      createFromStdOut(output.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void createFromStdOut(byte[] contents)
  {
    createFromStdOut(new ByteArrayInputStream(contents));
  }

  private void createFromStdOut(InputStream contents)
  {
    String line = null;
    try
    {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(contents));
      int count = 0;
      for (line = reader.readLine(); line != null; line = reader.readLine())
      {
        if(line.trim().length() == 0) {
            // ignore empty lines
            continue;
        }
        
        Matcher matcher = ANNOTATE.matcher(line);
        matcher.find();
        String author = matcher.group(1);
        long revision = Long.parseLong(matcher.group(2));
        String changeset = matcher.group(3);
        Date date = DATE_FORMAT.parse(matcher.group(4));
        blocks.add(new AnnotateBlock(new HgRevision(changeset, revision),
            author, date, count, count));
        count++;
      }
    } catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }
}
