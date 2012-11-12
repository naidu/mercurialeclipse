/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Watson - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class ActionShowHistory extends ActionDelegate {

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * @throws HgException
	 *
	 * Future: Support file history
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@Override
	public void run(IAction action) {
		try {
			Object historySource = null;

			if (selection != null) {
				// the loop here is for the case when we can't define any decent source from the first selected element
				for (Object o : selection.toList()) {
					historySource = getPreferredSource(o);

					if (historySource != null) {
						break;
					}
				}
			} else if (projectSelection != null) {
				historySource = getPreferredSource(projectSelection);
			}

			if (historySource != null) {
				final Object input = historySource;

				Runnable r = new Runnable() {
					public void run() {
						if (input instanceof HgRoot) {
							TeamUI.getHistoryView().showHistoryFor(input);
						} else {
							try {
								IHistoryView view =
										(IHistoryView) MercurialEclipsePlugin.getActivePage().getActivePart().getSite().getPage()
										.showView(IHistoryView.VIEW_ID);
								if (view != null) {
									view.showHistoryFor(input);
								}
							} catch (PartInitException e) {
								MercurialEclipsePlugin.logError(e);
							}
						}
					}
				};

				Display.getDefault().asyncExec(r);
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	private static Object getPreferredSource(Object o) {
		Object historySource = getFile(o);

		if (historySource == null) {
			historySource = getResource(o);
		}

		if (historySource == null) {
			historySource = getHgRoot(o);
		}

		return historySource;
	}
}
