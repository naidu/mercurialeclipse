/*******************************************************************************
 * Copyright (c) 2010 VecTrace (Zingo Andersen) and others.
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
 *     Philip Graf               - Field assistance for revision field and bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import static com.vectrace.MercurialEclipse.MercurialEclipsePlugin.logError;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.commands.extensions.HgBookmarkClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Bookmark;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.FileDataLoader;
import com.vectrace.MercurialEclipse.storage.RootDataLoader;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.ui.BookmarkTable;
import com.vectrace.MercurialEclipse.ui.BranchTable;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.TagTable;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class RevisionChooserPanel extends Dialog {
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

	public RevisionChooserPanel(Shell parentShell, String title, IFile file) {
		this(parentShell, title, new FileDataLoader(file));
	}

	public RevisionChooserPanel(Shell parentShell, String title, HgRoot hgRoot) {
		this(parentShell, title, new RootDataLoader(hgRoot));
	}

	private RevisionChooserPanel(Shell parentShell, String title, DataLoader loader) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.title = title;
		dataLoader = loader;
		int[] p = {};
		try {
			p = loader.getParents();
		} catch (HgException e) {
			logError(e);
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
		gridLayout.marginWidth = 10;
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.getString("RevisionChooserDialog.rev.label")); //$NON-NLS-1$

		text = new Text(composite, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		setupRevisionFieldAssistance();

		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_HORIZONTAL
				| GridData.FILL_VERTICAL);
		data.heightHint = 200;
		tabFolder.setLayoutData(data);
		// <wrong>This is a sublist of heads: unnecessary duplication to show.</wrong>
		// The branch tab shows also *inactive* branches, which do *not* have heads.
		// it make sense to show it to see the project state at given branch
		createBranchTabItem(tabFolder);
		createHeadTabItem(tabFolder);
		createTagTabItem(tabFolder);
		createRevisionTabItem(tabFolder);
		try {
			if (MercurialUtilities.isCommandAvailable("bookmarks", //$NON-NLS-1$
					ResourceProperties.EXT_BOOKMARKS_AVAILABLE,
					"hgext.bookmarks=")) { //$NON-NLS-1$
				createBookmarkTabItem(tabFolder);
			}
		} catch (HgException e) {
			logError(e);
		}
		createOptions(composite);
		return composite;
	}

	/**
	 * Adds field assistance to the revision text field.
	 */
	private void setupRevisionFieldAssistance() {
		ContentAssistCommandAdapter contentAssist = new ContentAssistCommandAdapter(text,
				new TextContentAdapter(), new RevisionContentProposalProvider(dataLoader), null,
				null, true);
		contentAssist.setAutoActivationDelay(300);
		contentAssist.setPopupSize(new Point(320, 240));
		contentAssist.setPropagateKeys(true);
		contentAssist.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

		contentAssist.addContentProposalListener(new IContentProposalListener() {
			public void proposalAccepted(IContentProposal proposal) {
				tag = null;
				branch = null;
				bookmark = null;

				String changeSetId = proposal.getContent().split(" ", 2)[0]; //$NON-NLS-1$
				try {
					changeSet = LocalChangesetCache.getInstance().getOrFetchChangeSetById(
							dataLoader.getHgRoot(), changeSetId);
				} catch (HgException e) {
					changeSet = null;
					String message = Messages.getString(
							"RevisionChooserDialog.error.loadChangeset1", changeSetId); //$NON-NLS-1$
					logError(message, e);
				}
			}
		});
	}

	private void createOptions(Composite composite) {
		if(showForceButton){
			forceButton = new Button(composite, SWT.CHECK);
			String message = getForceText();
			if(message == null) {
				message = Messages.getString("RevisionChooserDialog.button.forcedOperation.label"); //$NON-NLS-1$
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

	@SuppressWarnings("boxing")
	@Override
	protected void okPressed() {
		String[] split = text.getText().split(":"); //$NON-NLS-1$
		revision = split[0].trim();
		if (changeSet == null) {
			HgRoot hgRoot = dataLoader.getHgRoot();
			LocalChangesetCache localCache = LocalChangesetCache.getInstance();
			if (tag != null){
				try {
					changeSet = localCache.getOrFetchChangeSetById(hgRoot, tag.getRevision()
							+ ":" + tag.getGlobalId()); //$NON-NLS-1$
				} catch (HgException ex) {
					logError(
							Messages.getString("RevisionChooserDialog.error.loadChangeset2",
									tag.getRevision(), tag.getGlobalId()), ex);
				}
			} else if(branch != null) {
				try {
					changeSet = localCache.getOrFetchChangeSetById(hgRoot, branch.getRevision()
							+ ":" + branch.getGlobalId()); //$NON-NLS-1$
				} catch (HgException ex) {
					logError(Messages.getString("RevisionChooserDialog.error.loadChangeset2",
							branch.getRevision(), branch.getGlobalId()), ex);
				}
			} else if (bookmark != null) {
				try {
					changeSet = localCache.getOrFetchChangeSetById(hgRoot, bookmark.getRevision()
							+ ":" + bookmark.getShortNodeId()); //$NON-NLS-1$
				} catch (HgException ex) {
					logError(Messages.getString("RevisionChooserDialog.error.loadChangeset2",
							bookmark.getRevision(), bookmark.getShortNodeId()), ex);
				}
			}
		}
		if (changeSet != null) {
			revision = Integer.toString(changeSet.getChangesetIndex());
		}

		if (revision.length() == 0) {
			revision = null;
		}

		if (disallowSelectingParents) {
			for (int p : parents) {
				if (String.valueOf(p).equals(revision)) {
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING);
					mb.setText("Merge"); //$NON-NLS-1$
					mb.setMessage(Messages.getString("RevisionChooserDialog.cannotMergeWithParent")); //$NON-NLS-1$
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


		final ChangesetTable table;
		if(dataLoader.getResource() != null) {
			table = new ChangesetTable(folder, dataLoader.getResource());
		} else {
			table = new ChangesetTable(folder, dataLoader.getHgRoot());
		}
		table.setAutoFetch(false);
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

		final TagTable table = new TagTable(folder, dataLoader.getHgRoot());
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
							logError(e);
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

//		table.addListener(SWT.Show, new Listener() {
//			public void handleEvent(Event event) {
//				table.removeListener(SWT.Show, this);
				new SafeUiJob(Messages.getString("RevisionChooserDialog.branchJob.description")) { //$NON-NLS-1$
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						try {
							Branch[] branches = dataLoader.getBranches();
							table.setBranches(branches);
							return Status.OK_STATUS;
						} catch (HgException e) {
							logError(e);
							return Status.CANCEL_STATUS;
						}
					}
				}.schedule();
//			}
//		});

		item.setControl(table);
		return item;
	}

	protected TabItem createBookmarkTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("RevisionChooserDialog.bookmarkTab.name")); //$NON-NLS-1$

		final BookmarkTable table = new BookmarkTable(folder, dataLoader.getHgRoot());
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

		final ChangesetTable table = new ChangesetTable(folder, dataLoader.getHgRoot());
		table.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				table.removeListener(SWT.Show, this);
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
							logError(e);
							return Status.CANCEL_STATUS;
						}
					}
				}.schedule();
			}
		});
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

	/**
	 * Proposal provider for the revision text field.
	 */
	private static class RevisionContentProposalProvider implements IContentProposalProvider {

		private final Future<SortedSet<ChangeSet>> changeSets;
		private final Future<List<Bookmark>> bookmarks;

		private RevisionContentProposalProvider(final DataLoader dataLoader) {
			ExecutorService executor = Executors.newFixedThreadPool(2);

			changeSets = executor.submit(new Callable<SortedSet<ChangeSet>>() {
				public SortedSet<ChangeSet> call() throws Exception {
					IResource resource = dataLoader.getResource();
					HgRoot hgRoot = dataLoader.getHgRoot();
					SortedSet<ChangeSet> result;
					if(resource != null) {
						result = LocalChangesetCache.getInstance().getOrFetchChangeSets(resource);
					} else {
						result = LocalChangesetCache.getInstance().getOrFetchChangeSets(hgRoot);
					}
					if(result == null || result.isEmpty() || result.first().getChangesetIndex() != 0) {
						if(resource != null) {
							LocalChangesetCache.getInstance().fetchRevisions(resource, false, 0, 0, false);
							result = LocalChangesetCache.getInstance().getOrFetchChangeSets(resource);
						} else {
							LocalChangesetCache.getInstance().fetchRevisions(hgRoot, false, 0, 0, false);
							result = LocalChangesetCache.getInstance().getOrFetchChangeSets(hgRoot);
						}

						if(result == null) {
							// fetching the change sets failed
							result = Collections.unmodifiableSortedSet(new TreeSet<ChangeSet>());
						}
					}
					return result;
				}
			});

			bookmarks = executor.submit(new Callable<List<Bookmark>>() {
				public List<Bookmark> call() throws Exception {
					return HgBookmarkClient.getBookmarks(dataLoader.getHgRoot());
				}
			});

			executor.shutdown();
		}

		public IContentProposal[] getProposals(String contents, int position) {
			List<IContentProposal> result = new LinkedList<IContentProposal>();
			String filter = contents.substring(0, position).toLowerCase();
			try {
				for (ChangeSet changeSet : changeSets.get()) {
					if (changeSet.getName().toLowerCase().startsWith(filter)
							|| changeSet.getChangeset().startsWith(filter)) {
						result.add(0, new ChangeSetContentProposal(changeSet, ContentType.REVISION));
					} else {
						String value = getTagsStartingWith(filter, changeSet);
						if (value.length() > 0) {
							result.add(0, new ChangeSetContentProposal(changeSet, ContentType.TAG, value));
						} else if (changeSet.getBranch().toLowerCase().startsWith(filter)) {
							result.add(0, new ChangeSetContentProposal(changeSet, ContentType.BRANCH, changeSet.getBranch()));
						}
					}
				}
			} catch (InterruptedException e) {
				logError(Messages.getString("RevisionChooserDialog.error.loadChangesets"), e); //$NON-NLS-1$
			} catch (ExecutionException e) {
				logError(Messages.getString("RevisionChooserDialog.error.loadChangesets"), e); //$NON-NLS-1$
			}
			try {
				for (Bookmark bookmark : bookmarks.get()) {
					if (bookmark.getName().toLowerCase().startsWith(filter)) {
						result.add(new BookmarkContentProposal(bookmark));
					}
				}
			} catch (InterruptedException e) {
				logError(Messages.getString("RevisionChooserDialog.error.loadBookmarks"), e); //$NON-NLS-1$
			} catch (ExecutionException e) {
				logError(Messages.getString("RevisionChooserDialog.error.loadBookmarks"), e); //$NON-NLS-1$
			}
			return result.toArray(new IContentProposal[result.size()]);
		}

		private String getTagsStartingWith(String filter, ChangeSet changeSet) {
			StringBuilder builder = new StringBuilder();
			for(Tag tag: changeSet.getTags()) {
				if(tag.getName().toLowerCase().startsWith(filter)) {
					builder.append(tag.getName()).append(", "); //$NON-NLS-1$
				}
			}
			if(builder.length() > 2) {
				// truncate the trailing ", "
				builder.setLength(builder.length() - 2);
			}
			return builder.toString();
		}

		private static enum ContentType {REVISION, TAG, BRANCH}

		private static class ChangeSetContentProposal implements IContentProposal {

			private static final Pattern LABEL_SPLITTER = Pattern.compile("\\.\\s|[\\n\\r]"); //$NON-NLS-1$

			private final ChangeSet changeSet;
			private final ContentType type;
			private final String value;
			private String label;
			private String description;

			private ChangeSetContentProposal(ChangeSet changeSet, ContentType type) {
				this.changeSet = changeSet;
				this.type = type;
				value = null;
			}

			private ChangeSetContentProposal(ChangeSet changeSet, ContentType type, String value) {
				this.changeSet = changeSet;
				this.type = type;
				this.value = value;
			}

			public String getContent() {
				return changeSet.getName();
			}

			public int getCursorPosition() {
				return getContent().length();
			}

			public String getDescription() {
				if(description == null) {
					description = createDescription();
				}
				return description;
			}

			private String createDescription() {
				StringBuilder builder = new StringBuilder();

				// summary
				builder.append(changeSet.getSummary()).append("\n\n"); //$NON-NLS-1$

				// branch (optional)
				String branch = changeSet.getBranch();
				if(branch != null && branch.length() > 0) {
					builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.branch")); //$NON-NLS-1$
					builder.append(": ").append(branch).append('\n'); //$NON-NLS-1$
				}

				// tag (optional)
				String tags = ChangeSetUtils.getPrintableTagsString(changeSet);
				if(tags.length() > 0) {
					builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.tags")); //$NON-NLS-1$
					builder.append(": ").append(tags).append('\n'); //$NON-NLS-1$
				}

				// author
				builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.author")); //$NON-NLS-1$
				builder.append(": ").append(changeSet.getAuthor()).append('\n'); //$NON-NLS-1$

				// date
				builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.date")); //$NON-NLS-1$
				builder.append(": ").append(changeSet.getDateString()).append('\n'); //$NON-NLS-1$

				// revision
				builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.revision")); //$NON-NLS-1$
				builder.append(": ").append(changeSet.getName()); //$NON-NLS-1$

				return builder.toString();
			}

			public String getLabel() {
				if(label == null) {
					label = createLabel();
				}
				return label;
			}

			private String createLabel() {
				StringBuilder builder = new StringBuilder(String.valueOf(changeSet.getChangesetIndex()));
				builder.append(": "); //$NON-NLS-1$

				String text;
				switch(type) {
					case TAG:
					case BRANCH:
						text = "[" + value + "] " + changeSet.getSummary(); //$NON-NLS-1$ //$NON-NLS-2$
						break;

					case REVISION:
					default:
						text = changeSet.getSummary();
						break;
				}

				// shorten label text if necessary
				if(text.length() > 50) {
					// extract first sentence or line
					text = LABEL_SPLITTER.split(text, 2)[0].trim();
					// shorten it if still too long
					if(text.length() > 50) {
						text = text.substring(0, 43).trim() + "..."; //$NON-NLS-1$
					}
					builder.append(text);
				} else {
					builder.append(text);
				}

				return builder.toString();
			}

		}

		private static class BookmarkContentProposal implements IContentProposal {

			private final Bookmark bookmark;

			private BookmarkContentProposal(Bookmark bookmark) {
				this.bookmark = bookmark;
			}

			public String getContent() {
				return bookmark.getRevision() + ":" + bookmark.getShortNodeId(); //$NON-NLS-1$
			}

			public int getCursorPosition() {
				return getContent().length();
			}

			public String getDescription() {
				return bookmark.getRevision() + ":" + bookmark.getShortNodeId() + "\n\n" + bookmark.getName(); //$NON-NLS-1$ //$NON-NLS-2$
			}

			public String getLabel() {
				return bookmark.getRevision() + ": " + bookmark.getName(); //$NON-NLS-1$
			}

		}

	}

}
