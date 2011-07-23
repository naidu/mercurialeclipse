/*******************************************************************************
 * Copyright (c) 2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - support for multi-select tables
 *     Andrei Loskutov           - bug fixes
 *     Philip Graf               - bug fix
 *     Ilya Ivanov (Intland)     - modifications
 *     John Peberdy              - use virtual table
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class ChangesetTable extends Composite {

	/** single selection, border, scroll */
	private static final int DEFAULT_STYLE = SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
			| SWT.H_SCROLL | SWT.VIRTUAL;

	private static final Font PARENT_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

	private final Table table;
	protected Strategy strategy;
	private int[] parents;
	private int logBatchSize;

	// constructors

	public ChangesetTable(Composite parent, boolean multiSelect) {
		this(parent, (Strategy)null, multiSelect);
	}

	public ChangesetTable(Composite parent, IResource resource) {
		this(parent, new ResourceStrategy(resource), false);
	}

	public ChangesetTable(Composite parent, HgRoot hgRoot) {
		this(parent, new RootStrategy(hgRoot), false);
	}

	public ChangesetTable(Group parent, HgRoot root, boolean multiSelect) {
		this(parent, new RootStrategy(root), multiSelect);
	}

	/**
	 * @param parent non null swt parent widget
	 * @param resource a resource to show changesets for, mutually exclusive with the hgRoot argument
	 * @param hgRoot a hg root to show changesets for, , mutually exclusive with the resource argument
	 * @param autoFetch true to fetch extra changesets info on scroll as needed
	 */
	private ChangesetTable(Composite parent, Strategy strategy, boolean multiSelect) {
		super(parent, SWT.NONE);

		this.logBatchSize = LocalChangesetCache.getInstance().getLogBatchSize();
		// limit log to allow "smooth" scrolling (not too small and not too big)
		// - but only if not set in preferences
		if (logBatchSize <= 0) {
			logBatchSize = 200;
		}
		this.setLayout(new GridLayout());
		this.setLayoutData(new GridData());

		int tableStyle = DEFAULT_STYLE;

		if (multiSelect) {
			tableStyle |= SWT.MULTI;
		} else {
			tableStyle |= SWT.SINGLE;
		}

		table = new Table(this, tableStyle);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 150;
		data.minimumHeight = 50;
		table.setLayoutData(data);

		String[] titles = { Messages.getString("ChangesetTable.column.rev"),
				Messages.getString("ChangesetTable.column.global"),
				Messages.getString("ChangesetTable.column.date"),
				Messages.getString("ChangesetTable.column.author"),
				Messages.getString("ChangesetTable.column.branch"),
				"Tags",
				Messages.getString("ChangesetTable.column.summary") }; //$NON-NLS-1$
		int[] widths = { 60, 80, 100, 80, 70, 70, 300 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}

		table.addListener(SWT.PaintItem, new Listener() {
			public void paintControl(Event e) {
				// When painting the last item, attempt to load more data
				TableItem tableItem = (TableItem) e.item;
				ChangeSet cs = (ChangeSet) tableItem.getData();
				if (table.isEnabled()
						&& tableItem.equals(table.getItems()[table.getItemCount() - 1])
						&& cs.getChangesetIndex() > 0) {
					loadMore();
				}
			}

			public void handleEvent(Event event) {
				paintControl(event);
			}
		});

		table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				TableItem row = (TableItem) event.item;

				if (ChangesetTable.this.strategy != null) {
					ChangeSet rev = ChangesetTable.this.strategy.getChangeSet(table.indexOf(row));

					if (parents != null && isParent(rev.getChangesetIndex())) {
						row.setFont(PARENT_FONT);
					}

					row.setText(0, Integer.toString(rev.getChangesetIndex()));
					row.setText(1, rev.getChangeset());
					row.setText(2, rev.getDateString());
					row.setText(3, rev.getUser());
					row.setText(4, rev.getBranch());
					row.setText(5, ChangeSetUtils.getPrintableTagsString(rev));
					row.setText(6, rev.getSummary());
					row.setData(rev);
				}
			}
		});

		setStrategy(strategy);
	}

	// operations

	public void highlightParents(int[] newParents) {
		this.parents = newParents;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
		table.removeAll();
		loadMore();
	}

	protected void loadMore() {
		try {
			if (ChangesetTable.this.strategy != null) {
				int nLength = ChangesetTable.this.strategy.load(null, logBatchSize);

				if (nLength >= 0) {
					table.setItemCount(nLength);
				}
			}
		} catch (HgException e1) {
			MercurialEclipsePlugin.logError(e1);
		}
	}

	public void setSelection(ChangeSet selection) {
		try {
			if (ChangesetTable.this.strategy != null) {
				int n = ChangesetTable.this.strategy.load(selection, logBatchSize);

				if (n >= 0) {
					table.setItemCount(n);
				}

				n = ChangesetTable.this.strategy.indexOf(selection);

				if (n >= 0) {
					table.setSelection(n);
				}
			}
		} catch (HgException e1) {
			MercurialEclipsePlugin.logError(e1);
		}
	}

	public void clearSelection(){
		table.deselectAll();
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
	}

	protected boolean isParent(int r) {
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

	// inner types

	public abstract static class Strategy {
		private static final ChangeSet[] NO_CHANGESETS = new ChangeSet[0];

		protected ChangeSet[] fetched = NO_CHANGESETS;

		public final ChangeSet getChangeSet(int i) {
			if (0 <= i && i < fetched.length) {
				return fetched[i];
			}
			return null;
		}

		/**
		 * Load up to a changeset. If null then load some more.
		 */
		public abstract int load(ChangeSet cs, int batchSize) throws HgException;

		public int indexOf(ChangeSet cs) {
			if (cs != null) {
				for (int i = 0; i < fetched.length; i++) {
					if (cs.compareTo(fetched[i]) == 0) { // Why not use equals?
						return i;
					}
				}
			}

			return -1;
		}
	}

	public static class PrefetchedStrategy extends Strategy {
		public PrefetchedStrategy(ChangeSet[] changeSets) {
			Assert.isNotNull(changeSets);
			this.fetched = changeSets;
		}

		@Override
		public int load(ChangeSet cs, int batchSize) {
			return fetched.length;
		}
	}

	private abstract static class AutofetchStrategy extends Strategy {
		private boolean canLoad = true;

		@Override
		public int load(ChangeSet cs, int batchSize) throws HgException {
			if (cs != null) {
				while (indexOf(cs) < 0 && loadMore(batchSize)) {
				}
			} else {
				loadMore(batchSize);
			}
			return fetched.length;
		}

		private boolean loadMore(int batchSize) throws HgException {
			if (!canLoad) {
				return false;
			}

			// Get stuff from cache and added it to fetched
			LocalChangesetCache cache = LocalChangesetCache.getInstance();
			SortedSet<ChangeSet> set = get(cache);
			int prevLen = fetched.length;

			if (!set.isEmpty()) {
				int smallestInCache = set.first().getChangesetIndex();

				// TODO: what if the cache is flushed requested from elsewhere meaning we'd have a
				// gap in revs. Should not happen.
				if (fetched.length != 0
						&& smallestInCache >= fetched[fetched.length - 1].getChangesetIndex()) {
					set = getMore(cache, batchSize, smallestInCache);
				}

				SortedSet<ChangeSet> reverseOrderSet = new TreeSet<ChangeSet>(Collections.reverseOrder());
				reverseOrderSet.addAll(set);

				for (ChangeSet changeSet : fetched) {
					reverseOrderSet.add(changeSet);
				}

				fetched = reverseOrderSet.toArray(new ChangeSet[reverseOrderSet.size()]);
			}

			return canLoad = prevLen < fetched.length;
		}

		protected abstract SortedSet<ChangeSet> get(LocalChangesetCache cache) throws HgException;

		protected abstract SortedSet<ChangeSet> getMore(LocalChangesetCache cache, int batchSize, int startRev) throws HgException;
	}

	public static class ResourceStrategy extends AutofetchStrategy {
		private final IResource resource;

		public ResourceStrategy(IResource resource) {
			this.resource = resource;
		}

		@Override
		protected SortedSet<ChangeSet> get(LocalChangesetCache cache) throws HgException {
			return cache.getOrFetchChangeSets(resource);
		}

		@Override
		protected SortedSet<ChangeSet> getMore(LocalChangesetCache cache, int batchSize, int startRev) throws HgException {
			cache.fetchRevisions(resource, true, batchSize, startRev, false);
			return cache.getOrFetchChangeSets(resource);
		}
	}

	public static class RootStrategy extends AutofetchStrategy {
		private final HgRoot root;

		public RootStrategy(HgRoot root) {
			this.root = root;
		}

		@Override
		protected SortedSet<ChangeSet> get(LocalChangesetCache cache) throws HgException {
			return cache.getOrFetchChangeSets(root);
		}

		@Override
		protected SortedSet<ChangeSet> getMore(LocalChangesetCache cache, int batchSize, int startRev) throws HgException {
			cache.fetchRevisions(root, true, batchSize, startRev, false);
			return cache.getOrFetchChangeSets(root);
		}
	}
}
