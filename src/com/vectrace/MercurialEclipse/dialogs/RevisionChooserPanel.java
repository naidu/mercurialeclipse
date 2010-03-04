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

import java.util.ArrayList;
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
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
import com.vectrace.MercurialEclipse.storage.EmptyDataLoader;
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
public class RevisionChooserPanel extends Composite {

	public static class Settings {
		public boolean defaultShowingHeads;
		public boolean disallowSelectingParents;
		public boolean showForceButton;
		public boolean isForceChecked;
		public boolean highlightDefaultBranch;
		public String forceButtonText;
		public String revision;
		public ChangeSet changeSet;
	}

	private DataLoader dataLoader;
	private final Text text;
	private int[] parents;
	private final Settings data;

	private Tag tag;
	private Branch branch;
	private Bookmark bookmark;
	private Button forceButton;
	private RevisionChooserDialog dialog;
	private ContentAssistCommandAdapter contentAssist;

	public RevisionChooserPanel(Composite parent, DataLoader loader, Settings settings) {
		super(parent, SWT.NONE);
		this.data = settings;

		setDataLoader(loader);

		GridLayout gridLayout = new GridLayout(1, true);
		setLayoutData(new GridData(GridData.FILL_BOTH));
		setLayout(gridLayout);

		Label label = new Label(this, SWT.NONE);
		label.setText(Messages.getString("RevisionChooserDialog.rev.label")); //$NON-NLS-1$

		text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if(data.highlightDefaultBranch){
			text.setText(Branch.DEFAULT);
		}
		text.addFocusListener(new FocusListener() {
			String textStr;

			public void focusLost(FocusEvent e) {
				String newText = text.getText();
				if(newText.equals(textStr)){
					return;
				}
				// cleanup not up-to-date data
				data.changeSet = null;
				bookmark = null;
				tag = null;
				branch = null;
			}

			public void focusGained(FocusEvent e) {
				textStr = text.getText();
			}
		});
		setupRevisionFieldAssistance();

		TabFolder tabFolder = new TabFolder(this, SWT.NONE);
		GridData gdata = new GridData(GridData.FILL_HORIZONTAL	| GridData.FILL_VERTICAL);
		gdata.heightHint = 200;
		tabFolder.setLayoutData(gdata);
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
		createOptions(this);
	}

	public void setDataLoader(DataLoader loader) {
		dataLoader = loader;
		int[] p = {};
		try {
			p = loader.getParents();
		} catch (HgException e) {
			logError(e);
		}
		parents = p;
	}

	/**
	 * Adds field assistance to the revision text field.
	 */
	private void setupRevisionFieldAssistance() {
		contentAssist = new ContentAssistCommandAdapter(text,
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
					data.changeSet = LocalChangesetCache.getInstance().getOrFetchChangeSetById(
							dataLoader.getHgRoot(), changeSetId);
				} catch (HgException e) {
					data.changeSet = null;
					String message = Messages.getString(
							"RevisionChooserDialog.error.loadChangeset1", changeSetId); //$NON-NLS-1$
					logError(message, e);
				}
			}
		});
	}

	private void createOptions(Composite composite) {
		if(data.showForceButton){
			forceButton = new Button(composite, SWT.CHECK);
			String message = getForceText();
			if(message == null) {
				message = Messages.getString("RevisionChooserDialog.button.forcedOperation.label"); //$NON-NLS-1$
			}
			forceButton.setText(message);
			forceButton.setSelection(data.isForceChecked);
			forceButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					data.isForceChecked = forceButton.getSelection();
				}
			});
		}
	}

	public boolean isForceChecked(){
		return data.isForceChecked;
	}

	private String getForceText() {
		return data.forceButtonText;
	}

	@SuppressWarnings("boxing")
	public boolean calculateRevision() {
		String[] split = text.getText().split(":"); //$NON-NLS-1$
		data.revision = split[0].trim();
		if (data.changeSet == null) {
			HgRoot hgRoot = dataLoader.getHgRoot();
			LocalChangesetCache localCache = LocalChangesetCache.getInstance();
			if (tag != null){
				try {
					data.changeSet = localCache.getOrFetchChangeSetById(hgRoot, tag.getRevision()
							+ ":" + tag.getGlobalId()); //$NON-NLS-1$
				} catch (HgException ex) {
					logError(
							Messages.getString("RevisionChooserDialog.error.loadChangeset2",
									tag.getRevision(), tag.getGlobalId()), ex);
				}
			} else if(branch != null) {
				try {
					data.changeSet = localCache.getOrFetchChangeSetById(hgRoot, branch.getRevision()
							+ ":" + branch.getGlobalId()); //$NON-NLS-1$
				} catch (HgException ex) {
					logError(Messages.getString("RevisionChooserDialog.error.loadChangeset2",
							branch.getRevision(), branch.getGlobalId()), ex);
				}
			} else if (bookmark != null) {
				try {
					data.changeSet = localCache.getOrFetchChangeSetById(hgRoot, bookmark.getRevision()
							+ ":" + bookmark.getShortNodeId()); //$NON-NLS-1$
				} catch (HgException ex) {
					logError(Messages.getString("RevisionChooserDialog.error.loadChangeset2",
							bookmark.getRevision(), bookmark.getShortNodeId()), ex);
				}
			}
		}
		if (data.changeSet != null) {
			data.revision = Integer.toString(data.changeSet.getChangesetIndex());
		}

		if (data.revision.length() == 0) {
			data.revision = null;
		}

		if (data.disallowSelectingParents) {
			for (int p : parents) {
				if (String.valueOf(p).equals(data.revision)) {
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING);
					mb.setText("Merge"); //$NON-NLS-1$
					mb.setMessage(Messages.getString("RevisionChooserDialog.cannotMergeWithParent")); //$NON-NLS-1$
					mb.open();
					return false;
				}
			}
		}
		return true;
	}

	public void applyRevision() {
		if(calculateRevision()){
			if(dialog != null){
				dialog.revisionSelected();
			}
		}
	}

	public String getRevision() {
		return data.revision;
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
				ChangeSet selection = table.getSelection();
				text.setText(selection.getChangesetIndex() + ":" + selection.getChangeset()); //$NON-NLS-1$
				data.changeSet = selection;
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				applyRevision();
			}
		});

		table.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				table.removeListener(SWT.Show, this);
				new SafeUiJob(Messages.getString("RevisionChooserDialog.tagJob.description")) { //$NON-NLS-1$
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						table.setHgRoot(dataLoader.getHgRoot());
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
				tag = table.getSelection();
				text.setText(tag.getName());
				branch = null;
				bookmark = null;
				data.changeSet = null;
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				applyRevision();
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
							table.setHgRoot(dataLoader.getHgRoot());
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
		if(data.highlightDefaultBranch) {
			table.highlightBranch(Branch.DEFAULT);
		}
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				branch = table.getSelection();
				text.setText(branch.getName());
				tag = null;
				bookmark = null;
				data.changeSet = null;
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				applyRevision();
			}
		});

		boolean needListener = dataLoader == null || dataLoader instanceof EmptyDataLoader;
		final SafeUiJob fetchData = new SafeUiJob(Messages.getString("RevisionChooserDialog.branchJob.description")) { //$NON-NLS-1$
			@Override
			protected IStatus runSafe(IProgressMonitor monitor) {
				try {
					Branch[] branches = dataLoader.getBranches();
					table.setBranches(branches);
					table.redraw();
					table.update();
					return Status.OK_STATUS;
				} catch (HgException e) {
					logError(e);
					return Status.CANCEL_STATUS;
				}
			}
		};
		if(needListener){
			table.addListener(SWT.Paint, new Listener() {
				public void handleEvent(Event event) {
					table.removeListener(SWT.Paint, this);
					fetchData.schedule();
				}
			});
		} else {
			fetchData.schedule();
		}

		item.setControl(table);
		return item;
	}

	protected TabItem createBookmarkTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("RevisionChooserDialog.bookmarkTab.name")); //$NON-NLS-1$

		final BookmarkTable table = new BookmarkTable(folder);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		table.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				table.removeListener(SWT.Show, this);
				new SafeUiJob(Messages.getString("RevisionChooserDialog.bookmarksJob.name")) { //$NON-NLS-1$
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						table.updateTable(dataLoader.getHgRoot());
						table.setEnabled(true);
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		});

		table.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				bookmark = table.getSelection();
				text.setText(bookmark.getName());
				tag = null;
				branch = null;
				data.changeSet = null;
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				applyRevision();
			}
		});

		item.setControl(table);
		return item;
	}

	protected TabItem createHeadTabItem(TabFolder folder) {
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.getString("RevisionChooserDialog.headTab.name")); //$NON-NLS-1$

		final ChangesetTable table = new ChangesetTable(folder, dataLoader.getHgRoot());
		table.setAutoFetch(false);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {
				table.removeListener(SWT.Show, this);
				new SafeUiJob(Messages.getString("RevisionChooserDialog.fetchJob.description")) { //$NON-NLS-1$
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						try {
							table.setHgRoot(dataLoader.getHgRoot());
							table.highlightParents(parents);
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
				ChangeSet selection = table.getSelection();
				data.changeSet = selection;
				text.setText(selection.getChangesetIndex() + ":" + selection.getChangeset()); //$NON-NLS-1$
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				applyRevision();
			}
		});

		item.setControl(table);
		if (data.defaultShowingHeads) {
			folder.setSelection(item);
		}
		return item;
	}

	public ChangeSet getChangeSet() {
		return data.changeSet;
	}

	public void addSelectionListener(RevisionChooserDialog dialog1) {
		this.dialog = dialog1;
	}

	public void update(DataLoader loader){
		setDataLoader(loader);
		contentAssist.setContentProposalProvider(new RevisionContentProposalProvider(loader));
	}

	/**
	 * Proposal provider for the revision text field.
	 */
	private static class RevisionContentProposalProvider implements IContentProposalProvider {

		private final Future<SortedSet<ChangeSet>> changeSets;
		private final Future<List<Bookmark>> bookmarks;

		private RevisionContentProposalProvider(final DataLoader dataLoader) {

			ExecutorService executor = Executors.newFixedThreadPool(2);
			if(dataLoader instanceof EmptyDataLoader){
				changeSets = executor.submit(new Callable<SortedSet<ChangeSet>>() {
					public SortedSet<ChangeSet> call() throws Exception {
						return Collections.unmodifiableSortedSet(new TreeSet<ChangeSet>());
					}
				});

				bookmarks = executor.submit(new Callable<List<Bookmark>>() {
					public List<Bookmark> call() throws Exception {
						return Collections.unmodifiableList(new ArrayList<Bookmark>());
					}
				});
			} else {
				changeSets = executor.submit(new Callable<SortedSet<ChangeSet>>() {
					public SortedSet<ChangeSet> call() throws Exception {
						IResource resource = dataLoader.getResource();
						HgRoot hgRoot = dataLoader.getHgRoot();
						SortedSet<ChangeSet> result;
						LocalChangesetCache cache = LocalChangesetCache.getInstance();
						if(resource != null) {
							result = cache.getOrFetchChangeSets(resource);
						} else {
							result = cache.getOrFetchChangeSets(hgRoot);
						}
						if(result == null || result.isEmpty() || result.first().getChangesetIndex() != 0) {
							if(resource != null) {
								cache.fetchRevisions(resource, false, 0, 0, false);
								result = cache.getOrFetchChangeSets(resource);
							} else {
								cache.fetchRevisions(hgRoot, false, 0, 0, false);
								result = cache.getOrFetchChangeSets(hgRoot);
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
			}
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
