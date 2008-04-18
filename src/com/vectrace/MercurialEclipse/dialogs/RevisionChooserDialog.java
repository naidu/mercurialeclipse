/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch			 - small changes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.FileDataLoader;
import com.vectrace.MercurialEclipse.storage.ProjectDataLoader;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.TagTable;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class RevisionChooserDialog extends Dialog {

        
    private final DataLoader dataLoader;
	private final String title;
	private Text text;
	private String revision;
	private Tag tag;
	
	private final int[] parents;

	private ChangeSet changeSet;


    public RevisionChooserDialog(Shell parentShell, String title, IFile file) {
        this(parentShell, title, new FileDataLoader(file));
    }

    public RevisionChooserDialog(Shell parentShell, String title, IProject project) {
        this(parentShell, title, new ProjectDataLoader(project));
    }

    private RevisionChooserDialog(Shell parentShell, String title, DataLoader loader) {
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
        GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        data.heightHint = 200;
        tabFolder.setLayoutData(data);
        createRevisionTabItem(tabFolder);
        createTagTabItem(tabFolder);
        createHeadTabItem(tabFolder);

        return composite;
    }


	@Override
	protected void okPressed() {
		String[] split = text.getText().split(":");
		revision = split[0].trim();
		
		if (changeSet == null) {
			try {
				changeSet = this.dataLoader.getChangeSetByRevision(Integer
						.parseInt(revision));
			} catch (NumberFormatException e) {
				if (tag != null){
					changeSet = this.dataLoader.getChangeSetByTag(tag);
				}
			}
		}
		
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


        final ChangesetTable table = new ChangesetTable(folder);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.highlightParents(parents);

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	tag = null;
                text.setText(table.getSelection().getChangesetIndex()+":"+table.getSelection().getChangeset());
                changeSet = table.getSelection();
            }
        });

        new SafeUiJob("Fetching revisions from repository") {
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) {
                try {
                    ChangeSet[] revisions = dataLoader.getRevisions();
                    table.setChangesets(revisions);
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

        final TagTable table = new TagTable(folder);
        table.highlightParents(parents);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        table.addSelectionListener(new SelectionAdapter() {

			@Override
            public void widgetSelected(SelectionEvent e) {
                text.setText(table.getSelection().getName());
                tag = table.getSelection(); 
            }
        });

        table.addListener(SWT.Show, new Listener() {
            public void handleEvent(Event event) {
                table.removeListener(SWT.Show, this);
                new SafeUiJob("Fetching tags from repository") {
                    @Override
                    protected IStatus runSafe(IProgressMonitor monitor) {
                        try {
                            Tag[] tags = dataLoader.getTags();
                            table.setTags(tags);
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

        final ChangesetTable table = new ChangesetTable(folder);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.highlightParents(parents);

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	tag = null;
                text.setText(Integer.toString(table.getSelection().getChangesetIndex()));
            }
        });

        table.addListener(SWT.Show, new Listener() {
            public void handleEvent(Event event) {
                table.removeListener(SWT.Show, this);
                new SafeUiJob("Fetching heads from repository") {
                    @Override
                    protected IStatus runSafe(IProgressMonitor monitor) {
                        try {
                            ChangeSet[] revisions = dataLoader.getHeads();
                            table.setChangesets(revisions);
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

	public ChangeSet getChangeSet() {
		return changeSet;
	}

}
