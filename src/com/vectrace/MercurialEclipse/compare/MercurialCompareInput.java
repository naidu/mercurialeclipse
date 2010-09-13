/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Ilya Ivanov (Intland) - implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.AbstractCompareInput;
import org.eclipse.team.internal.ui.mapping.CompareInputChangeNotifier;
import org.eclipse.team.internal.ui.mapping.ResourceCompareInputChangeNotifier;
import org.eclipse.team.ui.mapping.ISynchronizationCompareInput;
import org.eclipse.team.ui.mapping.SaveableComparison;

import com.vectrace.MercurialEclipse.model.FileFromChangeSet;

@SuppressWarnings("restriction")
public class MercurialCompareInput extends AbstractCompareInput implements ISynchronizationCompareInput {

	private final ISynchronizationContext context;
	private final FileFromChangeSet fileFromChangeSet;

	public MercurialCompareInput(RevisionNode ancestor, ITypedElement left, RevisionNode right,
			ISynchronizationContext context, FileFromChangeSet fcs) {
		super(Differencer.CHANGE, ancestor, left, right);
		this.context = context;
		this.fileFromChangeSet = fcs;
	}

	@Override
	protected CompareInputChangeNotifier getChangeNotifier() {
		return ResourceCompareInputChangeNotifier.getChangeNotifier(context);
	}

	/**
	 * @see org.eclipse.team.internal.ui.mapping.AbstractCompareInput#needsUpdate()
	 */
	@Override
	public boolean needsUpdate() {
		return false;
	}

	/**
	 * @see org.eclipse.team.internal.ui.mapping.AbstractCompareInput#update()
	 */
	@Override
	public void update() {
	}

	/**
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareInput#getFullPath()
	 */
	public String getFullPath() {
		return ((RevisionNode)getRight()).getResource().getFullPath().toString();
	}

	/**
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareInput#getSaveable()
	 */
	public SaveableComparison getSaveable() {
		return null;
	}

	/**
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareInput#isCompareInputFor(java.lang.Object)
	 */
	public boolean isCompareInputFor(Object object) {
		IResource resource = ((RevisionNode)getRight()).getResource();
		IResource other = Utils.getResource(object);
		if (resource != null && other != null) {
			return resource.equals(other);
		}
		return false;
	}

	public boolean isInputFor(FileFromChangeSet fcs) {
		return this.fileFromChangeSet.equals(fcs);
	}

	/**
	 * @see org.eclipse.team.ui.mapping.ISynchronizationCompareInput#prepareInput(org.eclipse.compare.CompareConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void prepareInput(CompareConfiguration configuration, IProgressMonitor monitor) {
		configuration.setRightEditable(false);
		configuration.setLeftEditable(false);

		if (getLeft() instanceof RevisionNode) {
			configuration.setLeftLabel(((RevisionNode)getLeft()).getLabel());
		} else {
			configuration.setLeftLabel(getLeft().getName());
		}
		configuration.setRightLabel(((RevisionNode)getRight()).getLabel());
		if (getAncestor() != null) {
			configuration.setAncestorLabel(((RevisionNode)getAncestor()).getLabel());
		}
	}

	/**
	 * Fire a compare input change event.
	 * This method is public so that the change can be fired
	 * by the containing editor input on a save.
	 */
	@Override
	public void fireChange() {
		super.fireChange();
	}

}
