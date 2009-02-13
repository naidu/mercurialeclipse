/*******************************************************************************
 * Copyright (c) 2005-2009 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PropertyPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author bastian
 * 
 */
public class HgProjectPropertyPage extends PropertyPage {
    private IProject project;
    private Group reposGroup;

    /**
     * 
     */
    public HgProjectPropertyPage() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
     * .swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        this.project = (IProject) super.getElement();
        
        // create gui elements
        Composite comp = SWTWidgetHelper.createComposite(parent, 1);

        if (!MercurialUtilities.hgIsTeamProviderFor(project, false)) {
            setMessage("This project doesn't use MercurialEclipse as Team provider.");
            return comp;
        }
        
        
        reposGroup = SWTWidgetHelper.createGroup(comp,
                "Repository paths:", 1,
                        GridData.FILL_HORIZONTAL);
        
        // each repository gets a label with its logical name and a combo for
        // setting it within MercurialEclipse

        HgRepositoryLocationManager mgr = MercurialEclipsePlugin
                .getRepoManager();
        Set<HgRepositoryLocation> repos = mgr
                .getAllProjectRepoLocations(project);
        for (HgRepositoryLocation repo : repos) {
            Composite repoComposite = SWTWidgetHelper.createComposite(
                    reposGroup, 2);
            SWTWidgetHelper.createLabel(repoComposite,
                    repo.getLogicalName() == null ? "Unnamed" : repo
                            .getLogicalName());
            Combo combo = SWTWidgetHelper.createEditableCombo(repoComposite);
            combo.add(repo.getLocation());
            combo.select(0);
        }
        return comp;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        if (!MercurialUtilities.hgIsTeamProviderFor(project, false)) {
            return super.performOk();
        }
        IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
        
        Control[] composites = reposGroup.getChildren();
        
        for (Control control : composites) {
            Composite comp = (Composite) control;
            Control[] controls = comp.getChildren();
            store.putValue("repository." + controls[0].getData(),
                    ((Combo) controls[1]).getText());
        }
        return super.performOk();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        this.performOk();
    }
}
