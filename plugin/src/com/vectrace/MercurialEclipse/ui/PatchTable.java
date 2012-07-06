/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - adaptation to patches
 *     Andrei Loskutov - bug fixes
 *     Philip Graf               - refactoring: replaced Table with TableViewer
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.aragost.javahg.ext.mq.Patch;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class PatchTable extends AbstractHighlightableTable<Patch> {

	public PatchTable(Composite parent) {
		super(parent, new PatchTableLabelProvider());
	}

	/**
	 * @see com.vectrace.MercurialEclipse.ui.AbstractHighlightableTable#createColumns(org.eclipse.jface.viewers.TableViewer, org.eclipse.jface.layout.TableColumnLayout)
	 */
	@Override
	protected List<TableViewerColumn> createColumns(TableViewer viewer, TableColumnLayout tableColumnLayout) {
		List<TableViewerColumn> l = new ArrayList<TableViewerColumn>(4);

		String[] titles = {
				Messages.getString("PatchTable.index"), //$NON-NLS-1$
				Messages.getString("PatchTable.applied"), //$NON-NLS-1$
				Messages.getString("PatchTable.name"), //$NON-NLS-1$
				Messages.getString("PatchTable.summary") }; //$NON-NLS-1$
		ColumnLayoutData[] columnWidths = {
				new ColumnPixelData(20, false, true),
				new ColumnPixelData(75, false, true),
				new ColumnWeightData(25, 200, true),
				new ColumnWeightData(75, 200, true) };
		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.getColumn().setText(titles[i]);
			tableColumnLayout.setColumnData(column.getColumn(), columnWidths[i]);
			l.add(column);
		}

		return l;
	}

	private static class PatchTableLabelProvider extends HighlightingLabelProvider<Patch> {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			Patch patch = (Patch) element;
			switch (columnIndex) {
				case 0:
					return String.valueOf(patch.getIndex());
				case 1:
					return patch.isApplied() ? Messages.getString("PatchTable.statusApplied") : Messages.getString("PatchTable.statusUnapplied"); //$NON-NLS-1$ //$NON-NLS-2$
				case 2:
					return patch.getName();
				case 3:
					return patch.getSummary();
			}
			return null;
		}

		/**
		 * @see com.vectrace.MercurialEclipse.ui.AbstractHighlightableTable.HighlightingLabelProvider#isHighlighted(java.lang.Object)
		 */
		@Override
		public boolean isHighlighted(Patch patch) {
			return patch.isApplied();
		}
	}
}
