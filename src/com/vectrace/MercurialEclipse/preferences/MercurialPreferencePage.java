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
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

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

public class MercurialPreferencePage extends FieldEditorPreferencePage
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

    private final class LabelDecoratorRadioGroupFieldEditor extends
            RadioGroupFieldEditor {
        private LabelDecoratorRadioGroupFieldEditor(String name,
                String labelText, int numColumns, String[][] labelAndValues,
                Composite parent, boolean useGroup) {
            super(name, labelText, numColumns, labelAndValues, parent, useGroup);
        }

        @Override
        protected void doStore() {
            super.doStore();
            try {
                MercurialEclipsePlugin.getDefault().checkHgInstallation();
                if (MercurialEclipsePlugin.getDefault().isHgUsable()) {
                    MercurialStatusCache.getInstance().refresh();
                }
            } catch (TeamException e) {
                MercurialEclipsePlugin.logError(
                        "Couldn't refresh projects in workspace.", e);
            }
            // ResourceDecorator.onConfigurationChanged();
        }
    }

    private final class MercurialExecutableFileFieldEditor extends
            FileFieldEditor {
        private MercurialExecutableFileFieldEditor(String name,
                String labelText, Composite parent) {
            super(name, labelText, parent);
        }

        @Override
        protected boolean checkState() {
            // There are other ways of doing this properly but this is
            // better than the default behaviour
            return MercurialPreferenceConstants.MERCURIAL_EXECUTABLE
                    .equals(getTextControl().getText())
                    || super.checkState();
        }
    }

    private final class GpgExecutableFileFieldEditor extends FileFieldEditor {
        private GpgExecutableFileFieldEditor(String name, String labelText,
                Composite parent) {
            super(name, labelText, parent);
        }

        @Override
        protected boolean checkState() {
            // There are other ways of doing this properly but this is
            // better than the default behaviour
            return MercurialPreferenceConstants.GPG_EXECUTABLE
                    .equals(getTextControl().getText())
                    || super.checkState();
        }
    }

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

    protected static final int DEFAULT_LOG_BATCH_SIZE = 500;
    protected static final int DEFAULT_STATUS_BATCH_SIZE = 10;

    public MercurialPreferencePage() {
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
        FileFieldEditor execField = new MercurialExecutableFileFieldEditor(
                MercurialPreferenceConstants.MERCURIAL_EXECUTABLE,
                "Mercurial &Executable:", getFieldEditorParent());

        addField(execField);
        if (!MercurialEclipsePlugin.getDefault().isHgUsable()) {
            execField.setErrorMessage("Hg is not correctly installed.");
        }

        addField(new GpgExecutableFileFieldEditor(
                MercurialPreferenceConstants.GPG_EXECUTABLE,
                "&GnuPG Executable:", getFieldEditorParent()));

        addField(new StringFieldEditor(
                MercurialPreferenceConstants.MERCURIAL_USERNAME,
                "Mercurial &Username:", getFieldEditorParent()));

        addField(new LabelDecoratorRadioGroupFieldEditor(
                MercurialPreferenceConstants.LABELDECORATOR_LOGIC,
                "When a folders contains files with different statuses, flag the folder:",
                1,
                new String[][] {
                        {
                                "as modified",
                                MercurialPreferenceConstants.LABELDECORATOR_LOGIC_2MM },
                        {
                                "with the most important status",
                                MercurialPreferenceConstants.LABELDECORATOR_LOGIC_HB } },
                getFieldEditorParent(), true));

        // batch size preferences

        addField(new LogBatchSizeIntegerFieldEditor(
                MercurialPreferenceConstants.LogBatchSize,
                "hg log revision limit:",
                getFieldEditorParent()));

        addField(new StatusBatchSizeIntegerFieldEditor(
                MercurialPreferenceConstants.StatusBatchSize,
                "Number of files to query hg status with:", getFieldEditorParent()));

        // timeout preferences
        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.CloneTimeout,
                "Clone timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.PushTimeout, "Push timeout (ms):",
                getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.PullTimeout,
                "Pull/Incoming/Outgoing timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.UpdateTimeout,
                "Update timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.CommitTimeout,
                "Commit timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.ImergeTimeout,
                "Imerge timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.LogTimeout,
                "Log/GLog timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.StatusTimeout,
                "Status timeout (ms):", getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.AddTimeout, "Add timeout (ms):",
                getFieldEditorParent()));

        addField(new TimeoutFieldEditor(
                MercurialPreferenceConstants.RemoveTimeout,
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