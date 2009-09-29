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

import java.io.File;
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
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

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
        ArrayList<String> list = new ArrayList<String>();
        if (history != null) {
            list.addAll(Arrays.asList(history));
        }

        list.remove(newEntry);
        list.add(0, newEntry);

        // since only one new item was added, we can be over the limit
        // by at most one item
        if (list.size() > COMBO_HISTORY_LENGTH && limitHistory > 0) {
            list.remove(COMBO_HISTORY_LENGTH);
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Creates the UI part of the page.
     *
     * @param parent
     *            the parent of the created widgets
     */
    public void createControl(Composite parent) {
        Composite composite = SWTWidgetHelper.createComposite(parent, 1);

        Listener listener = new Listener() {
            public void handleEvent(Event event) {
                urlChanged();
            }
        };

        createUrlControl(composite, listener);

        if (showCredentials) {
            createAuthenticationControl(composite);
        }
        setControl(composite);
        urlCombo.setFocus();

        initializeValues();
        boolean ok = validateFields();
        setPageComplete(ok);
        if(ok) {
            setErrorMessage(null);
        }
    }

    private void createUrlControl(Composite composite, final Listener listener) {
        Composite urlComposite = SWTWidgetHelper.createComposite(composite, 4);

        Group g = SWTWidgetHelper.createGroup(urlComposite, Messages
                .getString("ConfigurationWizardMainPage.urlGroup.title"), 4, //$NON-NLS-1$
                GridData.FILL_HORIZONTAL);

        // repository Url
        SWTWidgetHelper.createLabel(g, Messages
                .getString("ConfigurationWizardMainPage.urlLabel.text")); //$NON-NLS-1$
        urlCombo = createEditableCombo(g);
        urlCombo.addListener(SWT.Modify, listener);

        browseButton = SWTWidgetHelper.createPushButton(g, Messages
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
                    dir = dir.trim();
                    // urlCombo.setText(new File(dir).toURI().toASCIIString());
                    getUrlCombo().setText(dir);
                }
            }
        });

        if (showBundleButton) {
            browseFileButton = SWTWidgetHelper.createPushButton(g, Messages
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
            public void modifyText(ModifyEvent e) {

                try {
                    // note that repo will not be null, will be blank
                    // repo if no existing one was found
                    HgRepositoryLocation repo = MercurialEclipsePlugin
                        .getRepoManager().getRepoLocation(getUrlText());

                    String user = repo.getUser();
                    if (user != null && user.length() != 0) {
                        getUserCombo().setText(user);
                    } else {
                        getUserCombo().setText("");
                    }
                    String password = repo.getPassword();
                    if (password != null && password.length() != 0) {
                        getPasswordText().setText(password);
                    } else {
                        getPasswordText().setText("");
                    }
                } catch (HgException e1) {
                    // Lookup obviously failed, but verification will
                    // pick this error up later
                    MercurialEclipsePlugin.logError(e1);
                }
            }
        });
    }

    private void createAuthenticationControl(Composite composite) {
        Group g;
        Composite authComposite = SWTWidgetHelper.createComposite(composite, 2);
        g = SWTWidgetHelper.createGroup(
                authComposite,
                Messages
                .getString("ConfigurationWizardMainPage.authenticationGroup.title")); //$NON-NLS-1$

        // User name
        SWTWidgetHelper.createLabel(g, Messages
                .getString("ConfigurationWizardMainPage.userLabel.text")); //$NON-NLS-1$
        userCombo = createEditableCombo(g);

        // Password
        SWTWidgetHelper.createLabel(g, Messages
                .getString("ConfigurationWizardMainPage.passwordLabel.text")); //$NON-NLS-1$
        passwordText = SWTWidgetHelper.createPasswordField(g);
    }

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
        properties = createProperties();

        saveWidgetValues();
        return true;
    }

    protected Properties createProperties() {
        Properties result = new Properties();
        if (showCredentials) {
            result.setProperty("user", getUserText()); //$NON-NLS-1$
            result.setProperty("password", passwordText.getText()); //$NON-NLS-1$
        }
        result.setProperty("url", getUrlText()); //$NON-NLS-1$
        return result;
    }

    /**
     * Initializes states of the controls.
     */
    private void initializeValues() {
        // Set remembered values
        IDialogSettings setts = getDialogSettings();
        if (setts != null) {
            String[] hostNames = updateHostNames();
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
        if (settings != null) {
            if (showCredentials) {
                String[] userNames = dialogSettings.getArray(STORE_USERNAME_ID);
                if (userNames == null) {
                    userNames = new String[0];
                }
                userNames = addToHistory(userNames, getUserText(),
                        COMBO_HISTORY_LENGTH);
                dialogSettings.put(STORE_USERNAME_ID, userNames);
            }
        }
    }

    private String getUserText() {
        return userCombo.getText().trim();
    }

    protected String getUrlText() {
        return urlCombo.getText().trim();
    }

    private String[] updateHostNames() {
        String[] newHostNames = new String[0];
        Set<HgRepositoryLocation> repositories = MercurialEclipsePlugin
        .getRepoManager().getAllRepoLocations();
        if (repositories != null) {
            int i = 0;
            for (Iterator<HgRepositoryLocation> iterator = repositories
                    .iterator(); iterator.hasNext(); i++) {
                HgRepositoryLocation hgRepositoryLocation = iterator.next();
                newHostNames = addToHistory(newHostNames, hgRepositoryLocation
                        .getLocation(), -1);
            }
        }
        return newHostNames;
    }

    /**
     * Validates the contents of the editable fields and set page completion and
     * error messages appropriately. Call each time url or username is modified
     */
    protected boolean validateFields() {
        // first check the url of the repository
        String url = getUrlText();

        if (url.length() == 0) {
            setErrorMessage(null);
            return false;
        }
        File localDirectory = getLocalDirectory(url);
        if(localDirectory != null){
            if(!localDirectory.exists()){
                setErrorMessage("Please provide a valid url or an existing directory!");
                return false;
            }
            File hgRepo = new File(localDirectory, ".hg");
            if(!hgRepo.isDirectory()){
                setErrorMessage("Directory " + localDirectory + " does not contain a valid hg repository!");
                return false;
            }
        }
        return true;
    }

    /**
     * @param urlString non null
     * @return true if the given url can be threated as local directory
     */
    private File getLocalDirectory(String urlString) {
        if(urlString.contains("http:") || urlString.contains("https:")
                || urlString.contains("ftp:") || urlString.contains("ssh:")){
            return null;
        }
        try {
           return new File(urlString);
        } catch (Exception e) {
            return null;
        }
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

    public boolean isShowCredentials() {
        return showCredentials;
    }

    public void setShowCredentials(boolean showCredentials) {
        this.showCredentials = showCredentials;
    }

    public Combo getUserCombo() {
        return userCombo;
    }

    public void setUserCombo(Combo userCombo) {
        this.userCombo = userCombo;
    }

    public Text getPasswordText() {
        return passwordText;
    }

    public void setPasswordText(Text passwordText) {
        this.passwordText = passwordText;
    }

    public Combo getUrlCombo() {
        return urlCombo;
    }

    public void setUrlCombo(Combo urlCombo) {
        this.urlCombo = urlCombo;
    }

    public boolean isShowBundleButton() {
        return showBundleButton;
    }

    public void setShowBundleButton(boolean showBundleButton) {
        this.showBundleButton = showBundleButton;
    }

    /**
     * Triggered if the user has changed repository url. Override to implement additional
     * checks after it.
     * @return true, if the filed validation was successful
     */
    protected boolean urlChanged() {
        boolean ok = validateFields();
        setPageComplete(ok);
        if(ok) {
            setErrorMessage(null);
        }
        return ok;
    }

}
