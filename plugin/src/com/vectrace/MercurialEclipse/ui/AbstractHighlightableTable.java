/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 *
 */
public abstract class AbstractHighlightableTable<E> extends Composite {

	private static final Font APPLIED_FONT = JFaceResources.getFontRegistry().getBold(
			JFaceResources.DIALOG_FONT);

	private final TableViewer viewer;

	public AbstractHighlightableTable(Composite parent, final HighlightingLabelProvider<E> labelProvider) {
		super(parent, SWT.NONE);

		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		setLayout(tableColumnLayout);

		viewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.H_SCROLL);

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(labelProvider);

		List<TableViewerColumn> cols = createColumns(viewer, tableColumnLayout);

		CellLabelProvider clp = new CellLabelProvider() {
			@SuppressWarnings("unchecked")
			@Override
			public void update(ViewerCell cell) {
				E patch = (E) cell.getElement();
				HighlightingLabelProvider<E> lp = (HighlightingLabelProvider<E>) viewer
						.getLabelProvider();
				cell.setText(labelProvider.getColumnText(patch, cell.getColumnIndex()));
				cell.setImage(labelProvider.getColumnImage(patch, cell.getColumnIndex()));
				if (lp.isHighlighted(patch)) {
					cell.setFont(APPLIED_FONT);
				} else {
					cell.setFont(null);
				}
			}
		};

		for (Iterator<TableViewerColumn> it = cols.iterator(); it.hasNext();) {
			it.next().setLabelProvider(clp);
		}

		Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
	}

	protected abstract List<TableViewerColumn> createColumns(TableViewer tableViewer, TableColumnLayout tableColumnLayout);

	/**
	 * @return The first selected patch, or {@code null} if the selection is empty.
	 */
	@SuppressWarnings("unchecked")
	public E getSelection() {
		return (E) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
	}

	/**
	 * @return A list of the selected patches. If the selection is empty an empty list is returned,
	 *         never {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public List<E> getSelections() {
		return ((IStructuredSelection) viewer.getSelection()).toList();
	}

	public void setItems(List<E> patches) {
		viewer.setInput(patches);
	}

	@SuppressWarnings("unchecked")
	public List<E> getItems() {
		Object inp = viewer.getInput();

		if (inp instanceof List) {
			return (List<E>) inp;
		}

		return Collections.emptyList();
	}

	public TableViewer getTableViewer() {
		return viewer;
	}

	public abstract static class HighlightingLabelProvider<F> extends LabelProvider implements ITableLabelProvider {
		public abstract boolean isHighlighted(F inst);
	}
}
