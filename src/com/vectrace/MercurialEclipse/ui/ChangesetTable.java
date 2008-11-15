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
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class ChangesetTable extends Composite {

    private final static Font PARENT_FONT = JFaceResources.getFontRegistry()
            .getItalic(JFaceResources.DIALOG_FONT);

    private Table table;
    private int[] parents;
    private IResource resource;

    private ChangeSet[] changesets = new ChangeSet[0];
    private int logBatchSize;
    private boolean autoFetch = true;

    private boolean bottomNotFetched = true;

    public ChangesetTable(Composite parent, IResource resource) {
        this(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL, resource);
    }

    public ChangesetTable(Composite parent, IResource resource,
            boolean autoFetch) {
        this(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL, resource);
        this.autoFetch = autoFetch;
    }

    public ChangesetTable(Composite parent, int tableStyle, IResource resource,
            boolean autoFetch) {
        this(parent, tableStyle, resource);
        this.autoFetch = autoFetch;
    }

    /**
     * 
     */
    public ChangesetTable(Composite parent, int tableStyle, IResource resource) {
        super(parent, SWT.NONE);
        this.resource = resource;
        this.setLayout(new GridLayout());
        this.setLayoutData(new GridData());

        table = new Table(this, tableStyle);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        table.setLayoutData(data);

        String[] titles = { "Rev", "Global", "Date", "Author", "Branch",
                "Summary" };
        int[] widths = { 50, 150, 150, 100, 100, 150 };
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
                        && tableItem
                        .equals(table.getItems()[table.getItemCount() - 1])
                        && cs.getChangesetIndex() > 0) {
                    logBatchSize = Integer
                            .parseInt(MercurialUtilities
                                    .getPreference(
                                            MercurialPreferenceConstants.LOG_BATCH_SIZE,
                                            "200"));
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
        if (table.isEnabled() && autoFetch) {
            if (startRev - logBatchSize > 0 || bottomNotFetched) {
                LocalChangesetCache.getInstance().refreshAllLocalRevisions(
                        resource, true, logBatchSize, startRev, false);
            }
            SortedSet<ChangeSet> set = LocalChangesetCache.getInstance()
                    .getLocalChangeSets(resource);
            
            // only fetch rev 0:0+logbatchsize once
            if (set.size() == 0 || set.first().getChangesetIndex() == 0) {
                bottomNotFetched = false;            
            }
            
            SortedSet<ChangeSet> reverseOrderSet = new TreeSet<ChangeSet>(
                    Collections.reverseOrder());
            reverseOrderSet.addAll(set);
            setChangesets(reverseOrderSet.toArray(new ChangeSet[reverseOrderSet
                    .size()]));
        }
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
            row.setText(2, rev.getDate());
            row.setText(3, rev.getUser());
            row.setText(4, rev.getBranch());
            row.setText(5, rev.getSummary());
            row.setData(rev);
        }
        table.setItemCount(sets.length);

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
        case 1:
            if (r == parents[0]) {
                return true;
            }
        default:
            return false;
        }
    }

    /**
     * @return the changesets
     */
    public ChangeSet[] getChangesets() {
        return changesets;
    }

    /**
     * @param autoFetch
     *            the autoFetch to set
     */
    public void setAutoFetch(boolean autoFetch) {
        this.autoFetch = autoFetch;
    }

    /**
     * @return the autoFetch
     */
    public boolean isAutoFetch() {
        return autoFetch;
    }

}
