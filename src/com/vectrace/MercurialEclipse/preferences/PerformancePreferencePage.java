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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

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

public class PerformancePreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    /**
     * @author bastian
     * 
     */
    private final class StatusBatchSizeIntegerFieldEditor extends
            IntegerFieldEditor {
        /**
         * @param name
         * @param labelText
         * @param parent
         */
        private StatusBatchSizeIntegerFieldEditor(String name,
                String labelText, Composite parent) {
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
                super.setStringValue(String.valueOf(DEFAULT_STATUS_BATCH_SIZE));
            }
        }
    }

    /**
     * @author bastian
     * 
     */
    private final class LogBatchSizeIntegerFieldEditor extends
            IntegerFieldEditor {
        /**
         * @param name
         * @param labelText
         * @param parent
         */
        private LogBatchSizeIntegerFieldEditor(String name, String labelText,
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
                super.setStringValue(String.valueOf(DEFAULT_LOG_BATCH_SIZE));
            }
        }
    }

    protected static final int DEFAULT_LOG_BATCH_SIZE = 500;
    protected static final int DEFAULT_STATUS_BATCH_SIZE = 10;

    public PerformancePreferencePage() {
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
        // batch size preferences

        addField(new LogBatchSizeIntegerFieldEditor(
                MercurialPreferenceConstants.LOG_BATCH_SIZE,
                "hg log revision limit:",
                getFieldEditorParent()));

        addField(new StatusBatchSizeIntegerFieldEditor(
                MercurialPreferenceConstants.STATUS_BATCH_SIZE,
                "Number of files to query hg status with:", getFieldEditorParent()));
        
        addField(new BooleanFieldEditor(MercurialPreferenceConstants.RESOURCE_DECORATOR_DEEP_COMPUTATION,
                "Always call hg status for whole repository (Compute deep decorator status)",getFieldEditorParent()));
        
        addField(new BooleanFieldEditor(MercurialPreferenceConstants.RESOURCE_DECORATOR_SHOW_CHANGESET,
                "Show changeset information for files",getFieldEditorParent()));
        
        addField(new BooleanFieldEditor(MercurialPreferenceConstants.SYNCHRONIZE_FILES,
                "Synchronize view compares files",getFieldEditorParent()));
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

}