/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch  -  implementation
 *******************************************************************************/
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
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

public class HistoryPainter {
	private final static int[] colors = new int[] { SWT.COLOR_RED,
			SWT.COLOR_DARK_BLUE, SWT.COLOR_MAGENTA, SWT.COLOR_DARK_GREEN,
			SWT.COLOR_DARK_YELLOW, SWT.COLOR_CYAN, SWT.COLOR_DARK_MAGENTA,
			SWT.COLOR_GREEN, SWT.COLOR_DARK_RED, SWT.COLOR_BLUE,
			SWT.COLOR_DARK_CYAN };

	private HistoryPainterRevision roof;
	private final SortedSet<HistoryPainterRevision> revisions = new TreeSet<HistoryPainterRevision>();
	private final SortedSet<HistoryPainterEdge> startedEdges = new TreeSet<HistoryPainterEdge>();

	private List<HistoryPainterRevision> searchList;

	public HistoryPainter(IResource resource) {
		try {
			LocalChangesetCache.getInstance().refreshAllLocalRevisions(
					resource.getProject(), false);

			this.roof = new HistoryPainterRevision(resource,
					LocalChangesetCache.getInstance().getNewestChangeSet(
							resource));

			// cleanup
			revisions.clear();
			startedEdges.clear();

			// gets and sorts all revisions (313, 312, 311, etc.)
			this.revisions.add(roof);
			this.revisions.addAll(loadRevisions(roof));

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

			// remove completed started edges
			for (Iterator<HistoryPainterEdge> iter = new TreeSet<HistoryPainterEdge>(
					startedEdges).iterator(); iter.hasNext();) {
				HistoryPainterEdge edge = iter.next();

				// remove completed edge from started edges
				if (edge.getStop().equals(rev)) {
					startedEdges.remove(edge);
				}
			}

			// add newly started edges
			for (int parCount = 0; parCount < rev.getParents().size(); parCount++) {
				HistoryPainterRevision parent = rev.getParents().get(parCount);

				// create edge for current parent
				HistoryPainterEdge edge = new HistoryPainterEdge(rev, parent, 0);

				// add edge to started
				startedEdges.add(edge);

				int parentLane = lane + parCount;
				if (parCount > 0){
					parentLane = startedEdges.size();
				}

				// set lane of edge target revision
				parent.setLane(parentLane);
			}

			rev.setLanes(Math.max(lane, startedEdges.size()));
		}
	}

	private SortedSet<HistoryPainterRevision> loadRevisions(
			HistoryPainterRevision hpr) {
		SortedSet<HistoryPainterRevision> parentRevisions = new TreeSet<HistoryPainterRevision>();
		for (HistoryPainterRevision parent : hpr.getParents()) {
			parentRevisions.add(parent);
			parentRevisions.addAll(loadRevisions(parent));
		}
		return parentRevisions;
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

		int branchOffsetY = itemHeight / 3;
		int startX = x;
		int startY = y;
		int endY = startY + itemHeight;

		// construct key without building graph
		HistoryPainterRevision key = new HistoryPainterRevision();
		key.setResource(mrev.getResource());
		key.setChangeSet(mrev.getChangeSet());

		int index = Collections.binarySearch(searchList, key);
		HistoryPainterRevision rev = searchList.get(index);

		// draw lane with revision circles
		for (int currLane = 0; currLane < rev.getLanes(); currLane++) {
			gc.setForeground(Display.getCurrent().getSystemColor(
					colors[currLane % 11]));
			startX = getStartX(pad, x, currLane);
			gc.drawLine(startX, startY, startX, endY - branchOffsetY);
			if (rev.getLane() == currLane + 1) {
				drawCircle(pad, gc, startX, startY);
				drawBranch(rev, pad, gc, branchOffsetY, startX, endY);
			}

			// draw rest of vertical line if there are parents
			if (rev.getParents().size() > 0) {
				gc.drawLine(startX, endY - branchOffsetY, startX, endY);
			}
		}

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

	/**
	 * @param pad
	 * @param gc
	 * @param arcOffsetY
	 * @param startX
	 * @param endY
	 */
	private void drawBranch(HistoryPainterRevision rev, int pad, GC gc,
			int arcOffsetY, int startX, int endY) {
		// arc out to outgoingEdges, if they are in different lanes
		for (HistoryPainterRevision parent : rev.getParents()) {
			int circleWidth = pad
					* Math.abs(rev.getLane() - parent.getLane());

			// arc to the right
			if (rev.getLane() < parent.getLane()) {
				// gc.drawArc(startX-pad, endY - arcOffsetY,
				// circleWidth,
				// endY, 0, 90);
				gc.drawLine(startX, endY - arcOffsetY, startX + circleWidth,
						endY);

				// arc to the left
			} else if (rev.getLane() > parent.getLane()) {
				// gc.drawArc(startX-pad, endY - arcOffsetY,
				// circleWidth, endY, 90, 180);
				gc.drawLine(startX - circleWidth, endY, startX, endY
						- arcOffsetY);
			}
		}
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
}
