/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Ahlberg            - implementation
 *     VecTrace (Zingo Andersen) - updateing it
 *     Jérôme Nègre              - adding label decorator section
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By sub
 * classing <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace
 * that allows us to create a page that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that
 * belongs to the main plug-in class. That way, preferences can be accessed directly via the
 * preference store.
 */
public class HistoryPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public HistoryPreferencePage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault().getPreferenceStore());
		setDescription("Preferences for the Mercurial History View"); //$NON-NLS-1$
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to
	 * manipulate various types of preferences. Each field editor knows how to save and restore
	 * itself.
	 */
	@Override
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(
				MercurialPreferenceConstants.PREF_SIGCHECK_IN_HISTORY,
				Messages.getString("GeneralPreferencePage.verifyGpgSignaturesInHistoryView"), getFieldEditorParent())); //$NON-NLS-1$

		addField(new BooleanFieldEditor(PREF_SHOW_PATHS, "Show file information of revisions",
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(PREF_SHOW_COMMENTS, "Show full comment",
				getFieldEditorParent()));

		Group g = SWTWidgetHelper.createGroup(getFieldEditorParent(), "Merge changesets");

		addField(SWTWidgetHelper.createFontFieldEditor(
				MercurialPreferenceConstants.PREF_HISTORY_MERGE_CHANGESET_FONT, "Font", g, this,
				getPreferenceStore()));

		addField(SWTWidgetHelper.createColorFieldEditor(
				MercurialPreferenceConstants.PREF_HISTORY_MERGE_CHANGESET_BACKGROUND,
				"Background color", g, this, getPreferenceStore()));

		addField(SWTWidgetHelper.createColorFieldEditor(
				MercurialPreferenceConstants.PREF_HISTORY_MERGE_CHANGESET_FOREGROUND,
				"Foreground color", g, this, getPreferenceStore()));
	}

	public void init(IWorkbench workbench) {
	}

}