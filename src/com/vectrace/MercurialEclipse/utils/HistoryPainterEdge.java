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
		return "Start:"+start+",Stop:"+stop;
	}
	
	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((stop == null) ? 0 : stop.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof HistoryPainterEdge))
			return false;
		final HistoryPainterEdge other = (HistoryPainterEdge) obj;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (stop == null) {
			if (other.stop != null)
				return false;
		} else if (!stop.equals(other.stop))
			return false;
		return true;
	}
}
