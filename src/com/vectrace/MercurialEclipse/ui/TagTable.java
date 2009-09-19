/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import org.eclipse.core.resources.IProject;
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

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class TagTable extends Composite {

    private final static Font PARENT_FONT = JFaceResources.getFontRegistry().getItalic(
            JFaceResources.DIALOG_FONT);

    private final Table table;
    private int[] parents;
    private boolean showTip = true;

    private final IProject project;

    public TagTable(Composite parent, IProject project) {
        super(parent, SWT.NONE);
        this.project = project;

        this.setLayout(new GridLayout());
        this.setLayoutData(new GridData());

        table = new Table(this, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
                | SWT.H_SCROLL);
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
        LocalChangesetCache cache = LocalChangesetCache.getInstance();
        for (Tag tag : tags) {
            if (showTip || !"tip".equals(tag.getName())) { //$NON-NLS-1$
                TableItem row = new TableItem(table, SWT.NONE);
                if (parents != null && isParent(tag.getRevision())) {
                    row.setFont(PARENT_FONT);
                }
                row.setText(0, Integer.toString(tag.getRevision()));
                row.setText(1, tag.getGlobalId());
                row.setText(2, tag.getName());
                row.setText(3, tag.isLocal() ? Messages.getString("TagTable.stateLocal")  //$NON-NLS-1$
                        : Messages.getString("TagTable.stateGlobal")); //$NON-NLS-1$
                ChangeSet changeSet = cache.getChangesetById(project, tag.getRevision() + ":" + tag.getGlobalId());
                if(changeSet != null){
                    row.setText(4, changeSet.getSummary());
                }
                row.setData(tag);
            }
        }
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
