/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse.annotations;

import java.util.Date;

import com.vectrace.MercurialEclipse.model.ChangeSet;

public class AnnotateBlock {

	private final ChangeSet changeset;
	private final int startLine;
	private int endLine;

	public AnnotateBlock(ChangeSet changeset, int startLine, int endLine) {
		this.changeset = changeset;
		this.startLine = startLine;
		this.endLine = endLine;
	}

	/**
	 * @return int the last source line of the receiver
	 */
	public int getEndLine() {
		return endLine;
	}

	public void setEndLine(int line) {
		endLine = line;
	}

	public ChangeSet getChangeSet() {
		return changeset;
	}

	/**
	 * @return the first source line number of the receiver
	 */
	public int getStartLine() {
		return startLine;
	}

	/**
	 * Answer true if the receiver contains the given line number, false otherwise.
	 *
	 * @param i
	 *            a line number
	 * @return true if receiver contains a line number.
	 */
	public boolean contains(int i) {
		return i >= startLine && i <= endLine;
	}

	/**
	 * @return Returns the date.
	 */
	public Date getDate() {
		return changeset.getDate();
	}
}
