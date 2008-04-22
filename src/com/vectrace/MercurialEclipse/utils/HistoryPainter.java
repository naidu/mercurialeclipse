package com.vectrace.MercurialEclipse.utils;


public class HistoryPainter {
//	private final static int[] colors = new int[] { SWT.COLOR_RED,
//			SWT.COLOR_DARK_BLUE, SWT.COLOR_MAGENTA, SWT.COLOR_DARK_GREEN,
//			SWT.COLOR_DARK_YELLOW, SWT.COLOR_CYAN, SWT.COLOR_DARK_MAGENTA,
//			SWT.COLOR_GREEN, SWT.COLOR_DARK_RED, SWT.COLOR_BLUE,
//			SWT.COLOR_DARK_CYAN };
//
//	public void paint(Event event) {
//
//		// some validity checks
//		if (event.type != SWT.PaintItem || event.item == null
//				|| !(event.item.getData() instanceof MercurialRevision)) {
//			return;
//		}
//
//		Rectangle bounds = event.getBounds();
//
//		int itemHeight = bounds.height;
//		int pad = 8;
//
//		int x = bounds.x + pad;
//		int y = bounds.y;
//
//		GC gc = event.gc;
//		gc.setAntialias(SWT.ON);
//		gc.setLineWidth(2);
//
//		int arcOffsetY = 0;
//		int startX = x;
//		int startY = y;
//		int endY = startY + itemHeight;
//		
//		int concurrentLanes = -1; // FIXME
//
//		// draw each lane with revision circles
//		
//		for (int currLane = 0; currLane < concurrentLanes; currLane++) {
//			gc.setForeground(Display.getCurrent().getSystemColor(
//					colors[currLane % 11]));
//
//			startX = getStartX(pad, x, currLane);
//			arcOffsetY = getArcOffsetY(currLane, itemHeight, arcOffsetY);
//
//			gc.drawLine(startX, startY, startX, endY - arcOffsetY);
//
//			// prolong line if an edge is found in the same lane or the next
//			// edge's lane is greater than current
//			for (ChangeSetNode edge : outgoingEdges) {
//				if (edge.getLane() == currLane || currLane < edge.getLane()) {
//					gc.drawLine(startX, endY - arcOffsetY, startX, endY);
//				}
//			}
//
//			drawRevisionCircles(currLane, pad, gc, startX, startY);
//		}
//
//		// now draw arcs - they gotta be drawn after the rest.
//		for (int currLane = 0; currLane < concurrentLanes; currLane++) {
//
//			gc.setForeground(Display.getCurrent().getSystemColor(
//					colors[currLane % 11]));
//			if (currLane + 1 == lane) {
//				startX = getStartX(pad, x, currLane);
//				arcOffsetY = getArcOffsetY(currLane, itemHeight, arcOffsetY);
//				drawArcs(pad, gc, arcOffsetY, startX, endY);
//			}
//		}
//		// event.gc.dispose();
//		
//	}
//
//
//
//	/**
//	 * @param itemHeight
//	 * @param arcOffsetY
//	 * @return
//	 */
//	private int getArcOffsetY(int currLane, int itemHeight, int arcOffsetY) {
//		int offset = 0;
//		if (lane == currLane + 1) {
//			if (outgoingEdges != null && outgoingEdges.size() > 1) {
//				offset = itemHeight / 3;
//			}
//		} else {
//			offset = 0;
//		}
//		return offset;
//	}
//
//	/**
//	 * @param pad
//	 * @param x
//	 * @param currLane
//	 * @return
//	 */
//	private int getStartX(int pad, int x, int currLane) {
//		int startX;
//		startX = x + currLane * pad;
//		return startX;
//	}
//
//	/**
//	 * @param pad
//	 * @param gc
//	 * @param startX
//	 * @param startY
//	 */
//	private void drawRevisionCircles(int currLane, int pad, GC gc, int startX,
//			int startY) {
//		// circle for revision
//		if (lane == currLane + 1) {
//			gc.drawArc(startX - pad / 2, startY + (pad / 2), pad, pad, 0, 360);
//			gc.fillArc(startX - pad / 2, startY + (pad / 2), pad, pad, 0, 360);
//		}
//	}
//
//	/**
//	 * @param pad
//	 * @param gc
//	 * @param arcOffsetY
//	 * @param startX
//	 * @param endY
//	 */
//	private void drawArcs(int pad, GC gc, int arcOffsetY, int startX, int endY) {
//		// arc out to outgoingEdges, if they are in different lanes
//		if (outgoingEdges != null && outgoingEdges.size() > 1) {
//			for (int edgeCount = 0; edgeCount < outgoingEdges.size(); edgeCount++) {
//				ChangeSetNode edge = outgoingEdges.get(edgeCount);
//				if (lane != edge.getLane()) {
//					int circleWidth = pad * Math.abs(lane - edge.getLane());
//
//					// arc to the right
//					if (lane < edge.getLane()) {
//						// gc.drawArc(startX-pad, endY - arcOffsetY,
//						// circleWidth,
//						// endY, 0, 90);
//						gc.drawLine(startX, endY - arcOffsetY, startX
//								+ circleWidth, endY);
//
//						// arc to the left
//					} else if (lane > edge.getLane()) {
//						// gc.drawArc(startX-pad, endY - arcOffsetY,
//						// circleWidth, endY, 90, 180);
//						gc.drawLine(startX - circleWidth, endY, startX, endY
//								- arcOffsetY);
//					}
//				}
//			}
//		}
//	}

}
