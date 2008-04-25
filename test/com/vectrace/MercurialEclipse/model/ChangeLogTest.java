package com.vectrace.MercurialEclipse.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import junit.framework.TestCase;

public class ChangeLogTest extends TestCase
{
  public static List<ChangeSet> createFromStdOut(String name)
  {
    try
    {
      return ChangeLog.createFromStdOut(toString(ChangeLogTest.class.getResourceAsStream(name + ".out")));
    } 
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  // It shouldn't have to be so hard!!!
  public static String toString(InputStream stream) throws IOException
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(stream));
    StringBuffer buffer = new StringBuffer();
    String line = null;
    while ((line = in.readLine()) != null) {
      buffer.append(line);
      buffer.append(System.getProperty("line.separator"));
    }
    in.close();
    return buffer.toString();
  }

  public void test1()
  {
    List<ChangeSet> logs = createFromStdOut("log1");
    assertEquals(3, logs.size());

    checkLog(logs, 0, 39, "9224c59c4f17", "tip", "zingo", "Sun Aug 20 00:07:53 2006 +0200", "META-INF/MANIFEST.MF plugin.xml", "Fix ticket #17 in a better way");
    checkLog(logs, 1, 37, "284d93450d4b",  null, "zingo@localhost", "Sat Aug 19 23:28:43 2006 +0200", "plugin.xml", "Team popup only if under hg repository... Fixes ticket #17");
    checkLog(logs, 2, 36, "7f30553655f4",  null, "zingo@localhost", "Sat Aug 19 23:13:15 2006 +0200", "plugin.xml", "Moved Prefs to team section fixes ticket #16");
  }

  private void checkLog(List<ChangeSet> logs, int i, int rev, String changeset, String tag, String user, String date, String files, String description)
  {
    ChangeSet log = logs.get(i);
    assertEquals(rev, log.getChangesetIndex());
    assertEquals(rev+":"+changeset, log.getChangeset());
    assertEquals(tag, log.getTag());
    assertEquals(user, log.getUser());
    assertEquals(date, log.getDate());
    assertEquals(description, log.getDescription());
  }
}
