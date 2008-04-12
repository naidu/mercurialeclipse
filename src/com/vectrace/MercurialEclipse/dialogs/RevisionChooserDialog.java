/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jérôme Nègre              - implementation
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.FileDataLoader;
import com.vectrace.MercurialEclipse.storage.ProjectDataLoader;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class RevisionChooserDialog extends Dialog {

    private final static Font PARENT_FONT = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);
    
    private final DataLoader dataLoader;
	private final String title;
	private Text text;
	private String revision;
	
	private final int[] parents;

	private ChangeSet changeSet;

	public RevisionChooserDialog(Shell parentShell, String title,
	        IFile file) {
        this(parentShell, title, new FileDataLoader(file));
	}

    public RevisionChooserDialog(Shell parentShell, String title,
            IProject project) {
        this(parentShell, title, new ProjectDataLoader(project));
    }

    private RevisionChooserDialog(Shell parentShell, String title,
            DataLoader loader) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        this.title = title;
        this.dataLoader = loader;
        int[] p = {};
        try {
            p = loader.getParents();
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
        this.parents = p;
    }

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout(1, true);
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Please enter a valid revision (local, global or tag):");

		text = new Text(composite, SWT.BORDER | SWT.DROP_DOWN);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL
                | GridData.FILL_VERTICAL);
		data.heightHint = 200;
		tabFolder.setLayoutData(data);
		createRevisionTabItem(tabFolder);
		createTagTabItem(tabFolder);
		createHeadTabItem(tabFolder);

		return composite;
	}

	@Override
	protected void okPressed() {
		revision = text.getText().split(":")[0].trim();
		changeSet = this.dataLoader.getChangeSetByRevision(Integer.parseInt(revision));
		if (revision.length() == 0) {
			revision = null;
		}
		super.okPressed();
	}

	public String getRevision() {
		return revision;
	}

	protected TabItem createRevisionTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("Revisions");

		final Table table = new Table(folder, SWT.SINGLE | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(data);
		
		table.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem tableItem = ((TableItem)e.item);
				text.setText(tableItem.getText(0));
			}
		});
		
		String[] titles = { "Rev", "Global", "Date", "Author" };
		int[] widths = { 50, 150, 150, 100 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}

        new SafeUiJob("Fetching revisions from repository") {
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    ChangeSet[] revisions = dataLoader.getRevisions();
                    for (ChangeSet rev : revisions) {
                        TableItem row = new TableItem(table, SWT.NONE);
                        if(isParent(rev.getChangesetIndex())) {
                            row.setFont(PARENT_FONT);
                        }
                        row.setText(0, Integer.toString(rev.getChangesetIndex()));
                        row.setText(1, rev.getChangeset());
                        row.setText(2, rev.getDate());
                        row.setText(3, rev.getUser());
                    }
                    return Status.OK_STATUS;
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    return Status.CANCEL_STATUS;
                }
            }
        }.schedule();

		item.setControl(table);
		return item;
	}

	protected TabItem createTagTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText("Tags");

		final Table table = new Table(folder, SWT.SINGLE | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(data);
		
		table.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				text.setText(((TableItem)e.item).getText(2));
			}
		});
		
		String[] titles = {"Rev", "Global", "Tag", "Local"};
		int[] widths = {50, 150, 300, 70 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}
		
		table.addListener(SWT.Show, new Listener() {
		    public void handleEvent(Event event) {
		        table.removeListener(SWT.Show, this);
		        new SafeUiJob("Fetching tags from repository") {
		            @Override
		            protected IStatus runSafe(IProgressMonitor monitor) {
		                try {
                            Tag[] tags = dataLoader.getTags();
                            for (Tag tag : tags) {
                                TableItem row = new TableItem(table, SWT.NONE);
                                if(isParent(tag.getRevision())) {
                                    row.setFont(PARENT_FONT);
                                }
                               row.setText(0, Integer.toString(tag.getRevision()));
                                row.setText(1, tag.getGlobalId());
                                row.setText(2, tag.getName());
                                row.setText(3, tag.isLocal()?"local":"");
                            }
                            return Status.OK_STATUS;
                        } catch (HgException e) {
                            MercurialEclipsePlugin.logError(e);
                            return Status.CANCEL_STATUS;
                        }
		            }
		        }.schedule();
		    }
		});

		item.setControl(table);
		return item;
	}

    protected TabItem createHeadTabItem(TabFolder folder) {
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Heads");

        final Table table = new Table(folder, SWT.SINGLE | SWT.BORDER
                | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        table.setLayoutData(data);
        
        table.addSelectionListener(new SelectionAdapter(){
            @Override
            public void widgetSelected(SelectionEvent e) {
                text.setText(((TableItem)e.item).getText(0));
            }
        });
        
        String[] titles = { "Rev", "Global", "Date", "Author" };
        int[] widths = { 50, 150, 150, 100 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }

        table.addListener(SWT.Show, new Listener() {
            public void handleEvent(Event event) {
                table.removeListener(SWT.Show, this);
                new SafeUiJob("Fetching heads from repository") {
                    @Override
                    protected IStatus runSafe(IProgressMonitor monitor) {
                        try {
                            ChangeSet[] revisions = dataLoader.getHeads();
                            for (ChangeSet rev : revisions) {
                                TableItem row = new TableItem(table, SWT.NONE);
                                if(isParent(rev.getChangesetIndex())) {
                                    row.setFont(PARENT_FONT);
                                }
                                row.setText(0, Integer.toString(rev.getChangesetIndex()));
                                row.setText(1, rev.getChangeset());
                                row.setText(2, rev.getDate());
                                row.setText(3, rev.getUser());
                            }
                            return Status.OK_STATUS;
                        } catch (HgException e) {
                            MercurialEclipsePlugin.logError(e);
                            return Status.CANCEL_STATUS;
                        }
                    }
                }.schedule();
            }
        });

        item.setControl(table);
        return item;
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

	public ChangeSet getChangeSet() {
		return changeSet;
	}
    
}
