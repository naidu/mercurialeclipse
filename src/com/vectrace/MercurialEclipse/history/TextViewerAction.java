/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 ******************************************************************************/
package com.vectrace.MercurialEclipse.history;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.texteditor.IUpdate;

import com.vectrace.MercurialEclipse.wizards.Messages;

/**
 * Used by ConsoleView
 */
public class TextViewerAction extends Action implements IUpdate {
	private int operationCode = -1;
	private final ITextOperationTarget operationTarget;

	public TextViewerAction(TextViewer viewer, int operationCode, String labelId) {
		this.operationCode = operationCode;
		operationTarget = viewer.getTextOperationTarget();
		setText(Messages.getString(labelId));
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				TextViewerAction.this.update();
			}
		});

		update();
	}

	/**
	 * Will enable this action if some text is select, disable it if not.
	 */
	public void update() {
		boolean wasEnabled = isEnabled();
		boolean isEnabled = operationTarget != null && operationTarget.canDoOperation(operationCode);
		setEnabled(isEnabled);
		if (wasEnabled != isEnabled) {
			firePropertyChange(ENABLED, wasEnabled ? Boolean.TRUE : Boolean.FALSE, isEnabled ? Boolean.TRUE : Boolean.FALSE);
		}
	}

	@Override
	public void run() {
		if (operationCode != -1 && operationTarget != null) {
			operationTarget.doOperation(operationCode);
		}
	}
}
