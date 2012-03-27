/*******************************************************************************
 * Copyright (c) 2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Philip Graf               - bug fix
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.model.Tag;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class TagTable extends Composite {
	private static final Font PARENT_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

	private final Table table;
	private int[] parents;
	private boolean showTip;

	private Tag[] data;

	public TagTable(Composite parent) {
		super(parent, SWT.NONE);
		showTip = true;

		this.setLayout(new GridLayout());
		this.setLayoutData(new GridData());

		table = new Table(this, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.VIRTUAL);
		table.setItemCount(0);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		String[] titles = {
				Messages.getString("TagTable.column.rev"), Messages.getString("TagTable.column.global"), Messages.getString("TagTable.column.tag"), Messages.getString("TagTable.column.local"), Messages.getString("ChangesetTable.column.summary") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		int[] widths = { 60, 150, 200, 70, 300 };
		@SuppressWarnings("rawtypes")
		Comparator[] comparators = { new Comparator<Tag>() {
			public int compare(Tag a, Tag b) {
				return TableSortListener.sort(a.getChangeset().getRevision(), b.getChangeset().getRevision());
			}
		}, new Comparator<Tag>() {
			public int compare(Tag a, Tag b) {
				return a.getChangeset().getNode().compareTo(b.getChangeset().getNode());
			}
		}, new Comparator<Tag>() {
			public int compare(Tag a, Tag b) {
				return a.getName().compareTo(b.getName());
			}
		}, new Comparator<Tag>() {
			public int compare(Tag a, Tag b) {
				return TableSortListener.sort(a.isLocal() ? 0 : 1, b.isLocal() ? 0 : 1);
			}
		}, new Comparator<Tag>() {
			public int compare(Tag a, Tag b) {
				return a.getChangeset().getMessage().compareTo(b.getChangeset().getMessage());
			}
		} };

		Listener sortListener = new TableSortListener(table, comparators) {
			@Override
			protected Object[] getData() {
				return data;
			}
		};

		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
			column.addListener(SWT.Selection, sortListener);
		}

		table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				TableItem item = (TableItem) event.item;
				int index = table.indexOf(item);

				if (data != null && 0 <= index && index < data.length) {
					Tag tag = data[index];

					if (isParent(tag.getChangeset().getRevision())) {
						item.setFont(PARENT_FONT);
					}
					item.setText(0, Integer.toString(tag.getChangeset().getRevision()));
					item.setText(1, tag.getChangeset().getNode());
					item.setText(2, tag.getName());
					item.setText(3, tag.isLocal() ? Messages.getString("TagTable.stateLocal") //$NON-NLS-1$
							: Messages.getString("TagTable.stateGlobal"));
					item.setText(4, tag.getChangeset().getMessage());
					item.setData(tag);
				}
			}
		});
	}

	public void hideTip() {
		this.showTip = false;
	}

	public void highlightParents(int[] newParents) {
		this.parents = newParents;
	}

	public void setTags(Tag[] tags) {
		List<Tag> filtered = new ArrayList<Tag>(tags.length);

		for (Tag tag : tags) {
			if (showTag(tag)) {
				filtered.add(tag);
			}
		}

		data = filtered.toArray(new Tag[filtered.size()]);
		table.clearAll();
		table.setItemCount(data.length);
	}

	private boolean showTag(Tag tag) {
		return showTip || !"tip".equals(tag.getName());
	}

	public Tag getSelection() {
		TableItem[] selection = table.getSelection();
		if (selection.length == 0) {
			return null;
		}
		return (Tag) selection[0].getData();
	}

	public void addSelectionListener(SelectionListener listener) {
		table.addSelectionListener(listener);
	}

	private boolean isParent(int r) {
		if (parents == null) {
			return false;
		}
		switch (parents.length) {
		case 2:
			if (r == parents[1]) {
				return true;
			}
			//$FALL-THROUGH$
		case 1:
			if (r == parents[0]) {
				return true;
			}
			//$FALL-THROUGH$
		default:
			return false;
		}
	}
}
