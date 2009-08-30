/**
 *
 */
package com.vectrace.MercurialEclipse.history;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.model.GChangeSet.Edge;
import com.vectrace.MercurialEclipse.model.GChangeSet.EdgeList;

public class GraphLogTableViewer extends TableViewer {
    private final List<Color> colours = new ArrayList<Color>();
    private final MercurialHistoryPage mhp;

    public GraphLogTableViewer(Composite parent, int style,
            MercurialHistoryPage mercurialHistoryPage) {
        super(parent, style);
        this.mhp = mercurialHistoryPage;
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

    protected void paint(Event event) {
        TableItem tableItem = (TableItem) event.item;
        if (event.index != 0) {
            return;
        }
        MercurialRevision rev = (MercurialRevision) tableItem.getData();
        GChangeSet gcs = rev.getGChangeSet();
        if (gcs != null) {
            paint(event, gcs.getBefore(), 0);
            paint(event, gcs.getMiddle(), 1);
            paint(event, gcs.getAfter(), 2);
        }
        Table table = tableItem.getParent();
        int from = rev.getChangeSet().getChangesetIndex() - 1;
        if (tableItem.equals(table.getItems()[table.getItemCount() - 1])
                && from != mhp.getMercurialHistory().getBottom() && from >= 0) {
            MercurialHistoryPage.RefreshMercurialHistory refreshJob = mhp.new RefreshMercurialHistory(
                    from, mhp
                            .getMercurialHistory());
            refreshJob.schedule();
            try {
                refreshJob.join();
            } catch (InterruptedException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }

        // validate signed changesets
        Signature sig = rev.getSignature();
        if (sig != null) {
            if (sig.validate()) {
                tableItem.setBackground(colours.get(0));
            } else {
                tableItem.setBackground(colours.get(2));
            }
        }
        if (mhp.getCurrentWorkdirChangeset() != null) {
            if (rev.getRevision() == mhp.getCurrentWorkdirChangeset().getRevision().getRevision()) {
                tableItem.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
            }
        }
    }

    private void paint(Event event, EdgeList edges, int i) {
        GC g = event.gc;
        g.setLineAttributes(new LineAttributes(2));
        g.setLineStyle(SWT.LINE_SOLID);
        int div3 = event.height / 3;
        int y = event.y + div3 * i;
        int middle = event.y + (event.height / 2);
        for (Edge e : edges.getEdges()) {
            drawLine(event, g, div3, e.isFinish() ? middle : y, e, e.getTop(),
                    e.getBottom());
            if (e.isDot()) {
                fillOval(event, e);
            }
        }
        int[] jump = edges.getJump();
        if (jump != null) {
            g.setLineStyle(SWT.LINE_DOT);
            g.setForeground(g.getDevice().getSystemColor(SWT.COLOR_BLACK));
            g.drawLine(getX(event, jump[0]), middle, getX(event, jump[1]),
                    middle);
        }
    }

    private void drawLine(Event event, GC g, int div3, int y, Edge e, int top,
            int bottom) {
        g.setForeground(getColor(event, e));
        g.drawLine(getX(event, top), y, getX(event, bottom), y + div3);
    }

    private void fillOval(Event event, Edge e) {
        int size = 6;
        event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_BLACK));
        int halfSize = size / 2;
        int i = e.getTop();
        if (e.isPlus()) {
            event.gc.drawOval(getX(event, i) - halfSize, event.y
                    + (event.height / 2) - halfSize, size, size);
        } else {
            event.gc.fillOval(getX(event, i) - halfSize, event.y
                    + (event.height / 2) - halfSize, size, size);
        }
    }

    private Color getColor(Event event, Edge edge) {
        return colours.get(edge.getLane() % colours.size());
    }

    private int getX(Event event, int col) {
        return event.x + (8 * col) + 5;
    }
}