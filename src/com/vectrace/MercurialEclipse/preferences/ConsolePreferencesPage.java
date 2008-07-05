/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class ConsolePreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ConsolePreferencesPage() {
		super(GRID);
		setPreferenceStore(MercurialEclipsePlugin.getDefault().getPreferenceStore());
	}
	private ColorFieldEditor commandColorEditor;
	private ColorFieldEditor messageColorEditor;
	private ColorFieldEditor errorColorEditor;
	private BooleanFieldEditor showOnMessage;
	private BooleanFieldEditor restrictOutput;
	private BooleanFieldEditor wrap;
	private IntegerFieldEditor highWaterMark;
	private IntegerFieldEditor width;

	@Override
    protected void createFieldEditors() {
		final Composite composite = getFieldEditorParent();
		createLabel(composite, "Console preferences"); 
		IPreferenceStore store = getPreferenceStore();
		
		// ** WRAP
		wrap = new BooleanFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_WRAP, "Wrap text", composite); 
		addField(wrap);
		
		width = new IntegerFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_WIDTH, "Console width", composite); 
		width.setValidRange(80, Integer.MAX_VALUE - 1);
		addField(width);
		width.setEnabled(store.getBoolean(MercurialPreferenceConstants.PREF_CONSOLE_WRAP), composite);
		
		// ** RESTRICT OUTPUT
		restrictOutput = new BooleanFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_LIMIT_OUTPUT, "Limit output", composite); 
		addField(restrictOutput);
		
		highWaterMark = new IntegerFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_HIGH_WATER_MARK, "High water mark", composite); //)
		highWaterMark.setValidRange(1000, Integer.MAX_VALUE - 1);
		addField(highWaterMark);
		highWaterMark.setEnabled(store.getBoolean(MercurialPreferenceConstants.PREF_CONSOLE_LIMIT_OUTPUT), composite);
		
		// ** SHOW AUTOMATICALLY
		showOnMessage = new BooleanFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_SHOW_ON_MESSAGE, "Show console on message", composite); 
		addField(showOnMessage);
		
		createLabel(composite, "Console color preferences"); 
		
		//	** COLORS AND FONTS
		commandColorEditor = createColorFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_COMMAND_COLOR,
			"Command color", composite); 
		addField(commandColorEditor);
		
		messageColorEditor = createColorFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_MESSAGE_COLOR,
			"Message color", composite); 
		addField(messageColorEditor);
		
		errorColorEditor = createColorFieldEditor(MercurialPreferenceConstants.PREF_CONSOLE_ERROR_COLOR,
			"Error color", composite); 
		addField(errorColorEditor);
		
		Dialog.applyDialogFont(composite);        
	}
	
	
	@Override
    public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		highWaterMark.setEnabled(restrictOutput.getBooleanValue(), getFieldEditorParent());
		width.setEnabled(wrap.getBooleanValue(), getFieldEditorParent());
	}

	/**
	 * Utility method that creates a label instance
	 * and sets the default layout data.
	 *
	 * @param parent  the parent for the new label
	 * @param text  the text for the new label
	 * @return the new label
	 */
	private Label createLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.LEFT);
		label.setText(text);
		GridData data = new GridData();
		data.horizontalSpan = 2;
		data.horizontalAlignment = GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Creates a new color field editor.
	 */
	private ColorFieldEditor createColorFieldEditor(String preferenceName, String label, Composite parent) {
		ColorFieldEditor editor = new ColorFieldEditor(preferenceName, label, parent);
		editor.setPage(this);
		editor.setPreferenceStore(getPreferenceStore());
		return editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	@Override
    public boolean performOk() {
	    boolean ok = super.performOk(); 
		MercurialEclipsePlugin.getDefault().savePluginPreferences();
		return ok;
	}
}
