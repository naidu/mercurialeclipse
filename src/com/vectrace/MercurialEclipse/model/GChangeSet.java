/**
 * 
 */
package com.vectrace.MercurialEclipse.model;

import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.model.GChangeSet.Edge.EdgeType;

public class GChangeSet
{
  private List<Edge> middle = new ArrayList<Edge>();
  private List<Edge> after = new ArrayList<Edge>();
  private int index;
  private final RowCount rowCount;
  
  public GChangeSet(RowCount rowCount, int index, String middleS, String afterS)
  {
    this.rowCount = rowCount;
    this.index = index;
    parse(middle, middleS);
    parse(after, afterS);
  }
  
  private void parse(List<Edge> list, String string)
  {
    int length = string.length();
    int count = 0;
    for(int i=0;i<length;i++)
    {
      count += addEdge(list, string, i, count);
    }
    rowCount.endRow();
    if(string.contains("+"))
    {
      rowCount.jump = string.indexOf('o');
    }
  }

  private int addEdge(List<Edge> list, String string, int i, int count)
  {
    char c = string.charAt(i);
    if(c == ' ') {
      return rowCount.space(i, count);
    }
    Edge edge = new Edge(c, count);
    return rowCount.update(list, edge);
  }

  public int getIndex()
  {
    return index;
  }
  
  public List<Edge> getMiddle()
  {
    return middle;
  }
  
  public List<Edge> getAfter()
  {
    return after;
  }
  
  public static class Col
  {
    int id = 0;
    
    public Col(int i)
    {
      this.id = i;
    }
  }
  
  public static class RowCount
  {
    public int jump;
    public List<Col> cols = new ArrayList<Col>();
    private int unique = 0;
    private Edge lastEdge;
    private int dec = -1;
    
    public RowCount() 
    {
      cols.add(new Col(0));
    }
    
    public int space(int i, int count)
    {
      lastEdge = null;
      if(jump == i)
      {
        dec = count;
        return 1;
      }
      return 0;
    }

    public int update(List<Edge> edges, Edge edge)
    {
      Col col;
      boolean lastLine = lastEdge != null && lastEdge.type == EdgeType.line;
      int count = 1;
      if(edge.type == EdgeType.backslash && lastLine)
      {
        unique++;
        cols.add(edge.col, col = new Col(unique));
      }
      else if(edge.type == EdgeType.slash && lastLine)
      {
        dec = edge.col;
        col = cols.get(edge.col);
      }
      else if(edge.type == EdgeType.line && lastEdge != null && lastEdge.type == EdgeType.backslash)
      {
        count = 0;
        edge.dec();
        cols.remove(edge.col);
        col = cols.get(edge.col);
      }
      else if(edge.type == EdgeType.dash && (lastEdge == null || lastEdge.type != EdgeType.dash))
      {
        lastEdge = edge;
        return 0;
      }
      else if(edge.col >= cols.size() )
      {
        unique++;
        cols.add(col = new Col(unique));
      }
      else
      {
        col = cols.get(edge.col);
      }
      edge.column = col;
      if(edge.type == EdgeType.dash)
      {
        lastEdge = null;
      } 
      else 
      {
        lastEdge = edge;
      }
      edges.add(edge);
      return count;
    }
    
    public void endRow()
    {
      lastEdge = null;
      if(dec > -1)
      {
        cols.remove(dec);
        dec = -1;
      }
      jump = -1;
    }
  }

  public static class Edge
  {
    public static enum EdgeType
    {
      line, dot, working, plus, dash, slash, backslash
    }
    
    private int top, bottom, col;
    private Col column;
    private EdgeType type;

    public Edge(char c, int i)
    {
      col = top = bottom = i;      
      type = EdgeType.line;
      switch(c)
      {
      case '/':
        type = EdgeType.slash;
        bottom--;
        break;
      case '\\':
        type = EdgeType.backslash;
        top--;
        break;
      case 'o':
        type = EdgeType.dot;
        break;
      case '@':
        type = EdgeType.working;
        break;
      case '+':
        type = EdgeType.plus;
        break;
      case '-':
        type = EdgeType.dash;
        break;
      }
    }
    
    public void dec()
    {
      top = bottom = col = col - 1;
    }

    public int getTop()
    {
      return top;
    }
    
    public int getBottom()
    {
      return bottom;
    }
    
    public int getCol()
    {
      return column.id;
    }
    
    public EdgeType getType()
    {
      return type;
    }
  }
}