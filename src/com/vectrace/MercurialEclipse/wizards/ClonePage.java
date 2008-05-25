/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

/**
 * @author bastian
 * 
 */
public class ClonePage extends PushPullPage {

    private Text directoryTextField;
    private Button directoryButton;
    private Text cloneNameTextField;
    private Button noUpdateCheckBox;
    private Button pullCheckBox;
    private Button uncompressedCheckBox;
    private Text revisionTextField;

    public ClonePage(IResource resource, String pageName, String title,
            ImageDescriptor titleImage) {
        super(resource, pageName, title, titleImage);
        this.resource = resource;
        setShowBundleButton(false);
        setShowRevisionTable(false);
        setShowForce(false);
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        Composite composite = (Composite) getControl();
        createOptionsGroup(composite);
        createDestGroup(composite);
                
    }

    /**
     * @param composite
     */
    private void createOptionsGroup(Composite composite) {
        Group g = optionGroup;        
        
        this.noUpdateCheckBox = createCheckBox(g,
                Messages.getString("ClonePage.noUpdateCheckBox.title")); //$NON-NLS-1$
        this.pullCheckBox = createCheckBox(g,
                Messages.getString("ClonePage.pullCheckBox.title")); //$NON-NLS-1$
        this.uncompressedCheckBox = createCheckBox(g,
                Messages.getString("ClonePage.uncompressedCheckBox.title")); //$NON-NLS-1$
        createLabel(g, Messages.getString("ClonePage.revisionLabel.title")); //$NON-NLS-1$
        this.revisionTextField = createTextField(g);
    }
    
    /* (non-Javadoc)
     * @see com.vectrace.MercurialEclipse.wizards.PushPullPage#getTimeoutCheckBoxLabel()
     */
    @Override
    protected String getTimeoutCheckBoxLabel() {
        return Messages.getString("ClonePage.timeoutCheckBox.title"); //$NON-NLS-1$
    }

    /**
     * @param composite
     */
    private void createDestGroup(Composite composite) {
        Group g = createGroup(composite, Messages.getString("ClonePage.destinationGroup.title"), 3, GridData.FILL_HORIZONTAL); //$NON-NLS-1$
        createLabel(g, Messages.getString("ClonePage.destinationDirectoryLabel.title")); //$NON-NLS-1$
        this.directoryTextField = createTextField(g);
        this.directoryButton = createPushButton(g, Messages.getString("ClonePage.directoryButton.title"), 1); //$NON-NLS-1$
        this.directoryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog.setMessage(Messages.getString("ClonePage.directoryDialog.message")); //$NON-NLS-1$
                String dir = dialog.open();
                if (dir != null) {
                    directoryTextField.setText(dir);
                }
            }
        });
        createLabel(g, Messages.getString("ClonePage.cloneDirectoryLabel.title")); //$NON-NLS-1$
        this.cloneNameTextField = createTextField(g);
        g.moveAbove(optionGroup);
    }

    @Override
    public boolean finish(IProgressMonitor monitor) {
        return super.finish(monitor);
    }

    /**
     * @return the directoryButton
     */
    public Button getDirectoryButton() {
        return directoryButton;
    }

    /**
     * @return the cloneNameTextField
     */
    public Text getCloneNameTextField() {
        return cloneNameTextField;
    }

    /**
     * @return the noUpdateCheckBox
     */
    public Button getNoUpdateCheckBox() {
        return noUpdateCheckBox;
    }

    /**
     * @return the pullCheckBox
     */
    public Button getPullCheckBox() {
        return pullCheckBox;
    }

    /**
     * @return the uncompressedCheckBox
     */
    public Button getUncompressedCheckBox() {
        return uncompressedCheckBox;
    }

    /**
     * @return the revisionTextField
     */
    public Text getRevisionTextField() {
        return revisionTextField;
    }

    /**
     * @return the directoryTextField
     */
    public Text getDirectoryTextField() {
        return directoryTextField;
    }

}
