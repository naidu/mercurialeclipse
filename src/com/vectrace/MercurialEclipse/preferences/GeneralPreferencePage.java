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
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
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

public class GeneralPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

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
            MercurialEclipsePlugin.getDefault().checkHgInstallation();
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

    public GeneralPreferencePage() {
        super(GRID);
        setPreferenceStore(MercurialEclipsePlugin.getDefault()
                .getPreferenceStore());
        setDescription(Messages.getString("GeneralPreferencePage.description")); //$NON-NLS-1$
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
                Messages.getString("GeneralPreferencePage.field.hgExecutable"), getFieldEditorParent()); //$NON-NLS-1$

        addField(execField);
        if (!MercurialEclipsePlugin.getDefault().isHgUsable()) {
            execField.setErrorMessage(Messages.getString("GeneralPreferencePage.error.HgNotInstalled")); //$NON-NLS-1$
        }

        addField(new GpgExecutableFileFieldEditor(
                MercurialPreferenceConstants.GPG_EXECUTABLE,
                Messages.getString("GeneralPreferencePage.field.gpgExecutable"), getFieldEditorParent())); //$NON-NLS-1$

        addField(new StringFieldEditor(
                MercurialPreferenceConstants.MERCURIAL_USERNAME,
                Messages.getString("GeneralPreferencePage.field.username"), getFieldEditorParent())); //$NON-NLS-1$
        
        addField(new BooleanFieldEditor(
                MercurialPreferenceConstants.PREF_USE_EXTERNAL_MERGE,
                Messages.getString("GeneralPreferencePage.useExternalMergeTool"), getFieldEditorParent())); //$NON-NLS-1$

        addField(new LabelDecoratorRadioGroupFieldEditor(
                MercurialPreferenceConstants.LABELDECORATOR_LOGIC,
                Messages.getString("GeneralPreferencePage.field.decorationGroup.description"), //$NON-NLS-1$
                1,
                new String[][] {
                        {
                                Messages.getString("GeneralPreferencePage.field.decorationGroup.asModified"), //$NON-NLS-1$
                                MercurialPreferenceConstants.LABELDECORATOR_LOGIC_2MM },
                        {
                                Messages.getString("GeneralPreferencePage.field.decorationGroup.mostImportant"), //$NON-NLS-1$
                                MercurialPreferenceConstants.LABELDECORATOR_LOGIC_HB } },
                getFieldEditorParent(), true));

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

}