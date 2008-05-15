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
 *******************************************************************************/

package com.vectrace.MercurialEclipse.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By sub classing <samp>FieldEditorPreferencePage</samp>,
 * we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class TimeoutPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

   
    private class TimeoutFieldEditor extends IntegerFieldEditor {
        private TimeoutFieldEditor(String name, String labelText,
                Composite parent) {
            super(name, labelText, parent);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.jface.preference.FieldEditor#load()
         */
        @Override
        public void load() {
            super.load();
            if (getIntValue() <= 0) {
                super.setPresentsDefaultValue(true);
                super.setStringValue(String
                        .valueOf(AbstractShellCommand.DEFAULT_TIMEOUT));
            }
        }
    }

    public TimeoutPreferencePage() {
        super(GRID);
        setPreferenceStore(MercurialEclipsePlugin.getDefault()
                .getPreferenceStore());
        setDescription("MercurialEclipse plugin for Mercurial(Hg) version control system");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {               

        // timeout preferences
        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.CLONE_TIMEOUT,
                "Clone timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.PUSH_TIMEOUT, "Push timeout (ms):",
                getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.PULL_TIMEOUT,
                "Pull/Incoming/Outgoing timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.UPDATE_TIMEOUT,
                "Update timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.COMMIT_TIMEOUT,
                "Commit timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.IMERGE_TIMEOUT,
                "Imerge timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.LOG_TIMEOUT,
                "Log/GLog timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.STATUS_TIMEOUT,
                "Status timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.ADD_TIMEOUT, "Add timeout (ms):",
                getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.REMOVE_TIMEOUT,
                "Remove timeout (ms):", getFieldEditorParent()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

}