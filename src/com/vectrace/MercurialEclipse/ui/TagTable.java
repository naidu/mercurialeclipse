/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class TagTable extends Composite {
	private final static Font PARENT_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

	private final Table table;
	private int[] parents;
	private boolean showTip = true;

	private final IProject project;

	public TagTable(Composite parent, IProject project) {
		super(parent, SWT.NONE);
		this.project = project;

		this.setLayout(new GridLayout());
		this.setLayoutData(new GridData());

		table = new Table(this, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(data);

		String[] titles = { Messages.getString("TagTable.column.rev"), Messages.getString("TagTable.column.global"), Messages.getString("TagTable.column.tag"), Messages.getString("TagTable.column.local"), Messages.getString("ChangesetTable.column.summary") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		int[] widths = { 50, 150, 200, 70, 300};
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}
	}

	public void hideTip() {
		this.showTip = false;
	}

	public void highlightParents(int[] newParents) {
		this.parents = newParents;
	}

	public void setTags(Tag[] tags) {
		table.removeAll();

		for (Tag tag : tags) {
			if (showTip || !HgRevision.TIP.getChangeset().equals(tag.getName())) {
				TableItem row = new TableItem(table, SWT.NONE);
				if (parents != null && isParent(tag.getRevision())) {
					row.setFont(PARENT_FONT);
				}
				row.setText(0, Integer.toString(tag.getRevision()));
				row.setText(1, tag.getGlobalId());
				row.setText(2, tag.getName());
				row.setText(3, tag.isLocal() ? Messages.getString("TagTable.stateLocal")  //$NON-NLS-1$
						: Messages.getString("TagTable.stateGlobal")); //$NON-NLS-1$
				row.setData(tag);
			}
		}
		fetchChangesetInfo(tags);
	}

	void fetchChangesetInfo(final Tag[] tags) {
		final LocalChangesetCache cache = LocalChangesetCache.getInstance();
		Job fetchJob = new Job("Fetching hg changesets info") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// this can cause UI hang for big projects. Should be done in a job.
				// the only reason we need this is to show the changeset comments, so we can complete
				// this data in background
				try {
					cache.refreshAllLocalRevisions(project, false, false);
				} catch (HgException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
				final Map<Tag, ChangeSet> tagToCs = new HashMap<Tag, ChangeSet>();
				for (Tag tag : tags) {
					if(monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					if (showTip || !HgRevision.TIP.getChangeset().equals(tag.getName())) {
						ChangeSet changeSet;
						try {
							changeSet = cache.getOrFetchChangeSetById(project, tag.getRevision() + ":" + tag.getGlobalId());
							tagToCs.put(tag, changeSet);
						} catch (HgException e) {
							MercurialEclipsePlugin.logError(e);
						}
					}
				}

				Runnable updateTable = new Runnable() {
					public void run() {
						if(table.isDisposed()) {
							return;
						}
						TableItem[] items = table.getItems();
						for (TableItem item : items) {
							Object tag = item.getData();
							if (tag instanceof Tag && tagToCs.get(tag) != null) {
								item.setText(4, tagToCs.get(tag).getSummary());
							}
						}
					}
				};
				MercurialEclipsePlugin.getStandardDisplay().asyncExec(updateTable);
				return Status.OK_STATUS;
			}
		};
		fetchJob.schedule();
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
