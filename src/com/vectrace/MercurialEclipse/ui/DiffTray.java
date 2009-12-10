/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author bastian
 *
 */
public class DiffTray extends org.eclipse.jface.dialogs.DialogTray {

	private CompareEditorInput compareInput;
	private Composite comp;

	/**
	 *
	 */
	public DiffTray(CompareEditorInput compareInput) {
		this.compareInput = compareInput;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.jface.dialogs.DialogTray#createContents(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		try {
		comp = SWTWidgetHelper.createComposite(parent, 1);
		compareInput.run(new NullProgressMonitor());
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			MercurialEclipsePlugin.logError(e);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			MercurialEclipsePlugin.logError(e);
		}
		Control c = compareInput.createContents(comp);
		GridData layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.minimumWidth = 400;
		c.setLayoutData(layoutData);
		return comp;
	}

}
