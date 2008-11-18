/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * @author bastian
 * 
 */
public class PushPullPage extends ConfigurationWizardMainPage {

    protected IResource resource;
    protected Button forceCheckBox;
    protected boolean force;
    protected ChangesetTable changesetTable;
    protected String revision;
    protected Button revCheckBox;
    protected Button timeoutCheckBox;
    protected boolean timeout;
    protected Group optionGroup;
    protected boolean showRevisionTable = true;
    protected boolean showForce = true;
    protected Button forestCheckBox;
    protected boolean showForest = false;
    protected Combo snapFileCombo;
    protected Button snapFileButton;
    protected boolean showSnapFile = true;
    protected boolean showSvn = false;
    protected Button svnCheckBox;

    public PushPullPage(IResource resource, String pageName, String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
        this.resource = resource;
        try {
            setShowForest(true);
            setShowSvn(true);
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            setErrorMessage(e.getMessage());
        }
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();

        // now the options
        optionGroup = SWTWidgetHelper.createGroup(composite, Messages
                .getString("PushRepoPage.optionGroup.title")); //$NON-NLS-1$
        this.timeoutCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                getTimeoutCheckBoxLabel());

        if (showForce) {
            this.forceCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                    getForceCheckBoxLabel());
        }
        if (showRevisionTable) {
            createRevisionTable(composite);
        }

        createExtensionControls();

        setDefaultLocation();
    }

    /**
     * 
     */
    private void createExtensionControls() {
        if (showForest) {
            this.forestCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                    "Repository is a forest");

            if (showSnapFile) {
                Composite c = SWTWidgetHelper.createComposite(optionGroup, 3);
                final Label forestLabel = SWTWidgetHelper.createLabel(c,
                        "Snapfile");
                forestLabel.setEnabled(false);
                this.snapFileCombo = createEditableCombo(c);
                snapFileCombo.setEnabled(false);
                this.snapFileButton = SWTWidgetHelper.createPushButton(c,
                        "Browse...", 1);
                snapFileButton.setEnabled(false);
                this.snapFileButton
                        .addSelectionListener(new SelectionAdapter() {
                            @Override
                            public void widgetSelected(SelectionEvent e) {
                                FileDialog dialog = new FileDialog(getShell());
                                dialog.setText("Please select snapfile");
                                String file = dialog.open();
                                if (file != null) {
                                    snapFileCombo.setText(file);
                                }
                            }
                        });

                SelectionListener forestCheckBoxListener = new SelectionListener() {
                    public void widgetSelected(SelectionEvent e) {
                        forestLabel.setEnabled(forestCheckBox.getSelection());
                        snapFileButton
                                .setEnabled(forestCheckBox.getSelection());
                        snapFileCombo.setEnabled(forestCheckBox.getSelection());
                    }

                    public void widgetDefaultSelected(SelectionEvent e) {
                        widgetSelected(e);
                    }
                };
                forestCheckBox.addSelectionListener(forestCheckBoxListener);
            }
        }

        if (showSvn) {
            this.svnCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                    "Use Subversion extension");            
        }
    }

    /**
     * @param composite
     */
    private void createRevisionTable(Composite composite) {
        this.revCheckBox = SWTWidgetHelper.createCheckBox(optionGroup,
                getRevCheckBoxLabel());

        Listener revCheckBoxListener = new Listener() {
            public void handleEvent(Event event) {
                // en-/disable list view
                changesetTable.setEnabled(revCheckBox.getSelection());
            }
        };

        this.revCheckBox.addListener(SWT.Selection, revCheckBoxListener);

        Group revGroup = SWTWidgetHelper.createGroup(composite,
                getRevGroupLabel(), GridData.FILL_BOTH);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 150;
        gridData.minimumHeight = 50;
        this.changesetTable = new ChangesetTable(revGroup, resource);
        this.changesetTable.setLayoutData(gridData);
        this.changesetTable.setEnabled(false);

        SelectionListener listener = new SelectionListener() {
            /*
             * (non-Javadoc)
             * 
             * @see
             * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse
             * .swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                setPageComplete(true);
                revision = changesetTable.getSelection().toString();
            }

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected
             * (org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        };

        this.changesetTable.addSelectionListener(listener);
    }

    protected void setDefaultLocation() {
        try {
            if (resource == null) {
                return;
            }
            String defaultLocation = null;
            Map<String, String> paths = HgPathsClient
                    .getPaths(resource.getProject());
            if (paths.containsKey(HgPathsClient.DEFAULT_PULL)) {
                defaultLocation = paths.get(HgPathsClient.DEFAULT_PULL);
            } else if (paths.containsKey(HgPathsClient.DEFAULT)) {
                defaultLocation = paths.get(HgPathsClient.DEFAULT);
            }
            if (defaultLocation != null) {
                getUrlCombo().setText(defaultLocation);
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    /**
     * @return
     */
    protected String getRevGroupLabel() {
        return Messages.getString("PushRepoPage.revGroup.title"); //$NON-NLS-1$
    }

    /**
     * @return
     */
    protected String getRevCheckBoxLabel() {
        return Messages.getString("PushRepoPage.revCheckBox.text");//$NON-NLS-1$
    }

    /**
     * @return
     */
    protected String getForceCheckBoxLabel() {
        return Messages.getString("PushRepoPage.forceCheckBox.text");//$NON-NLS-1$
    }

    /**
     * @return
     */
    protected String getTimeoutCheckBoxLabel() {
        return Messages.getString("PushRepoPage.timeoutCheckBox.text");//$NON-NLS-1$
    }

    /**
     * @return the force
     */
    public boolean isForce() {
        return force;
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * @return the timeout
     */
    public boolean isTimeout() {
        return timeout;
    }

    /**
     * @return the resource
     */
    public IResource getResource() {
        return resource;
    }

    /**
     * @param resource
     *            the resource to set
     */
    public void setResource(IResource resource) {
        this.resource = resource;
    }

    /**
     * @return the forceCheckBox
     */
    public Button getForceCheckBox() {
        return forceCheckBox;
    }

    /**
     * @return the changesetTable
     */
    public ChangesetTable getChangesetTable() {
        return changesetTable;
    }

    /**
     * @return the revCheckBox
     */
    public Button getRevCheckBox() {
        return revCheckBox;
    }

    /**
     * @return the timeoutCheckBox
     */
    public Button getTimeoutCheckBox() {
        return timeoutCheckBox;
    }

    /**
     * @return the showRevisionTable
     */
    public boolean isShowRevisionTable() {
        return showRevisionTable;
    }

    /**
     * @param showRevisionTable
     *            the showRevisionTable to set
     */
    public void setShowRevisionTable(boolean showRevisionTable) {
        this.showRevisionTable = showRevisionTable;
    }

    /**
     * @return the showForce
     */
    public boolean isShowForce() {
        return showForce;
    }

    /**
     * @param showForce
     *            the showForce to set
     */
    public void setShowForce(boolean showForce) {
        this.showForce = showForce;
    }

    public boolean isShowForest() {
        return showForest;
    }

    public void setShowForest(boolean showForest) throws HgException {
        this.showForest = showForest
                && MercurialUtilities.isCommandAvailable("fpull",
                        ResourceProperties.EXT_FOREST_AVAILABLE, null);
    }

    public Combo getSnapFileCombo() {
        return snapFileCombo;
    }

    public void setSnapFileCombo(Combo snapFileCombo) {
        this.snapFileCombo = snapFileCombo;
    }

    public boolean isShowSnapFile() {
        return showSnapFile;
    }

    public void setShowSnapFile(boolean showSnapFile) {
        this.showSnapFile = showSnapFile;
    }

    public Button getForestCheckBox() {
        return forestCheckBox;
    }

    public void setForestCheckBox(Button forestCheckBox) {
        this.forestCheckBox = forestCheckBox;
    }

    public boolean isShowSvn() {
        return showSvn;
    }

    public void setShowSvn(boolean showSvn) throws HgException {
        this.showSvn = showSvn
                && MercurialUtilities.isCommandAvailable("svn",
                        ResourceProperties.EXT_HGSUBVERSION_AVAILABLE, null);
    }

    public Button getSvnCheckBox() {
        return svnCheckBox;
    }

    public void setSvnCheckBox(Button svnCheckBox) {
        this.svnCheckBox = svnCheckBox;
    }

}
