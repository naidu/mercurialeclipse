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
import com.vectrace.MercurialEclipse.model.Bookmark;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.FileDataLoader;
import com.vectrace.MercurialEclipse.storage.ProjectDataLoader;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.BookmarkTable;
import com.vectrace.MercurialEclipse.ui.BranchTable;
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
    private Branch branch;
    private Bookmark bookmark;
	
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
        label.setText("Please enter a valid revision (local, global, tag or branch):");

        text = new Text(composite, SWT.BORDER | SWT.DROP_DOWN);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
        GridData data = new GridData(GridData.FILL_HORIZONTAL
                | GridData.FILL_VERTICAL);
        data.heightHint = 200;
        tabFolder.setLayoutData(data);
        createRevisionTabItem(tabFolder);
        try {
            if (MercurialUtilities.isCommandAvailable("bookmarks",
                    ResourceProperties.EXT_BOOKMARKS_AVAILABLE,
                    "hgext.bookmarks=")) {
                createBookmarkTabItem(tabFolder);
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
        createTagTabItem(tabFolder);
        createBranchTabItem(tabFolder);        
        createHeadTabItem(tabFolder);

        return composite;
    }


	@Override
	protected void okPressed() {
		String[] split = text.getText().split(":");
		revision = split[0].trim();
		
		if (changeSet == null) {
			if (tag != null){
					changeSet = LocalChangesetCache.getInstance().getChangeSet(
                        tag.getRevision() + ":"+tag.getGlobalId());				
			}
			else if(branch != null) {
			    changeSet = LocalChangesetCache.getInstance().getChangeSet(
                        branch.getRevision() + ":"+branch.getGlobalId());     
			} else if (bookmark != null) {
                changeSet = LocalChangesetCache.getInstance().getChangeSet(
                        bookmark.getRevision() + ":"
                                + bookmark.getShortNodeId());
                this.revision = changeSet.getChangesetIndex() + "";
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


        final ChangesetTable table = new ChangesetTable(folder, dataLoader
                .getProject());
        table.setEnabled(true);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.highlightParents(parents);

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	tag = null;
            	branch = null;
                text.setText(table.getSelection().getChangesetIndex()+":"+table.getSelection().getChangeset());
                changeSet = table.getSelection();
            }
        });
        table.setEnabled(true);
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
                branch = null;
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

    protected TabItem createBranchTabItem(TabFolder folder) {
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Branches");

        final BranchTable table = new BranchTable(folder);
        table.highlightParents(parents);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        table.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                text.setText(table.getSelection().getName());
                branch = table.getSelection(); 
                tag = null;
                bookmark = null;
            }
        });

        table.addListener(SWT.Show, new Listener() {
            public void handleEvent(Event event) {
                table.removeListener(SWT.Show, this);
                new SafeUiJob("Fetching branches from repository") {
                    @Override
                    protected IStatus runSafe(IProgressMonitor monitor) {
                        try {
                            Branch[] branches = dataLoader.getBranches();
                            table.setBranches(branches);
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
    
    protected TabItem createBookmarkTabItem(TabFolder folder) {
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Bookmarks");

        final BookmarkTable table = new BookmarkTable(folder, dataLoader
                .getProject());
        table.setLayoutData(new GridData(GridData.FILL_BOTH));

        table.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                text.setText(table.getSelection().getName());
                bookmark = table.getSelection();
                tag = null;
                branch = null;
            }
        });

        item.setControl(table);
        return item;
    }

    protected TabItem createHeadTabItem(TabFolder folder) {
        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText("Heads");

        final ChangesetTable table = new ChangesetTable(folder, dataLoader
                .getProject(), false);
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        table.highlightParents(parents);
        
        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	tag = null;
            	branch = null;
            	text.setText(table.getSelection().getChangesetIndex()+":"+table.getSelection().getChangeset());
                changeSet = table.getSelection();
            }
        });

        table.addListener(SWT.Show, new Listener() {
            public void handleEvent(Event event) {
                table.removeListener(SWT.Show, this);
                new SafeUiJob("Fetching heads from repository") {
                    @Override
                    protected IStatus runSafe(IProgressMonitor monitor) {
                        try {
                            table.setAutoFetch(false);
                            
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
