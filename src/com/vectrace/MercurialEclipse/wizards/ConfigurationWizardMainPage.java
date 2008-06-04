/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptions&additions
 ******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/**
 * Wizard page for entering information about a Hg repository location. This
 * wizard can be initialized using setProperties or using setDialogSettings
 */
public class ConfigurationWizardMainPage extends HgWizardPage {
    protected boolean showCredentials = false;
    protected boolean showBundleButton = false;

    // Widgets

    // User
    protected Combo userCombo;
    // Password
    protected Text passwordText;

    // url of the repository we want to add
    protected Combo urlCombo;

    // local repositories button
    protected Button browseButton;

    // bundles
    protected Button browseFileButton;

    private static final int COMBO_HISTORY_LENGTH = 10;

    // Dialog store id constants
    private static final String STORE_USERNAME_ID = "ConfigurationWizardMainPage.STORE_USERNAME_ID"; //$NON-NLS-1$
    private static final String STORE_URL_ID = "ConfigurationWizardMainPage.STORE_URL_ID"; //$NON-NLS-1$

    /**
     * ConfigurationWizardMainPage constructor.
     * 
     * @param pageName
     *            the name of the page
     * @param title
     *            the title of the page
     * @param titleImage
     *            the image for the page
     */
    public ConfigurationWizardMainPage(String pageName, String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    /**
     * Adds an entry to a history, while taking care of duplicate history items
     * and excessively long histories. The assumption is made that all histories
     * should be of length
     * <code>ConfigurationWizardMainPage.COMBO_HISTORY_LENGTH</code>.
     * 
     * @param history
     *            the current history
     * @param newEntry
     *            the entry to add to the history
     * @param limitHistory
     *            number of max entries, -1 if no limit
     * @return the history with the new entry appended
     */
    private String[] addToHistory(String[] history, String newEntry,
            int limitHistory) {
        ArrayList<String> l = new ArrayList<String>();
        if (history != null) {
            l.addAll(Arrays.asList(history));
        }

        l.remove(newEntry);
        l.add(0, newEntry);

        // since only one new item was added, we can be over the limit
        // by at most one item
        if (l.size() > COMBO_HISTORY_LENGTH && limitHistory > 0) {
            l.remove(COMBO_HISTORY_LENGTH);
        }

        String[] r = new String[l.size()];
        l.toArray(r);
        return r;
    }

    /**
     * Creates the UI part of the page.
     * 
     * @param parent
     *            the parent of the created widgets
     */
    public void createControl(Composite parent) {
        Composite composite = createComposite(parent, 1);

        Listener listener = new Listener() {
            public void handleEvent(Event event) {
                validateFields();
            }
        };

        createUrlControl(composite, listener);

        if (showCredentials) {
            createAuthenticationControl(composite, listener);
        }

        initializeValues();
        validateFields();
        urlCombo.setFocus();

        setControl(composite);
    }

    

    /**
     * @param composite
     * @param listener
     */
    private void createUrlControl(Composite composite, Listener listener) {
        Composite urlComposite = createComposite(composite, 4);

        Group g = createGroup(urlComposite, Messages
                .getString("ConfigurationWizardMainPage.urlGroup.title"), 4, //$NON-NLS-1$
                GridData.FILL_HORIZONTAL); //$NON-NLS-1$

        // repository Url
        createLabel(g, Messages
                .getString("ConfigurationWizardMainPage.urlLabel.text")); //$NON-NLS-1$
        urlCombo = createEditableCombo(g);
        urlCombo.addListener(SWT.Selection, listener);
        urlCombo.addListener(SWT.Modify, listener);

        browseButton = createPushButton(g, Messages
                .getString("ConfigurationWizardMainPage.browseButton.text"), 1); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                dialog
                        .setMessage(Messages
                                .getString("ConfigurationWizardMainPage.dialog.message")); //$NON-NLS-1$
                String dir = dialog.open();
                if (dir != null) {                    
//                    urlCombo.setText(new File(dir).toURI().toASCIIString());
                    getUrlCombo().setText(dir);
                }
            }
        });

        if (showBundleButton) {
            browseFileButton = createPushButton(g, Messages
                    .getString("PullPage.browseFileButton.text"), 1);//$NON-NLS-1$

            browseFileButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    FileDialog dialog = new FileDialog(getShell());
                    dialog.setText(Messages
                            .getString("PullPage.bundleDialog.text")); //$NON-NLS-1$
                    String file = dialog.open();
                    if (file != null) {
//                        getUrlCombo().setText(new File(file).toURI().toASCIIString());
                        getUrlCombo().setText(file);
                    }
                }
            });
        }

        urlCombo.addModifyListener(new ModifyListener() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
             */
            public void modifyText(ModifyEvent e) {
                setPageComplete(true);
            }
        });
    }

    /**
     * @param composite
     * @param listener
     */
    private void createAuthenticationControl(Composite composite,
            Listener listener) {
        Group g;
        Composite authComposite = createComposite(composite, 2);
        g = createGroup(
                authComposite,
                Messages
                        .getString("ConfigurationWizardMainPage.authenticationGroup.title")); //$NON-NLS-1$

        // User name
        createLabel(g, Messages
                .getString("ConfigurationWizardMainPage.userLabel.text")); //$NON-NLS-1$
        userCombo = createEditableCombo(g);
        userCombo.addListener(SWT.Selection, listener);
        userCombo.addListener(SWT.Modify, listener);

        // Password
        createLabel(g, Messages
                .getString("ConfigurationWizardMainPage.passwordLabel.text")); //$NON-NLS-1$
        passwordText = createTextField(g);
        passwordText.setEchoChar('*');
    }

    /*
     * private void setDefaultLocation(ComboViewer locations) { try {
     * HgRepositoryLocation defaultLocation = null; Map<String,
     * HgRepositoryLocation> paths = HgPathsClient
     * .getPaths(resource.getProject()); if
     * (paths.containsKey(HgPathsClient.DEFAULT_PULL)) { defaultLocation =
     * paths.get(HgPathsClient.DEFAULT_PULL); } else if
     * (paths.containsKey(HgPathsClient.DEFAULT)) { defaultLocation =
     * paths.get(HgPathsClient.DEFAULT); } if (defaultLocation != null) {
     * locations.add(defaultLocation); locations .setSelection(new
     * StructuredSelection(defaultLocation)); } } catch (HgException e) {
     * MercurialEclipsePlugin.logError(e); } }
     */

    /**
     * Utility method to create an editable combo box
     * 
     * @param parent
     *            the parent of the combo box
     * @return the created combo
     */
    protected Combo createEditableCombo(Composite parent) {
        Combo combo = new Combo(parent, SWT.NULL);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = IDialogConstants.ENTRY_FIELD_WIDTH;
        combo.setLayoutData(data);
        return combo;
    }

    /**
     * @see HgWizardPage#finish
     */
    @Override
    public boolean finish(IProgressMonitor monitor) {
        // Set the result to be the current values
        Properties result = new Properties();
        if (showCredentials) {
            result.setProperty("user", userCombo.getText()); //$NON-NLS-1$
            result.setProperty("password", passwordText.getText()); //$NON-NLS-1$
        }
        result.setProperty("url", urlCombo.getText()); //$NON-NLS-1$
        this.properties = result;

        saveWidgetValues();

        return true;
    }

    /**
     * Initializes states of the controls.
     */
    private void initializeValues() {
        // Set remembered values
        IDialogSettings setts = getDialogSettings();
        if (setts != null) {
            String[] hostNames = setts.getArray(STORE_URL_ID);
            hostNames = updateHostNames(hostNames);
            if (hostNames != null) {
                for (int i = 0; i < hostNames.length; i++) {
                    urlCombo.add(hostNames[i]);
                }
            }
            if (showCredentials) {
                String[] userNames = setts.getArray(STORE_USERNAME_ID);
                if (userNames != null) {
                    for (int i = 0; i < userNames.length; i++) {
                        userCombo.add(userNames[i]);
                    }
                }
            }
        }

        if (properties != null) {
            if (showCredentials) {
                String user = properties.getProperty("user"); //$NON-NLS-1$
                if (user != null) {
                    userCombo.setText(user);
                }

                String password = properties.getProperty("password"); //$NON-NLS-1$
                if (password != null) {
                    passwordText.setText(password);
                }
            }
            String host = properties.getProperty("url"); //$NON-NLS-1$
            if (host != null) {
                urlCombo.setText(host);
            }
        }
    }

    /**
     * Saves the widget values for the next time
     */
    private void saveWidgetValues() {
        // Update history
        IDialogSettings dialogSettings = getDialogSettings();
        String[] hostNames = null;
        hostNames = updateHostNames(hostNames);
        if (settings != null) {
            if (showCredentials) {
                String[] userNames = dialogSettings.getArray(STORE_USERNAME_ID);
                if (userNames == null) {
                    userNames = new String[0];
                }
                userNames = addToHistory(userNames, userCombo.getText(),
                        COMBO_HISTORY_LENGTH);
                dialogSettings.put(STORE_USERNAME_ID, userNames);
            }
            hostNames = dialogSettings.getArray(STORE_URL_ID);
            hostNames = addToHistory(hostNames, urlCombo.getText(), -1);
            dialogSettings.put(STORE_URL_ID, hostNames);
        }
    }

    /**
     * @param hostNames
     * @return
     */
    private String[] updateHostNames(String[] hostNames) {
        String[] newHostNames = hostNames;
        Set<HgRepositoryLocation> repositories = MercurialEclipsePlugin
                .getRepoManager().getAllRepoLocations();
        if (repositories != null) {
            int i = 0;
            for (Iterator<HgRepositoryLocation> iterator = repositories
                    .iterator(); iterator.hasNext(); i++) {
                HgRepositoryLocation hgRepositoryLocation = iterator.next();
                newHostNames = addToHistory(newHostNames, hgRepositoryLocation
                        .getUrl(), -1);
            }
        }
        return newHostNames;
    }

    /**
     * Validates the contents of the editable fields and set page completion and
     * error messages appropriately. Call each time url or username is modified
     */
    private void validateFields() {
        // first check the url of the repository
        String url = urlCombo.getText();                
        
        if (url.length() == 0) {
            setErrorMessage(null);
            setPageComplete(false);
            return;
        }
        setErrorMessage(null);
        setPageComplete(true);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            if (urlCombo != null) {
                urlCombo.setFocus();
            }
        }
    }

    @Override
    public boolean canFlipToNextPage() {
        return super.canFlipToNextPage();
    }

    /**
     * @return the showCredentials
     */
    public boolean isShowCredentials() {
        return showCredentials;
    }

    /**
     * @param showCredentials
     *            the showCredentials to set
     */
    public void setShowCredentials(boolean showCredentials) {
        this.showCredentials = showCredentials;
    }

    /**
     * @return the userCombo
     */
    public Combo getUserCombo() {
        return userCombo;
    }

    /**
     * @param userCombo
     *            the userCombo to set
     */
    public void setUserCombo(Combo userCombo) {
        this.userCombo = userCombo;
    }

    /**
     * @return the passwordText
     */
    public Text getPasswordText() {
        return passwordText;
    }

    /**
     * @param passwordText
     *            the passwordText to set
     */
    public void setPasswordText(Text passwordText) {
        this.passwordText = passwordText;
    }

    /**
     * @return the urlCombo
     */
    public Combo getUrlCombo() {
        return urlCombo;
    }

    /**
     * @param urlCombo
     *            the urlCombo to set
     */
    public void setUrlCombo(Combo urlCombo) {
        this.urlCombo = urlCombo;
    }

    /**
     * @return the showBundleButton
     */
    public boolean isShowBundleButton() {
        return showBundleButton;
    }

    /**
     * @param showBundleButton
     *            the showBundleButton to set
     */
    public void setShowBundleButton(boolean showBundleButton) {
        this.showBundleButton = showBundleButton;
    }

}
