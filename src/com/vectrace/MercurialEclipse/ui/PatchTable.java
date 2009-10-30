/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - adaptation to patches
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.model.Patch;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class PatchTable extends Composite {
    private static Color APPLIED_COLOR;

    private final static Font APPLIED_FONT = JFaceResources.getFontRegistry()
            .getBold(JFaceResources.DIALOG_FONT);

    private final Table table;

    public PatchTable(Composite parent) {
        super(parent, SWT.NONE);

        if (APPLIED_COLOR == null) {
            APPLIED_COLOR = new Color(getDisplay(), new RGB(225, 255, 172));
        }

        this.setLayout(new GridLayout(1, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        table = new Table(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        // data.minimumHeight = 100;
        table.setLayoutData(data);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);

        String[] titles = { Messages.getString("PatchTable.0"), Messages.getString("PatchTable.applied"), Messages.getString("PatchTable.name"), Messages.getString("PatchTable.summary") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        int[] widths = { 20, 100, 150, 150 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }
    }

    public void setPatches(Patch[] patches) {

        table.removeAll();
        for (Patch patch : patches) {
            TableItem row = new TableItem(table, SWT.NONE);
            if (patch.isApplied()) {
                row.setFont(APPLIED_FONT);
                row.setBackground(1, APPLIED_COLOR);
            }
            row.setText(0, patch.getIndex());
            row.setText(1, patch.isApplied() ? Messages.getString("PatchTable.statusApplied") : Messages.getString("PatchTable.statusUnapplied")); //$NON-NLS-1$ //$NON-NLS-2$
            row.setText(2, patch.getName());
            row.setText(3, patch.getSummary());
            row.setData(patch);
        }
    }

    public Patch getSelection() {
        List<Patch>list = getSelections();
        if (list.size()==0) {
            return null;
        }
        return list.get(0);
    }

    public List<Patch> getSelections() {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0) {
            return null;
        }
        List<Patch> list = new ArrayList<Patch>();
        for (TableItem tableItem : selection) {
            Patch p = (Patch) tableItem.getData();
            list.add(p);
        }
        return list;
    }

    public void addSelectionListener(SelectionListener listener) {
        table.addSelectionListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        APPLIED_COLOR.dispose();
    }

}
