/**
 * 
 */
package com.vectrace.MercurialEclipse.history;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet.Edge;
import com.vectrace.MercurialEclipse.model.GChangeSet.Edge.EdgeType;

public class GraphLogTableViewer extends TableViewer
{
  private List<Color> colours = new ArrayList<Color>();
  
  public GraphLogTableViewer(Composite parent, int style)
  {
    super(parent, style);
    
    getTable().addListener(SWT.PaintItem, new Listener() {
      public void handleEvent(final Event event) {
        paint(event);
      }
    });
    
    Display display = parent.getDisplay();
    colours.add(display.getSystemColor(SWT.COLOR_GREEN));
    colours.add(display.getSystemColor(SWT.COLOR_BLUE));
    colours.add(display.getSystemColor(SWT.COLOR_RED));
    colours.add(display.getSystemColor(SWT.COLOR_MAGENTA));
    colours.add(display.getSystemColor(SWT.COLOR_GRAY));
    colours.add(display.getSystemColor(SWT.COLOR_DARK_YELLOW));
    colours.add(display.getSystemColor(SWT.COLOR_DARK_MAGENTA));
    colours.add(display.getSystemColor(SWT.COLOR_DARK_CYAN));
  }

  protected void paint(Event event)
  {
    TableItem tableItem = (TableItem) event.item;
    if(event.index != 0) return;
    MercurialRevision rev = (MercurialRevision)tableItem.getData();
    GChangeSet gcs = rev.getGChangeSet();
    if(gcs != null)
    {
      paint(event, gcs.getMiddle(), 0);
      paint(event, gcs.getMiddle(), 1);
      paint(event, gcs.getAfter(), 2);
    }
  }
  
  private void paint(Event event, List<Edge> edges, int i)
  {
    GC g = event.gc;
    g.setLineAttributes(new LineAttributes(2));
    int div3 = event.height/3;
    int y = event.y + div3 * i;
    for(Edge e : edges)
    {
      int top = e.getTop();
      int bottom = e.getBottom();
      if((i == 0 || i == 1) && top != bottom)
      {
    	  top = bottom = Math.max(top, bottom);
      }
      drawLine(event, g, div3, y, e, top, bottom);
      if(e.getType() == EdgeType.dot || e.getType() == EdgeType.working)
      {
    	  fillOval(event, e.getTop(), false);
      }
    }
  }

  private void drawLine(Event event, GC g, int div3, int y, Edge e, int top,
      int bottom)
  {
    g.setForeground(getColor(event, e));
    g.drawLine(getX(event, top), y, getX(event, bottom), y + div3);
  }

  private void fillOval(Event event, int i, boolean working)
  {
    int size = 6;
    int color = working ? SWT.COLOR_RED : SWT.COLOR_BLACK;
    event.gc.setBackground(event.display.getSystemColor(color));
    int halfSize = size/2;
    event.gc.fillOval(getX(event, i) - halfSize , event.y + (event.height / 2) - halfSize, size, size);
  }

  private Color getColor(Event event, Edge edge)
  {
    return colours.get(edge.getCol() % colours.size());
  }
  
  private int getX(Event event, int col)
  {
    return event.x + (8 * col) + 5;
  }
}