/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.Set;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;

/**
 * The group containing both "dirty" files as also not yet committed changesets
 *
 * @author Andrei
 */
public class UncommittedChangesetGroup extends ChangesetGroup {

	public UncommittedChangesetGroup() {
		super("Uncommitted", Direction.LOCAL);
	}

	/**
	 * @param sets non null
	 */
	public void setChangesets(Set<WorkingChangeSet> sets) {
		Set<ChangeSet> changesets = getChangesets();
		changesets.clear();
		changesets.addAll(sets);
	}

}
