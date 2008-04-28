/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

public class HistoryPainterEdge implements Comparable<HistoryPainterEdge> {
	private HistoryPainterRevision start;
	private HistoryPainterRevision stop;
	private int startLane;
	private int stopLane;

	/**
	 * @param start
	 * @param stop
	 * @param lane
	 */
	public HistoryPainterEdge(HistoryPainterRevision start,
			HistoryPainterRevision stop, int lane) {
		this.start = start;
		this.stop = stop;
	}

	/**
	 * @return the start
	 */
	public HistoryPainterRevision getStart() {
		return start;
	}

	/**
	 * @param start
	 *            the start to set
	 */
	public void setStart(HistoryPainterRevision start) {
		this.start = start;
	}

	/**
	 * @return the stop
	 */
	public HistoryPainterRevision getStop() {
		return stop;
	}

	/**
	 * @param stop
	 *            the stop to set
	 */
	public void setStop(HistoryPainterRevision stop) {
		this.stop = stop;
	}
	

	public int compareTo(HistoryPainterEdge o) {
		// reverse order of revisions
		int result = o.getStart().getChangeSet().getChangesetIndex()
				- start.getChangeSet().getChangesetIndex();
		if (result != 0) {
			return result;
		}
		return result = o.getStop().getChangeSet().getChangesetIndex()
				- stop.getChangeSet().getChangesetIndex();

	}
	
	@Override
	public String toString() {
		return "Start:"+start+",Stop:"+stop+",StartLane:"+startLane+",StopLane:"+stopLane;
	}

	/**
	 * @return the startLane
	 */
	public int getStartLane() {
		return startLane;
	}

	/**
	 * @param startLane the startLane to set
	 */
	public void setStartLane(int startLane) {
		this.startLane = startLane;
	}

	/**
	 * @return the stopLane
	 */
	public int getStopLane() {
		return stopLane;
	}

	/**
	 * @param stopLane the stopLane to set
	 */
	public void setStopLane(int stopLane) {
		this.stopLane = stopLane;
	}
}
