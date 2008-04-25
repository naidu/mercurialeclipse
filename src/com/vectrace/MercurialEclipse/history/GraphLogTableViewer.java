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
    colours.add(display.getSystemColor(SWT.COLOR_DARK_CYAN));
  }

  protected void paint(Event event)
  {
    TableItem tableItem = (TableItem) event.item;
    if(event.index != 0) return;
    MercurialRevision rev = (MercurialRevision)tableItem.getData();
    GChangeSet gcs = rev.getGChangeSet();
    GC g = event.gc;
    
    int revLength = gcs.getRevLength()+Math.min(0, gcs.getColumnsDiff());
    int height = event.height;
    int halfheight = height/2;
    int middle = event.y + halfheight;
    int index = gcs.getIndex();
    int itemCount = getTable().getItemCount() - 1;
    
    boolean expand = gcs.getRevIndex() == revLength - 2 && gcs.getColumnsDiff() > 0;
    
    for(int i=0;i<revLength;i++) 
    {
      drawLine(event, g, 
          i, index == 0 ? middle : event.y,
          i, index == itemCount || (expand && i == revLength - 1 )? halfheight : height);
    }
    for(Edge e : gcs.getEdges())
    {
      drawLine(event, g, e.getX(), middle, e.getY(), height);
    }
    // TODO HEADS
    // TODO gcs.getJumps()
    if(gcs.getColumnsDiff() < 0)
    {
      drawLine(event, g, revLength, event.y, revLength, halfheight);
      drawLine(event, g, revLength, middle, revLength - 1, height);
    }
    if(gcs.getColumnsDiff() > 0)
    {
      System.err.println(gcs.getRevIndex() + " " + revLength );
      drawLine(event, g, revLength-1, middle, revLength, height);
    }
    
    fillOval(event, gcs, g, middle, gcs.isWorking());
  }

  private void fillOval(Event event, GChangeSet gcs, GC g, int middle, boolean working)
  {
    int size = 6;
    int color = working ? SWT.COLOR_RED : SWT.COLOR_BLACK;
    g.setBackground(event.display.getSystemColor(color));
    int halfSize = size/2;
    
    g.fillOval(getX(event, gcs.getRevIndex()) - halfSize , middle - halfSize, size, size);
  }

  private void drawLine(Event event, GC g, int x1, int y1, int x2, int y2)
  {    
    g.setForeground(getColor(event, Math.max(x1, x2)));
    g.setLineAttributes(new LineAttributes(2));
    g.drawLine(getX(event, x1), y1, getX(event, x2), event.y + y2);
  }

  private Color getColor(Event event, int i)
  {
    return colours.get(i % colours.size());
  }
  
  private int getX(Event event, int col)
  {
    return event.x + (8 * col) + 5;
  }
}