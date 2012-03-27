/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.commands.HgBisectClient.Status;
import com.vectrace.MercurialEclipse.history.GraphLayout.GraphRow;
import com.vectrace.MercurialEclipse.model.Signature;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

public class GraphLogTableViewer extends TableViewer {

	/**
	 * Number of colors. See {@link #colors}.
	 */
	public static final int NUM_COLORS = 11;

	private static final int COL_WIDTH_PIXELS = 12;

	private static int DOT_RADIUS_PIXELS = 3;

	/**
	 * @see #NUM_COLORS
	 */
	private final List<Color> colors = new ArrayList<Color>();
	private final MercurialHistoryPage mhp;
	private final Color mergeBack;
	private final Color mergeFore;

	public GraphLogTableViewer(Composite parent, int style,
			MercurialHistoryPage mercurialHistoryPage) {
		super(parent, style);
		this.mhp = mercurialHistoryPage;
		getTable().addListener(SWT.PaintItem, new Listener() {
			public void handleEvent(final Event event) {
				paint(event);
			}
		});

		Display display = parent.getDisplay();
		colors.add(display.getSystemColor(SWT.COLOR_DARK_RED));
		colors.add(display.getSystemColor(SWT.COLOR_GREEN));
		colors.add(display.getSystemColor(SWT.COLOR_BLUE));
		colors.add(display.getSystemColor(SWT.COLOR_RED));
		colors.add(display.getSystemColor(SWT.COLOR_MAGENTA));
		colors.add(display.getSystemColor(SWT.COLOR_GRAY));
		colors.add(display.getSystemColor(SWT.COLOR_DARK_YELLOW));
		colors.add(display.getSystemColor(SWT.COLOR_DARK_MAGENTA));
		colors.add(display.getSystemColor(SWT.COLOR_DARK_CYAN));
		colors.add(display.getSystemColor(SWT.COLOR_DARK_GRAY));
		colors.add(display.getSystemColor(SWT.COLOR_DARK_GREEN));

		// TODO add pref store listener
		mergeBack = MercurialUtilities
				.getColorPreference(MercurialPreferenceConstants.PREF_HISTORY_MERGE_CHANGESET_BACKGROUND);
		mergeFore = MercurialUtilities
				.getColorPreference(MercurialPreferenceConstants.PREF_HISTORY_MERGE_CHANGESET_FOREGROUND);
	}

	protected void paint(Event event) {
		TableItem tableItem = (TableItem) event.item;
		if (event.index != 0) {
			return;
		}

		MercurialRevision rev = (MercurialRevision) tableItem.getData();
		MercurialHistory data = mhp.getMercurialHistory();

		paintRow(event, data.getGraphRow(rev));

		final Table table = tableItem.getParent();
		int from = rev.getRevision() - 1;
		int lastReqVersion = mhp.getMercurialHistory().getLastRequestedVersion();
		if (from != lastReqVersion && from >= 0 && mhp.getMercurialHistory().getLastVersion() > 0) {
			if (tableItem.equals(table.getItems()[table.getItemCount() - 1])) {
				MercurialHistoryPage.RefreshMercurialHistoryJob refreshJob = mhp.new RefreshMercurialHistoryJob(
						from);
				refreshJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event1) {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								if (table.isDisposed()) {
									return;
								}
								table.redraw();
								table.update();
							}
						});
					}
				});
				mhp.scheduleInPage(refreshJob);
			}
		}

		// validate signed changesets
		Signature sig = rev.getSignature();
		if (sig != null) {
			if (sig.validate()) {
				tableItem.setBackground(colors.get(0));
			} else {
				tableItem.setBackground(colors.get(2));
			}
		}

		if (mhp.getCurrentWorkdirChangeset() != null) {
			if (rev.getRevision() == mhp.getCurrentWorkdirChangeset().getIndex()) {
				tableItem.setFont(JFaceResources.getFontRegistry().getBold(
						JFaceResources.DEFAULT_FONT));
			}
		}

		// bisect colorization
		Status bisectStatus = rev.getBisectStatus();
		if (bisectStatus != null) {
			if (bisectStatus == Status.BAD) {
				tableItem.setBackground(colors.get(10));
			} else {
				tableItem.setBackground(colors.get(9));
			}
		} else {
			if (rev.getChangeSet().isMerge()) {
				// Don't set font here -> UI freezes on windows
				tableItem.setBackground(mergeBack);
				tableItem.setForeground(mergeFore);

			}
		}
	}

	private void paintRow(Event event, GraphRow curRow) {
		if (curRow == null) {
			return;
		}

		GraphRow prevRow = curRow.getPrevious();

		if (prevRow != null) {
			for (int i = 0, n = prevRow.numColumns(); i < n; i++) {
				int numParents = prevRow.numParents(i);

				for (int p = 0; p < numParents; p++) {
					int color = p == 0 ? prevRow.getColor(i) : prevRow.getParentColor(i, p);
					paintTop(event, i, prevRow.getParentIndex(i, p), getColor(color));
				}
			}
		}

		for (int i = 0, n = curRow.numColumns(); i < n; i++) {
			int numParents = curRow.numParents(i);

			for (int p = 0; p < numParents; p++) {
				int color = p == 0 ? curRow.getColor(i) : curRow.getParentColor(i, p);
				paintBottom(event, i, curRow.getParentIndex(i, p), getColor(color));
			}
		}

		paintDot(event, curRow.getDot());
	}

	private Color getColor(int color) {
		return colors.get(color % colors.size());
	}

	/**
	 * Paint the top part of the cell, between the current change set and it's children
	 */
	private static void paintTop(Event event, int fromCol, int toCol, Color color) {
		GC g = event.gc;
		g.setLineAttributes(new LineAttributes(2));
		g.setLineStyle(SWT.LINE_SOLID);
		g.setForeground(color);

		int endY = event.y + event.height / 2;
		int toX = getX(event, toCol);

		g.drawLine(toX, event.y, toX, endY);
	}

	/**
	 * Paint the bottom part of the cell, between the current change set and it's parents
	 */
	private static void paintBottom(Event event, int fromCol, int toCol, Color color) {
		GC g = event.gc;
		g.setLineAttributes(new LineAttributes(2));
		g.setLineStyle(SWT.LINE_SOLID);
		g.setForeground(color);

		int fromX = getX(event, fromCol);
		int toX = getX(event, toCol);
		int endY = event.y + event.height;

		if (fromCol == toCol) {
			g.drawLine(fromX, event.y + event.height / 2, fromX, endY);
		} else {
			int horStartX = getX(event, toCol + (fromCol > toCol ? 1 : -1));
			// horizontal line
			if (Math.abs(toCol - fromCol) > 1) {
				g.drawLine(fromX, event.y + event.height / 2, horStartX, event.y + event.height / 2);
			}

			// diagonal line
			g.drawLine(horStartX, event.y + event.height / 2, toX, event.y  + event.height);
		}
	}

	private static void paintDot(Event event, int dot) {
		event.gc.setBackground(event.display.getSystemColor(SWT.COLOR_BLACK));

		event.gc.fillOval(getX(event, dot) - DOT_RADIUS_PIXELS, // x
				event.y + (event.height / 2) - DOT_RADIUS_PIXELS, // y
				DOT_RADIUS_PIXELS * 2, // width
				DOT_RADIUS_PIXELS * 2); // height
	}

	/**
	 * @return The x coordinate of the centre of the column
	 */
	private static int getX(Event event, int col) {
		return event.x + (COL_WIDTH_PIXELS * col) + 5;
	}
}