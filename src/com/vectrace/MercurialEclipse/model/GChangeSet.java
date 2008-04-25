/**
 * 
 */
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.List;

public class GChangeSet
{
  private int rev_index;
  private List<Edge> edges = new ArrayList<Edge>();
  private List<Integer> jumps = new ArrayList<Integer>();
  private int rev_length;
  private int n_columns_diff;
  private int index;
  private boolean working;
  
  public GChangeSet(int index, String before, String after)
  {
    this.index = index;
    this.working = before.indexOf('@') > -1;
    rev_length = before.length() / 2 + 1;
    rev_index = Math.max(before.indexOf('@'), before.indexOf('o')) / 2;
    
    int diff = 0;
    if(after.indexOf('/') > 0)
    {
      diff--;
    }
    else if(after.indexOf('\\') > 0)
    {
      diff++;
    }
    n_columns_diff = diff;
    
    int dbl = rev_index*2;
    if(after.length() > dbl) 
    {
      char a = after.charAt(dbl);
      if(a == '|')
      {
        edges.add(new Edge(rev_index, rev_index));
      }
      dbl++;
      if(after.length() > dbl) 
      {
        char b = after.charAt(dbl);
        if(b == '\\')
        {
          edges.add(new Edge(rev_index, rev_index + 1));
        }
      }
    }
    
    int plus = before.indexOf('+');
    if(plus > -1)
    {
      jumps.add(plus/2);
    }
    
  }
  
  public int getColumnsDiff()
  {
    return n_columns_diff;
  }
  
  public int getRevIndex()
  {
    return rev_index;
  }
  
  public int getRevLength()
  {
    return rev_length;
  }
  
  public List<Edge> getEdges()
  {
    return edges;
  }
  
  public List<Integer> getJumps()
  {
    return jumps;
  }
  
  public int getIndex()
  {
    return index;
  }
  
  public boolean isWorking()
  {
    return working;
  }
  
  public static class Edge
  {
    private int x, y;

    public Edge(int x, int y)
    {
      this.x = x;
      this.y = y;
    }
    
    public int getX()
    {
      return x;
    }
    
    public int getY()
    {
      return y;
    }
    
    @Override
    public String toString()
    {
      return "("+x+","+y+")";
    }
  }
}