package com.vectrace.MercurialEclipse.annotations;

import java.util.List;

import junit.framework.TestCase;

public class AnnotateCommandTest extends TestCase
{
  private List<AnnotateBlock> createFromStdOut(String name)
  {
    return AnnotateCommand.createFromStdOut(
        getClass().getResourceAsStream(name + ".out")).getAnnotateBlocks();
  }

  public void test1()
  {
    List<AnnotateBlock> blocks = createFromStdOut("annotate1");
    assertEquals(48, blocks.size());

    blah(blocks, 0, "zingo", "146:16cd70529433", "Tue Feb 05 21:17:52 2008 +0100", 0, 0);
    blah(blocks, 1, "zingo", "151:893d61d581c6", "Thu Mar 27 22:47:06 2008 +0100", 1, 5);
  }

  private void blah(List<AnnotateBlock> blocks, int i, String user, String rev, String date, int startLine, int endLine)
  {
    AnnotateBlock block = blocks.get(i);
    assertEquals(user, block.getUser());
    assertEquals(rev, block.getRevision().toString());
    assertEquals(startLine, block.getStartLine());
    assertEquals(endLine, block.getEndLine());
    assertEquals(date, AnnotateCommand.DATE_FORMAT.format(block.getDate()));
  }
}
