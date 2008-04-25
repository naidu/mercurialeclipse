package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.model.ChangeLogTest;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet.Edge;

public class HgGLogClientTest extends TestCase
{
  public void test()
  {
    List<GChangeSet> changeSets = createFrom("glog");
    assertEquals(14, changeSets.size());
    checkGraph(changeSets, 1, 226, 0, 1, 1, new int[][]{{0,0},{0,1}}, new int[]{});
    checkGraph(changeSets, 2, 225, 1, 2, 0, new int[][]{{1,1}}, new int[]{});
    checkGraph(changeSets, 3, 188, 0, 2, -1, new int[][]{{0,0}}, new int[]{});
  }

  private List<GChangeSet> createFrom(String name)
  {
    try
    {
      return new HgGLogClient(ChangeLogTest.toString(getClass().getResourceAsStream(name+".txt"))).getChangeSets();
    } catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
  
  private void checkGraph(List<GChangeSet> graph, int index, int cr, int ri, int rl, int cd, int[][] edges, int[] jumps)
  {
    GChangeSet cs = graph.get(index);
    assertEquals(ri, cs.getRevIndex());
    assertEquals(rl, cs.getRevLength());
    assertEquals(cd, cs.getColumnsDiff());
    
    for(int i=0;i<edges.length;i++)
    {
      Edge e2 = cs.getEdges().get(i);
      assertEquals(edges[i][0], e2.getX());
      assertEquals(edges[i][1], e2.getY());
    }
    
    for(int i=0;i<jumps.length;i++)
    {
      assertEquals(jumps[i], cs.getJumps().get(i).intValue());
    }
  }
}
