/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jérôme Nègre              - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse.dialogs;

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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class TagDialog extends Dialog {

	ChangeSet[] revisions;
	Tag[] tags;
	
	//main TabItem
	Text nameText;
	Button forceButton;
	Button localButton;
	
	//output
	String name;
	String targetRevision;
	boolean forced;
	boolean local;
	

	//TODO revisions and tags should be fetched on demand
	public TagDialog(Shell parentShell,ChangeSet[] revisions, Tag[] tags) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.revisions = revisions;
		this.tags = tags;
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
		//TODO createOptionsTabItem(tabFolder);
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
		
		//tag name
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
		
		//List of existing tags
		label = new Label(composite, SWT.NONE);
		label.setText("Existing tags:");
		label.setLayoutData(createGridData(1));
		
		
		Table table = new Table(composite, SWT.SINGLE | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 150;
		table.setLayoutData(data);
		
		table.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = (TableItem)e.item;
				nameText.setText(item.getText(2));
				localButton.setSelection(item.getText(3).length()!=0);
			}
		});
		
		String[] titles = {"Rev", "Global", "Tag", "Local"};
		int[] widths = {50, 150, 300, 70 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}
		
		for (Tag tag : tags) {
			if(!"tip".equals(tag.getName())) {
				TableItem row = new TableItem(table, SWT.NONE);
				row.setText(0, Integer.toString(tag.getRevision()));
				row.setText(1, tag.getGlobalId());
				row.setText(2, tag.getName());
				row.setText(3, tag.isLocal()?"local":"");
			}
		}
	
		item.setControl(composite);
		return item;
	}

	protected TabItem createOptionsTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("Options");

		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		//commit date
		//TODO
		
		//user name
		final Button customUserButton = new Button(composite, SWT.CHECK);
		customUserButton.setText("Use custom user name");
		
		final Text userText = new Text(composite, SWT.BORDER);
		userText.setLayoutData(createGridData(1, 250));
		userText.setEnabled(false);
		
		customUserButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				userText.setEnabled(customUserButton.getSelection());
			}
		});
		
		//commit message

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
		
		final Table table = new Table(composite, SWT.SINGLE | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setEnabled(false);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 200;
		table.setLayoutData(data);
		
		table.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = (TableItem)e.item;
				targetRevision = item.getText(0);
			}
		});
		
		String[] titles = { "Rev", "Global", "Date", "Author" };
		int[] widths = { 50, 150, 150, 100 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}

		for (ChangeSet rev : revisions) {
			TableItem row = new TableItem(table, SWT.NONE);
			row.setText(0, Integer.toString(rev.getChangesetIndex()));
			row.setText(1, rev.getChangeset());
			row.setText(2, rev.getDate());
			row.setText(3, rev.getUser());
		}

		parentButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				table.setEnabled(false);
				targetRevision = null;
			}
		});

		otherButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				table.setEnabled(true);
				TableItem[] selection = table.getSelection();
				if(selection.length != 0) {
					targetRevision = selection[0].getText(0);
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
