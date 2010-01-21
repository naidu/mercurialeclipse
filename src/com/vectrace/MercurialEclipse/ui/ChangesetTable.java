/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - support for multi-select tables
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class ChangesetTable extends Composite {

	/** single selection, border, scroll */
	private static final int DEFAULT_STYLE = SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL;

	private final static Font PARENT_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

	private final Table table;
	private int[] parents;
	private IResource resource;

	private ChangeSet[] changesets;
	private int logBatchSize;
	private boolean autoFetch;

	private boolean bottomNotFetched;

	private final HgRoot hgRoot;

	public ChangesetTable(Composite parent, IResource resource) {
		this(parent, DEFAULT_STYLE, resource, true);
	}

	public ChangesetTable(Composite parent, HgRoot hgRoot) {
		this(parent, DEFAULT_STYLE, null, hgRoot, true);
	}

	public ChangesetTable(Composite parent, int tableStyle, IResource resource, boolean autoFetch) {
		this(parent, tableStyle, resource, null, autoFetch);
	}

	/**
	 * @param parent non null swt parent widget
	 * @param tableStyle SWT style bits
	 * @param resource a resource to show changesets for, mutually exclusive with the hgRoot argument
	 * @param hgRoot a hg root to show changesets for, , mutually exclusive with the resource argument
	 * @param autoFetch true to fetch extra changesets info on scroll as needed
	 */
	protected ChangesetTable(Composite parent, int tableStyle, IResource resource, HgRoot hgRoot, boolean autoFetch) {
		super(parent, SWT.NONE);
		this.hgRoot = hgRoot;
		this.resource = resource;
		this.autoFetch = autoFetch;
		changesets = new ChangeSet[0];
		bottomNotFetched = true;
		this.logBatchSize = LocalChangesetCache.getInstance().getLogBatchSize();
		this.setLayout(new GridLayout());
		this.setLayoutData(new GridData());

		table = new Table(this, tableStyle);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 150;
		data.minimumHeight = 50;
		table.setLayoutData(data);

		String[] titles = { Messages.getString("ChangesetTable.column.rev"), Messages.getString("ChangesetTable.column.global"), Messages.getString("ChangesetTable.column.date"), Messages.getString("ChangesetTable.column.author"), Messages.getString("ChangesetTable.column.branch"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				Messages.getString("ChangesetTable.column.summary") }; //$NON-NLS-1$
		int[] widths = { 50, 150, 150, 100, 100, 300 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}
		Listener paintListener = new Listener() {

			public void paintControl(Event e) {
				TableItem tableItem = (TableItem) e.item;
				ChangeSet cs = (ChangeSet) tableItem.getData();
				if (table.isEnabled()
						&& tableItem.equals(table.getItems()[table.getItemCount() - 1])
						&& cs.getChangesetIndex() > 0) {
					// limit log to allow "smooth" scrolling (not too small and not too big)
					logBatchSize = 200;
					try {
						int startRev = cs.getChangesetIndex() - 1;
						updateTable(startRev);
					} catch (HgException e1) {
						MercurialEclipsePlugin.logError(e1);
					}
				}
			}

			public void handleEvent(Event event) {
				paintControl(event);
			}
		};
		table.addListener(SWT.PaintItem, paintListener);

	}

	public void highlightParents(int[] newParents) {
		this.parents = newParents;
	}

	/**
	 * @param startRev
	 * @throws HgException
	 */
	private void updateTable(int startRev) throws HgException {
		if (!isAutoFetchEnabled()) {
			return;
		}
		LocalChangesetCache cache = LocalChangesetCache.getInstance();
		if (startRev - logBatchSize > 0 || bottomNotFetched) {
			if(resource != null) {
				cache.fetchRevisions(resource, true, logBatchSize, startRev, false);
			} else {
				cache.fetchRevisions(hgRoot, true, logBatchSize, startRev, false);
			}
		}
		SortedSet<ChangeSet> set;
		if(resource != null) {
			set = cache.getOrFetchChangeSets(resource);
		} else {
			set = cache.getOrFetchChangeSets(hgRoot);
		}

		// only fetch rev 0:0+logbatchsize once
		if (set == null || set.size() == 0 || set.first().getChangesetIndex() == 0) {
			bottomNotFetched = false;
			if (set == null) {
				return;
			}
		}

		SortedSet<ChangeSet> reverseOrderSet = new TreeSet<ChangeSet>(Collections.reverseOrder());
		reverseOrderSet.addAll(set);
		setChangesets(reverseOrderSet.toArray(new ChangeSet[reverseOrderSet.size()]));
	}

	/**
	 * @return true if it is allowed to start fetching the data
	 */
	private boolean isAutoFetchEnabled() {
		return autoFetch && (resource != null || hgRoot != null) && table.isEnabled();
	}

	public void setChangesets(ChangeSet[] sets) {
		this.changesets = sets;
		// table.removeAll();
		for (int i = Math.max(0, table.getItemCount()); i < sets.length; i++) {
			ChangeSet rev = sets[i];
			TableItem row = new TableItem(table, SWT.NONE);
			if (parents != null && isParent(rev.getChangesetIndex())) {
				row.setFont(PARENT_FONT);
			}
			row.setText(0, Integer.toString(rev.getChangesetIndex()));
			row.setText(1, rev.getChangeset());
			row.setText(2, rev.getDateString());
			row.setText(3, rev.getUser());
			row.setText(4, rev.getBranch());
			row.setText(5, rev.getSummary());
			row.setData(rev);
		}
		table.setItemCount(sets.length);

	}

	public void clearTable() {
		table.removeAll();
		this.changesets = null;
	}

	public ChangeSet[] getSelections() {
		TableItem[] selection = table.getSelection();
		if (selection.length == 0) {
			return null;
		}

		ChangeSet[] csArray = new ChangeSet[selection.length];
		for (int i = 0; i < selection.length; i++) {
			csArray[i] = (ChangeSet) selection[i].getData();
		}
		return csArray;
	}

	public ChangeSet getSelection() {
		if (getSelections() != null) {
			return getSelections()[0];
		}
		return null;
	}

	public void addSelectionListener(SelectionListener listener) {
		table.addSelectionListener(listener);
	}

	@Override
	public void setEnabled(boolean enabled) {
		table.setEnabled(enabled);
		try {
			if (enabled) {
				updateTable(-1);
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	private boolean isParent(int r) {
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

	public ChangeSet[] getChangesets() {
		return changesets;
	}

	public void setAutoFetch(boolean autoFetch) {
		this.autoFetch = autoFetch;
	}

	public boolean isAutoFetch() {
		return autoFetch;
	}

	public IResource getResource() {
		return resource;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
	}

}
