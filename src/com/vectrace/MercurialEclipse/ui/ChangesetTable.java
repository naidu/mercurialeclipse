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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class ChangesetTable extends Composite {

    private final static Font PARENT_FONT = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);

    private Table table;
    private int[] parents; //TODO init

    public ChangesetTable(Composite parent, int style) {
        super(parent, SWT.NONE);

        table = new Table(this, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
                | SWT.H_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        // TODO GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        // TODO table.setLayoutData(data);

        String[] titles = { "Rev", "Global", "Date", "Author" };
        int[] widths = { 50, 150, 150, 100 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }
    }

    public void setChangesets(ChangeSet[] sets) {
        for (ChangeSet rev : sets) {
            TableItem row = new TableItem(table, SWT.NONE);
            if(isParent(rev.getChangesetIndex())) {
                row.setFont(PARENT_FONT);
            }
            row.setText(0, Integer.toString(rev.getChangesetIndex()));
            row.setText(1, rev.getChangeset());
            row.setText(2, rev.getDate());
            row.setText(3, rev.getUser());
            row.setData(rev);
        }
    }
    
    public ChangeSet getSelection() {
        TableItem[] selection = table.getSelection();
        if(selection.length == 0) {
            return null;
        }
        return (ChangeSet)selection[0].getData();
    }
    
    public void addSelectionListener(SelectionListener listener) {
        table.addSelectionListener(listener);
    }

    private boolean isParent(int r) {
        switch(parents.length) {
            case 2:
                if(r == parents[1]) {
                    return true;
                }
            case 1:
                if(r == parents[0]) {
                    return true;
                }
            default:
                return false;
        }
    }
    
}
