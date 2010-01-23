/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.io.IOException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Properties page for hg repository, which allows to change some basic properties like login
 * name/password etc
 */
public class RepoPropertiesPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {

	private static final String KEY_LNAME = "lname";
	private static final String KEY_LOCATION = "location";
	private static final String KEY_LOGIN_NAME = "name";
	private static final String KEY_LOGIN_PWD = "pwd";

	private IAdaptable adaptable;

	public RepoPropertiesPage() {
		super(GRID);
		PreferenceStore store = new PreferenceStore() {
			@Override
			public void save() throws IOException {
				internalSave();
			}
		};
		setPreferenceStore(store);
	}

	protected void internalSave() {
		if (adaptable == null) {
			return;
		}
		HgRepositoryLocation repo = (HgRepositoryLocation) adaptable
				.getAdapter(HgRepositoryLocation.class);
		if (repo == null) {
			return;
		}
		IPreferenceStore store = getPreferenceStore();
		String user = store.getString(KEY_LOGIN_NAME);
		repo.setUser(user);
		String pwd = store.getString(KEY_LOGIN_PWD);
		repo.setPassword(pwd);
		String lname = store.getString(KEY_LNAME);
		repo.setLogicalName(lname);
	}

	@Override
	protected Control createContents(Composite parent) {
		return super.createContents(parent);
	}

	@Override
	protected void createFieldEditors() {
		StringFieldEditor locationEditor = new StringFieldEditor(KEY_LOCATION, "Location",
				getFieldEditorParent());
		addField(locationEditor);
		StringFieldEditor logicalNameEditor = new StringFieldEditor(KEY_LNAME, "Logical Name",
				getFieldEditorParent());
		addField(logicalNameEditor);
		locationEditor.getTextControl(getFieldEditorParent()).setEditable(false);
		StringFieldEditor nameEditor = new StringFieldEditor(KEY_LOGIN_NAME, "Login",
				getFieldEditorParent());
		addField(nameEditor);
		StringFieldEditor pwdEditor = new StringFieldEditor(KEY_LOGIN_PWD, "Password",
				getFieldEditorParent());
		pwdEditor.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(pwdEditor);
	}

	public IAdaptable getElement() {
		return adaptable;
	}

	public void setElement(IAdaptable element) {
		this.adaptable = element;
		if (adaptable == null) {
			return;
		}
		HgRepositoryLocation repo = (HgRepositoryLocation) adaptable
				.getAdapter(HgRepositoryLocation.class);
		if (repo == null) {
			return;
		}
		IPreferenceStore store = getPreferenceStore();
		if (repo.getUser() != null) {
			store.setDefault(KEY_LOGIN_NAME, repo.getUser());
		}
		if (repo.getPassword() != null) {
			store.setDefault(KEY_LOGIN_PWD, repo.getPassword());
		}
		if (repo.getLocation() != null) {
			store.setDefault(KEY_LOCATION, repo.getLocation());
		}
		if (repo.getLogicalName() != null) {
			store.setDefault(KEY_LNAME, repo.getLogicalName());
		}
	}

}
