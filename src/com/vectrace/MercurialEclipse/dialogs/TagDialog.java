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

package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgTagClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.TagTable;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class TagDialog extends Dialog {

    boolean revisionsLoaded;
    private final IProject project;

    // main TabItem
    Text nameText;
    Button forceButton;
    Button localButton;

    // output
    String name;
    String targetRevision;
    boolean forced;
    boolean local;

    public TagDialog(Shell parentShell, IProject project) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        this.project = project;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Tag as Version...");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        GridLayout gridLayout = new GridLayout(2, false);
        composite.setLayout(gridLayout);

        TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
                | GridData.FILL_VERTICAL));

        createMainTabItem(tabFolder);
        // TODO createOptionsTabItem(tabFolder);
        createTargetTabItem(tabFolder);

        return composite;
    }

    private GridData createGridData(int colspan) {
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = colspan;
        data.minimumWidth = SWT.DEFAULT;
        return data;
    }

    private GridData createGridData(int colspan, int width) {
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = colspan;
        data.minimumWidth = width;
        return data;
    }

    protected TabItem createMainTabItem(TabFolder folder) {
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Main");

        Composite composite = new Composite(folder, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));

        // tag name
        Label label = new Label(composite, SWT.NONE);
        label.setText("Please enter a tag name:");
        label.setLayoutData(createGridData(1));

        nameText = new Text(composite, SWT.BORDER);
        nameText.setLayoutData(createGridData(1));

        forceButton = new Button(composite, SWT.CHECK);
        forceButton.setText("Move tag if it already exists");
        forceButton.setLayoutData(createGridData(1));

        localButton = new Button(composite, SWT.CHECK);
        localButton.setText("Create local tag");
        localButton.setLayoutData(createGridData(1));

        // List of existing tags
        label = new Label(composite, SWT.NONE);
        label.setText("Existing tags:");
        label.setLayoutData(createGridData(1));

        final TagTable table = new TagTable(composite);
        table.hideTip();
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = 150;
        table.setLayoutData(data);

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Tag tag = table.getSelection();
                nameText.setText(tag.getName());
                localButton.setSelection(tag.isLocal());
            }
        });

        try {
            table.setTags(HgTagClient.getTags(project));
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }

        item.setControl(composite);
        return item;
    }

    protected TabItem createOptionsTabItem(TabFolder folder) {
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Options");

        Composite composite = new Composite(folder, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));

        // commit date
        // TODO

        // user name
        final Button customUserButton = new Button(composite, SWT.CHECK);
        customUserButton.setText("Use custom user name");

        final Text userText = new Text(composite, SWT.BORDER);
        userText.setLayoutData(createGridData(1, 250));
        userText.setEnabled(false);

        customUserButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                userText.setEnabled(customUserButton.getSelection());
            }
        });

        // commit message

        item.setControl(composite);
        return item;
    }

    protected TabItem createTargetTabItem(TabFolder folder) {
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Target");

        Composite composite = new Composite(folder, SWT.NONE);
        composite.setLayout(new GridLayout(1, true));

        Button parentButton = new Button(composite, SWT.RADIO);
        parentButton.setText("Tag parent changeset");
        parentButton.setSelection(true);

        Button otherButton = new Button(composite, SWT.RADIO);
        otherButton.setText("Tag another changeset");

        final ChangesetTable table = new ChangesetTable(composite, project);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setEnabled(false);
        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                targetRevision = Integer.toString(table.getSelection()
                        .getChangesetIndex());
            }
        });

        parentButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                table.setEnabled(false);
                targetRevision = null;
            }
        });

        otherButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                    table.setEnabled(true);
                    ChangeSet changeset = table.getSelection();
                    if (changeset != null) {
                    targetRevision = Integer.toString(changeset
                            .getChangesetIndex());
                }                
            }
        });

        item.setControl(composite);
        return item;
    }

    @Override
    protected void okPressed() {
        name = nameText.getText();
        forced = forceButton.getSelection();
        local = localButton.getSelection();
        super.okPressed();
    }

    public String getName() {
        return name;
    }

    public String getTargetRevision() {
        return targetRevision;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean isLocal() {
        return local;
    }
}
