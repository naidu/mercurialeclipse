package com.vectrace.MercurialEclipse.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;

public class HistoryPainter {
	private final static int[] colors = new int[] { SWT.COLOR_RED,
			SWT.COLOR_DARK_BLUE, SWT.COLOR_MAGENTA, SWT.COLOR_DARK_GREEN,
			SWT.COLOR_DARK_YELLOW, SWT.COLOR_CYAN, SWT.COLOR_DARK_MAGENTA,
			SWT.COLOR_GREEN, SWT.COLOR_DARK_RED, SWT.COLOR_BLUE,
			SWT.COLOR_DARK_CYAN };

	private HistoryPainterRevision roof;
	private SortedSet<HistoryPainterEdge> edges = new TreeSet<HistoryPainterEdge>();
	private SortedSet<HistoryPainterRevision> revisions = new TreeSet<HistoryPainterRevision>();
	private SortedSet<HistoryPainterEdge> startedEdges = new TreeSet<HistoryPainterEdge>();

	private List<HistoryPainterRevision> searchList;

	public HistoryPainter(IResource resource) {
		try {
			this.roof = new HistoryPainterRevision(resource,
					MercurialStatusCache.getInstance().getNewestLocalChangeSet(
							resource));

			// cleanup
			revisions.clear();
			edges.clear();
			startedEdges.clear();

			// gets and sorts all edges
			this.edges.addAll(addParentEdges(roof));

			// gets and sorts all revisions (313, 312, 311, etc.)
			this.revisions.add(roof);
			this.revisions.addAll(addParentRevisions(roof));

			this.searchList = new ArrayList<HistoryPainterRevision>(revisions);
			determineLayout();
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	private void determineLayout() {
		int lane = 1;
		for (HistoryPainterRevision rev : revisions) {
			lane = Math.max(1, rev.getLane());
			rev.setLane(lane);
			
			// update started edges
			for (int parCount = 0; parCount < rev.getEdgeStarts().size(); parCount++) {
				HistoryPainterEdge edge = rev.getEdgeStarts().get(parCount);

				int parentLane = lane + parCount;
				edge.setStartLane(lane);
				edge.setStopLane(parentLane);
				edge.getStop().setLane(parentLane);

				// add revision start edges to started, remove them from edges
				if (edge.getStart().equals(rev)) {
					startedEdges.add(edge);
				}
			}

			// update started edges
			for (Iterator<HistoryPainterEdge> iter = rev.getEdgeStops()
					.iterator(); iter.hasNext();) {
				HistoryPainterEdge edge = iter.next();

				// add revision start edges to started, remove them from edges
				if (edge.getStop().equals(rev)) {
					startedEdges.remove(edge);
				}
			}

			// started edges = lanes
			rev.setLanes(startedEdges.size());
		}
	}

	private SortedSet<HistoryPainterRevision> addParentRevisions(
			HistoryPainterRevision hpr) {
		SortedSet<HistoryPainterRevision> parentRevisions = new TreeSet<HistoryPainterRevision>();
		for (HistoryPainterRevision parent : hpr.getParents()) {
			parentRevisions.add(parent);
			parentRevisions.addAll(addParentRevisions(parent));
		}
		return parentRevisions;
	}

	private SortedSet<HistoryPainterEdge> addParentEdges(
			HistoryPainterRevision hpr) {
		SortedSet<HistoryPainterEdge> parentEdges = new TreeSet<HistoryPainterEdge>();
		for (HistoryPainterRevision parent : hpr.getParents()) {
			HistoryPainterEdge edge = new HistoryPainterEdge(hpr, parent, 0);
			parentEdges.add(edge);
			hpr.getEdgeStarts().add(edge);
			parent.getEdgeStops().add(edge);
			parentEdges.addAll(addParentEdges(parent));
		}
		return parentEdges;
	}

	public void paint(Event e, int columnIndex) {
		if (e.type != SWT.PaintItem || e.index != columnIndex || e.item == null
				|| e.item.getData() == null
				|| !(e.item.getData() instanceof MercurialRevision)) {
			return;
		}

		MercurialRevision mrev = (MercurialRevision) e.item.getData();

		// int height = e.height;
		int itemHeight = e.height;
		int pad = 8;

		int x = e.x + pad;
		int y = e.y;

		GC gc = e.gc;
		gc.setAntialias(SWT.ON);
		gc.setLineWidth(2);

		int branchOffsetY = 0;// itemHeight / 3;
		int startX = x;
		int startY = y;
		int endY = startY + itemHeight;

		// construct key without building graph
		HistoryPainterRevision key = new HistoryPainterRevision();
		key.setResource(mrev.getResource());
		key.setChangeSet(mrev.getChangeSet());
		int index = Collections.binarySearch(searchList, key);
		HistoryPainterRevision rev = searchList.get(index);
		System.out.println(rev.toString());

		// draw lane with revision circles
		for (int currLane = 0; currLane < rev.getLanes(); currLane++) {
			gc.setForeground(Display.getCurrent().getSystemColor(
					colors[currLane % 11]));
			startX = getStartX(pad, x, currLane);
			gc.drawLine(startX, startY, startX, endY - branchOffsetY);
			if (rev.getLane() == currLane + 1) {
				drawCircle(pad, gc, startX, startY);
			}
		}

		// // now draw arcs - they gotta be drawn after the rest.
		// for (int currLane = 0; currLane < concLanes; currLane++) {
		// gc.setForeground(Display.getCurrent().getSystemColor(
		// colors[currLane % 11]));
		// if (currLane + 1 == lane) {
		// startX = getStartX(pad, x, currLane);
		// drawBranch(pad, gc, branchOffsetY, startX, endY);
		// }
		// }
		// found the revision, now we stop.

		e.gc.dispose();

	}

	/**
	 * @param pad
	 * @param x
	 * @param currLane
	 * @return
	 */
	private int getStartX(int pad, int x, int currLane) {
		int startX;
		startX = x + currLane * pad;
		return startX;
	}

	/**
	 * @param pad
	 * @param gc
	 * @param startX
	 * @param startY
	 */
	private void drawCircle(int pad, GC gc, int startX, int startY) {
		// circle for revision
		gc.drawArc(startX - pad / 2, startY + (pad / 2), pad, pad, 0, 360);
		gc.fillArc(startX - pad / 2, startY + (pad / 2), pad, pad, 0, 360);
	}

	// /**
	// * @param pad
	// * @param gc
	// * @param arcOffsetY
	// * @param startX
	// * @param endY
	// */
	// private void drawBranch(int pad, GC gc, int arcOffsetY, int startX, int
	// endY) {
	// // arc out to outgoingEdges, if they are in different lanes
	// if (outgoingEdges != null && outgoingEdges.size() > 1) {
	// for (int edgeCount = 0; edgeCount < outgoingEdges.size(); edgeCount++) {
	// ChangeSetNode edge = outgoingEdges.get(edgeCount);
	// if (lane != edge.getLane()) {
	// int circleWidth = pad * Math.abs(lane - edge.getLane());
	//
	// // arc to the right
	// if (lane < edge.getLane()) {
	// // gc.drawArc(startX-pad, endY - arcOffsetY,
	// // circleWidth,
	// // endY, 0, 90);
	// gc.drawLine(startX, endY - arcOffsetY, startX
	// + circleWidth, endY);
	//
	// // arc to the left
	// } else if (lane > edge.getLane()) {
	// // gc.drawArc(startX-pad, endY - arcOffsetY,
	// // circleWidth, endY, 90, 180);
	// gc.drawLine(startX - circleWidth, endY, startX, endY
	// - arcOffsetY);
	// }
	// }
	// }
	// }
	// }

	/**
	 * @return the edges
	 */
	public SortedSet<HistoryPainterEdge> getEdges() {
		return edges;
	}

	/**
	 * @param edges
	 *            the edges to set
	 */
	public void setEdges(SortedSet<HistoryPainterEdge> edges) {
		this.edges = edges;
	}

	/**
	 * @return the roof
	 */
	public HistoryPainterRevision getRoof() {
		return roof;
	}

	/**
	 * @param roof
	 *            the roof to set
	 */
	public void setRoof(HistoryPainterRevision roof) {
		this.roof = roof;
	}

	public void addEdge(HistoryPainterEdge e) {
		if (edges == null) {
			edges = new TreeSet<HistoryPainterEdge>();
		}
		this.edges.add(e);
	}

	public boolean removeEdge(HistoryPainterEdge e) {
		return edges.remove(e);
	}

}
