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
 *     Adam Berkes (Intland)     - bug fixes
 *     Andrei Loskutov (Intland) - bug fixes
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
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
 */
public class RevisionChooserDialog extends Dialog {
	private final DataLoader dataLoader;
	private final String title;
	private Text text;
	private String revision;
	private Tag tag;
	private Branch branch;
	private Bookmark bookmark;
	private boolean defaultShowingHeads;
	private boolean disallowSelectingParents;

	private final int[] parents;

	private ChangeSet changeSet;
	private boolean showForceButton;
	private Button forceButton;
	private String forceButtonText;
	private boolean isForceChecked;

	public RevisionChooserDialog(Shell parentShell, String title, IFile file) {
		this(parentShell, title, new FileDataLoader(file));
	}

	public RevisionChooserDialog(Shell parentShell, String title, IProject project) {
		this(parentShell, title, new ProjectDataLoader(project));
	}

	public RevisionChooserDialog(Shell parentShell, String title, DataLoader loader) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.title = title;
		dataLoader = loader;
		int[] p = {};
		try {
			p = loader.getParents();
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		parents = p;
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
		label.setText(Messages.getString("RevisionChooserDialog.rev.label")); //$NON-NLS-1$

		text = new Text(composite, SWT.BORDER | SWT.DROP_DOWN);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL
				| GridData.FILL_VERTICAL);
		data.heightHint = 200;
		tabFolder.setLayoutData(data);
		createHeadTabItem(tabFolder);
		createRevisionTabItem(tabFolder);
		createTagTabItem(tabFolder);
		// <wrong>This is a sublist of heads: unnecessary duplication to show.</wrong>
		// The branch tab shows also *inactive* branches, which do *not* have heads.
		// it make sense to show it to see the project state at given branch
		createBranchTabItem(tabFolder);
		try {
			if (MercurialUtilities.isCommandAvailable("bookmarks", //$NON-NLS-1$
					ResourceProperties.EXT_BOOKMARKS_AVAILABLE,
					"hgext.bookmarks=")) { //$NON-NLS-1$
				createBookmarkTabItem(tabFolder);
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		createOptions(composite);
		return composite;
	}

	private void createOptions(Composite composite) {
		if(showForceButton){
			forceButton = new Button(composite, SWT.CHECK);
			String message = getForceText();
			if(message == null) {
				message = "Forced operation";
			}
			forceButton.setText(message);
			forceButton.setSelection(isForceChecked);
			forceButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					isForceChecked = forceButton.getSelection();
				}
			});
		}
	}

	public void setForceChecked(boolean on){
		isForceChecked = true;
	}
	public boolean isForceChecked(){
		return isForceChecked;
	}

	private String getForceText() {
		return forceButtonText;
	}

	public void showForceButton(boolean show){
		showForceButton = show;
	}

	public void setForceButtonText(String forceButtonText) {
		this.forceButtonText = forceButtonText;
	}

	@Override
	protected void okPressed() {
		String[] split = text.getText().split(":"); //$NON-NLS-1$
		revision = split[0].trim();
		if (changeSet == null) {
			IProject project = dataLoader.getProject();
			LocalChangesetCache localCache = LocalChangesetCache.getInstance();
			try {
				if (tag != null){
					changeSet = localCache.getOrFetchChangeSetById(project, tag.getRevision() + ":" + tag.getGlobalId());
				} else if(branch != null) {
					changeSet = localCache.getOrFetchChangeSetById(project, branch.getRevision() + ":" + branch.getGlobalId()); //$NON-NLS-1$
				} else if (bookmark != null) {
					changeSet = localCache.getOrFetchChangeSetById(project, bookmark.getRevision() + ":" + bookmark.getShortNodeId()); //$NON-NLS-1$
				}
			} catch (HgException ex) {
				MercurialEclipsePlugin.logError("Unable to get or fetch revision: <" + tag.getRevision() + ":" + tag.getGlobalId() + ">", ex);
			}
		}
		if (changeSet != null) {
			revision = changeSet.getChangesetIndex() + ""; //$NON-NLS-1$
		}

		if (revision.length() == 0) {
			revision = null;
		}

		if (disallowSelectingParents) {
			for (int p : parents) {
				if (String.valueOf(p).equals(revision)) {
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING);
					mb.setText("Merge");
					mb.setMessage(Messages.getString("RevisionChooserDialog.cannotMergeWithParent"));
					mb.open();
					return;
				}
			}
		}

		super.okPressed();
	}

	public String getRevision() {
		return revision;
	}

	protected TabItem createRevisionTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("RevisionChooserDialog.revTab.name")); //$NON-NLS-1$


		final ChangesetTable table = new ChangesetTable(folder, dataLoader
				.getResource(), false);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.highlightParents(parents);

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tag = null;
				branch = null;
				bookmark = null;
				text.setText(table.getSelection().getChangesetIndex()+":"+table.getSelection().getChangeset()); //$NON-NLS-1$
				changeSet = table.getSelection();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				okPressed();
			}
		});

		table.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				table.removeListener(SWT.Show, this);
				new SafeUiJob(Messages.getString("RevisionChooserDialog.tagJob.description")) { //$NON-NLS-1$
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						table.setAutoFetch(true);
						table.setEnabled(true);
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		});

		item.setControl(table);
		return item;
	}

	protected TabItem createTagTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("RevisionChooserDialog.tagTab.name")); //$NON-NLS-1$

		final TagTable table = new TagTable(folder, dataLoader.getProject());
		table.highlightParents(parents);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		table.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				text.setText(table.getSelection().getName());
				tag = table.getSelection();
				branch = null;
				bookmark = null;
				changeSet = null;
			}
		});

		table.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				table.removeListener(SWT.Show, this);
				new SafeUiJob(Messages.getString("RevisionChooserDialog.tagJob.description")) { //$NON-NLS-1$
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
		item.setText(Messages.getString("RevisionChooserDialog.branchTab.name")); //$NON-NLS-1$

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
				changeSet = null;
			}
		});

		table.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				table.removeListener(SWT.Show, this);
				new SafeUiJob(Messages.getString("RevisionChooserDialog.branchJob.description")) { //$NON-NLS-1$
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
		item.setText(Messages.getString("RevisionChooserDialog.bookmarkTab.name")); //$NON-NLS-1$

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
				changeSet = null;
			}
		});

		item.setControl(table);
		return item;
	}

	protected TabItem createHeadTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("RevisionChooserDialog.headTab.name")); //$NON-NLS-1$

		final ChangesetTable table = new ChangesetTable(folder, dataLoader
				.getProject());
		new SafeUiJob(Messages.getString("RevisionChooserDialog.fetchJob.description")) { //$NON-NLS-1$
			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				try {
					table.setLayoutData(new GridData(GridData.FILL_BOTH));
					table.highlightParents(parents);
					table.setAutoFetch(false);

					ChangeSet[] revisions = dataLoader.getHeads();
					table.setChangesets(revisions);
					table.setEnabled(true);
					return Status.OK_STATUS;
				} catch (HgException e) {
					MercurialEclipsePlugin.logError(e);
					return Status.CANCEL_STATUS;
				}
			}
		}.schedule();

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tag = null;
				branch = null;
				bookmark = null;
				text.setText(table.getSelection().getChangesetIndex()+":"+table.getSelection().getChangeset()); //$NON-NLS-1$
				changeSet = table.getSelection();
			}
		});

		item.setControl(table);
		if (defaultShowingHeads) {
			folder.setSelection(item);
		}
		return item;
	}

	public ChangeSet getChangeSet() {
		return changeSet;
	}

	public void setDefaultShowingHeads(boolean defaultShowingHeads) {
		this.defaultShowingHeads = defaultShowingHeads;
	}

	public void setDisallowSelectingParents(boolean b) {
		this.disallowSelectingParents = b;
	}
}
