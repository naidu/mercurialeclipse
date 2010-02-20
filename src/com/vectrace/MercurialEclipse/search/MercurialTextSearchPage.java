/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Juerg Billeter, juergbi@ethz.ch - 47136 Search view should show match objects
 *     Ulrich Etter, etteru@ethz.ch - 47136 Search view should show match objects
 *     Roman Fuchs, fuchsro@ethz.ch - 47136 Search view should show match objects
 *     Bastian Doetsch - adaptation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.ISearchHelpContextIds;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * @author Bastian
 *
 */
@SuppressWarnings("restriction")
public class MercurialTextSearchPage extends DialogPage implements ISearchPage {
	private static class SearchPatternData {
		public final boolean isCaseSensitive;
		public final boolean isRegExSearch;
		public final String textPattern;
		public final int scope;
		public final IWorkingSet[] workingSets;

		public SearchPatternData(String textPattern, boolean isCaseSensitive,
				boolean isRegExSearch, String[] fileNamePatterns, int scope,
				IWorkingSet[] workingSets) {
			Assert.isNotNull(fileNamePatterns);
			this.isCaseSensitive = isCaseSensitive;
			this.isRegExSearch = isRegExSearch;
			this.textPattern = textPattern;
			this.scope = scope;
			this.workingSets = workingSets; // can be null
		}

	}

	private ISearchPageContainer container;
	private Combo fExtensions;
	private Button fIsRegExCheckbox;
	private Combo fPattern;
	private boolean fIsCaseSensitive;
	private CLabel fStatusLabel;
	private ContentAssistCommandAdapter fPatterFieldContentAssist;
	private boolean fIsRegExSearch;
	private Button fIsCaseSensitiveCheckbox;
	@SuppressWarnings("unchecked")
	private final List fPreviousSearchPatterns = new ArrayList(20);

	/**
	 *
	 */
	public MercurialTextSearchPage() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 */
	public MercurialTextSearchPage(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @param image
	 */
	public MercurialTextSearchPage(String title, ImageDescriptor image) {
		super(title, image);
		// TODO Auto-generated constructor stub
	}

	public boolean performAction() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setContainer(ISearchPageContainer container) {
		this.container = container;
	}

	private void statusMessage(boolean error, String message) {
		fStatusLabel.setText(message);
		if (error) {
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel
					.getDisplay()));
		} else {
			fStatusLabel.setForeground(null);
		}
	}

	private boolean validateRegex() {
		if (fIsRegExCheckbox.getSelection()) {
			try {
				PatternConstructor.createPattern(fPattern.getText(),
						fIsCaseSensitive, true);
			} catch (PatternSyntaxException e) {
				String locMessage = e.getLocalizedMessage();
				int i = 0;
				while (i < locMessage.length()
						&& "\n\r".indexOf(locMessage.charAt(i)) == -1) {
					i++;
				}
				statusMessage(true, locMessage.substring(0, i)); // only take
				// first
				// line
				return false;
			}
			statusMessage(false, "");
		} else {
			statusMessage(false, SearchMessages.SearchPage_containingText_hint);
		}
		return true;
	}

	private ISearchPageContainer getContainer() {
		return this.container;
	}

	final void updateOKStatus() {
		boolean regexStatus = validateRegex();
		boolean hasFilePattern = fExtensions.getText().length() > 0;
		getContainer().setPerformActionEnabled(regexStatus && hasFilePattern);
	}

	private void handleWidgetSelected() {
		int selectionIndex = fPattern.getSelectionIndex();
		if (selectionIndex < 0
				|| selectionIndex >= fPreviousSearchPatterns.size()) {
			return;
		}

		SearchPatternData patternData = (SearchPatternData) fPreviousSearchPatterns
				.get(selectionIndex);
		if (!fPattern.getText().equals(patternData.textPattern)) {
			return;
		}
		fIsCaseSensitiveCheckbox.setSelection(patternData.isCaseSensitive);
		fIsRegExCheckbox.setSelection(patternData.isRegExSearch);
		fPattern.setText(patternData.textPattern);
		if (patternData.workingSets != null) {
			getContainer().setSelectedWorkingSets(patternData.workingSets);
		} else {
			getContainer().setSelectedScope(patternData.scope);
		}
	}

	private void addTextPatternControls(Composite group) {
		// grid layout with 2 columns

		// Info text
		Label label = new Label(group, SWT.LEAD);
		label.setText(SearchMessages.SearchPage_containingText_text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2,
				1));
		label.setFont(group.getFont());

		// Pattern combo
		fPattern = new Combo(group, SWT.SINGLE | SWT.BORDER);
		// Not done here to prevent page from resizing
		// fPattern.setItems(getPreviousSearchPatterns());
		fPattern.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelected();
				updateOKStatus();
			}
		});
		// add some listeners for regex syntax checking
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		fPattern.setFont(group.getFont());
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, false,
				1, 1);
		data.widthHint = convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);

		ComboContentAdapter contentAdapter = new ComboContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(
				true);
		fPatterFieldContentAssist = new ContentAssistCommandAdapter(fPattern,
				contentAdapter, findProposer,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
				new char[] { '\\', '[', '(' }, true);
		fPatterFieldContentAssist.setEnabled(fIsRegExSearch);

		fIsCaseSensitiveCheckbox = new Button(group, SWT.CHECK);
		fIsCaseSensitiveCheckbox
				.setText(SearchMessages.SearchPage_caseSensitive);
		fIsCaseSensitiveCheckbox.setSelection(fIsCaseSensitive);
		fIsCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fIsCaseSensitive = fIsCaseSensitiveCheckbox.getSelection();
			}
		});
		fIsCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL,
				SWT.CENTER, false, false, 1, 1));
		fIsCaseSensitiveCheckbox.setFont(group.getFont());

		// Text line which explains the special characters
		fStatusLabel = new CLabel(group, SWT.LEAD);
		fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));
		fStatusLabel.setFont(group.getFont());
		fStatusLabel.setAlignment(SWT.LEFT);
		fStatusLabel.setText(SearchMessages.SearchPage_containingText_hint);

		// RegEx checkbox
		fIsRegExCheckbox = new Button(group, SWT.CHECK);
		fIsRegExCheckbox.setText(SearchMessages.SearchPage_regularExpression);
		fIsRegExCheckbox.setSelection(fIsRegExSearch);

		fIsRegExCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fIsRegExSearch = fIsRegExCheckbox.getSelection();
				updateOKStatus();
				fPatterFieldContentAssist.setEnabled(fIsRegExSearch);
			}
		});
		fIsRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
				false, false, 1, 1));
		fIsRegExCheckbox.setFont(group.getFont());
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite result = new Composite(parent, SWT.NONE);
		result.setFont(parent.getFont());
		GridLayout layout = new GridLayout(2, false);
		result.setLayout(layout);

		addTextPatternControls(result);

		Label separator = new Label(result, SWT.NONE);
		separator.setVisible(false);
		GridData data = new GridData(GridData.FILL, GridData.FILL, false,
				false, 2, 1);
		data.heightHint = convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);

		setControl(result);
		Dialog.applyDialogFont(result);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(result,
				ISearchHelpContextIds.TEXT_SEARCH_PAGE);
	}

}
