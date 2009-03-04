/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven                    - implementation 
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.LocationChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;

/*
 * A wizard page which will allow the user to choose location to export patch.
 * 
 */

public class ExportPage extends HgWizardPage implements Listener {

    protected final List<IResource> resources;

    private HgRoot root;
    private CommitFilesChooser commitFiles;

    private LocationChooser locationChooser;

    public ExportPage(List<IResource> resources, HgRoot root) {
        super(Messages.getString("ExportWizard.pageName"), Messages //$NON-NLS-1$
                .getString("ExportWizard.pageTitle"), null); // TODO icon //$NON-NLS-1$
        this.resources = resources;
        this.root = root;
    }

    protected boolean validatePage() {
        String msg = locationChooser.validate();
        if (msg == null && getCheckedResources().size() == 0) {
            msg = Messages.getString("ExportWizard.InvalidPathFile"); //$NON-NLS-1$
        }
        if (msg == null) {
            setMessage(null);
        }
        setErrorMessage(msg);
        setPageComplete(msg == null);
        return msg == null;
    }

    public void createControl(Composite parent) {
        Composite composite = SWTWidgetHelper.createComposite(parent, 1);
        // TODO help

        Group group = SWTWidgetHelper.createGroup(composite, Messages
                .getString("ExportWizard.PathLocation")); //$NON-NLS-1$
        locationChooser = new LocationChooser(group, true, getDialogSettings());
        locationChooser.addStateListener(this);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalAlignment = SWT.FILL;
        locationChooser.setLayoutData(data);

        // TODO no diff for untracked files, bug?
        commitFiles = new CommitFilesChooser(null, composite, true, resources,
                root,
                false);
        commitFiles.setLayoutData(new GridData(GridData.FILL_BOTH));
        commitFiles.addStateListener(this);

        setControl(composite);
        validatePage();
    }

    public ArrayList<IResource> getCheckedResources() {
        return commitFiles.getCheckedResources();
    }

    public void handleEvent(Event event) {
        validatePage();
    }

    public Location getLocation() {
        return locationChooser.getCheckedLocation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vectrace.MercurialEclipse.wizards.HgWizardPage#finish(org.eclipse
     * .core.runtime.IProgressMonitor)
     */
    @Override
    public boolean finish(IProgressMonitor monitor) {
        locationChooser.saveSettings();
        return super.finish(monitor);
    }
}
