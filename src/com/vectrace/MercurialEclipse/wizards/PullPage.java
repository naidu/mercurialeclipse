/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * This file implements a wizard page which will allow the user to create a
 * repository location.
 */
public class PullPage extends PushPullPage {

    public Button getRebaseCheckBox() {
        return rebaseCheckBox;
    }

    private Button updateCheckBox;
    private Button mergeCheckBox;
    private Button commitDialogCheckBox;
    private Button rebaseCheckBox;

    public Button getCommitDialogCheckBox() {
        return commitDialogCheckBox;
    }

    public PullPage(String pageName, String title, String description,
            IResource resource, ImageDescriptor titleImage) {
        super(resource, pageName, title, titleImage);
        setDescription(description);
        setShowCredentials(true);
        setShowBundleButton(true);
        setShowRevisionTable(false);
    }

    @Override
    public boolean canFlipToNextPage() {
        try {
            if (getUrlCombo().getText() != null
                    && getUrlCombo().getText() != null) {
                IncomingPage incomingPage = (IncomingPage) getNextPage();
                incomingPage.setProject(resource.getProject());
                HgRepositoryLocation loc =
                    MercurialEclipsePlugin
                        .getRepoManager().getRepoLocation(
                                getUrlCombo().getText(),
                                getUserCombo().getText(),
                                getPasswordText().getText());
                incomingPage.setLocation(loc);
                incomingPage.setSvn(getSvnCheckBox() != null
                        && getSvnCheckBox().getSelection());
                setErrorMessage(null);
                return isPageComplete()
                        && (getWizard().getNextPage(this) != null);
            }
        } catch (HgException e) {
            setErrorMessage(e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean isPageComplete() {
        return super.isPageComplete()
                && HgRepositoryLocation.validateLocation(getUrlCombo()
                        .getText());
    }

    protected boolean isPageComplete(String url) {
        return HgRepositoryLocation.validateLocation(url);
    }

    protected boolean validateAndSetComplete(String url) {
        boolean validLocation = isPageComplete(url);

        setPageComplete(validLocation);

        return validLocation;
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();

        // now the options
        Group pullGroup = SWTWidgetHelper
                .createGroup(composite, Messages.getString("PullPage.pullGroup.label")); //$NON-NLS-1$
        this.updateCheckBox = SWTWidgetHelper.createCheckBox(pullGroup,
                Messages.getString("PullPage.toggleUpdate.text")); //$NON-NLS-1$
        this.updateCheckBox.setSelection(true);

        try {
            if (MercurialUtilities.isCommandAvailable("rebase", //$NON-NLS-1$
                    ResourceProperties.REBASE_AVAILABLE, "hgext.rebase=")) { //$NON-NLS-1$
                this.rebaseCheckBox = SWTWidgetHelper.createCheckBox(pullGroup,
                        Messages.getString("PullPage.option.rebase")); //$NON-NLS-1$
                SelectionListener rebaseCheckBoxListener = new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent e) {
                        widgetSelected(e);
                    }

                    public void widgetSelected(SelectionEvent e) {
                        if (rebaseCheckBox.getSelection()) {
                            updateCheckBox.setSelection(!rebaseCheckBox
                                    .getSelection());
                        }
                    }
                };
                SelectionListener updateCheckBoxListener = new SelectionListener() {
                    public void widgetDefaultSelected(SelectionEvent e) {
                        widgetSelected(e);
                    }
                    public void widgetSelected(SelectionEvent e) {
                        if (updateCheckBox.getSelection()) {
                            rebaseCheckBox.setSelection(!updateCheckBox
                                    .getSelection());
                        }
                    }
                };
                rebaseCheckBox.addSelectionListener(rebaseCheckBoxListener);
                updateCheckBox.addSelectionListener(updateCheckBoxListener);
            }
        } catch (HgException e2) {
            MercurialEclipsePlugin.logError(e2);
        }

        this.forceCheckBox.setParent(pullGroup);
        pullGroup.moveAbove(optionGroup);

        Group mergeGroup = SWTWidgetHelper.createGroup(composite,
                Messages.getString("PullPage.option.merge")); //$NON-NLS-1$
        this.mergeCheckBox = SWTWidgetHelper.createCheckBox(mergeGroup,
                Messages.getString("PullPage.option.commitAfterMerge")); //$NON-NLS-1$

        this.commitDialogCheckBox = SWTWidgetHelper.createCheckBox(mergeGroup,
                Messages.getString("PullPage.option.editCommitMessage")); //$NON-NLS-1$

        this.commitDialogCheckBox.setSelection(true);
        this.commitDialogCheckBox.setEnabled(false);

        mergeGroup.moveBelow(pullGroup);

        SelectionListener mergeCheckBoxListener = new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                commitDialogCheckBox.setEnabled(mergeCheckBox.getSelection());
                if (mergeCheckBox.getSelection()) {
                    String status;
                    try {
                        status = HgStatusClient
                                .getStatus(resource.getProject());
                        if (status.length() > 0 && status.indexOf("M ") >= 0) { //$NON-NLS-1$
                            setErrorMessage(Messages.getString("PullPage.error.modifiedResources")); //$NON-NLS-1$
                            setPageComplete(false);
                        } else {
                            setErrorMessage(null);
                            setPageComplete(true);
                        }
                    } catch (HgException e1) {
                        setErrorMessage(Messages.getString("PullPage.error.noStatus")); //$NON-NLS-1$
                        mergeCheckBox.setSelection(false);
                        mergeCheckBox.setEnabled(false);
                        setPageComplete(true);
                    }
                } else {
                    setErrorMessage(null);
                    setPageComplete(true);
                }
            }
        };

        mergeCheckBox.addSelectionListener(mergeCheckBoxListener);

        setPageComplete(true);
        setControl(composite);

    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);
    }

    @Override
    protected String getForceCheckBoxLabel() {
        return Messages.getString("PullPage.forceCheckBox.title"); //$NON-NLS-1$
    }

    @Override
    protected String getRevGroupLabel() {
        return Messages.getString("PullPage.revGroup.title"); //$NON-NLS-1$
    }

    @Override
    protected String getRevCheckBoxLabel() {
        return Messages.getString("PullPage.revCheckBox.title"); //$NON-NLS-1$
    }

    @Override
    protected String getTimeoutCheckBoxLabel() {
        return Messages.getString("PullPage.timeoutCheckBox.title"); //$NON-NLS-1$
    }

    public Button getUpdateCheckBox() {
        return updateCheckBox;
    }

    public void setUpdateCheckBox(Button updateCheckBox) {
        this.updateCheckBox = updateCheckBox;
    }

    public Button getMergeCheckBox() {
        return mergeCheckBox;
    }

}
