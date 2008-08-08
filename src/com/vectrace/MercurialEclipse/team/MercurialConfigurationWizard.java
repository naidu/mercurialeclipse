/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch           - make map operation asynchronous
 *******************************************************************************/

/**
 * Mercurial create Wizard
 * 
 * This wizard will dig up the project root and then 
 * it lets you modify the directory, when you are done it will 
 * create a mercurial repository
 * 
 * It will follow the dirictory chain to the bottom to se is
 * there is a .hg directory someware, is so it will suggest that you use it
 * instead of creating a new repository.
 * 
 */
package com.vectrace.MercurialEclipse.team;


import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.operations.InitOperation;

/**
 * @author zingo
 * 
 */

public class MercurialConfigurationWizard extends Wizard implements
        IConfigurationWizard {
    private class NewWizardPage extends WizardPage implements SelectionListener {
        Button changeDirButton;
        Button restoreDefaultDirButton;
        Button restoreExistingDirButton;
        boolean newMercurialProject;

        NewWizardPage(boolean newMercurialProject) {
            super(WizardPage.class.getName());
            this.newMercurialProject = newMercurialProject;
            if (newMercurialProject) {
                setTitle("Mercurial Setup wizard");
                setDescription("Put this project under mercurial version control.");
            } else {
                setTitle("Mercurial Connect to existing repository");
                setDescription("Connect to an existing mercurial version control");
            }

            // String imgKey = "icons/sample.gif";
            // setImageDescriptor( new ImageDescriptor( ( imgKey ) );
            setPageComplete(true); // Thel that it has the needed info
        }

        // interface methods of CreateRepositoryPage
        // //////////////////////////////////////////

        public void createControl(final Composite parent) {
            Composite mainControl;
            Label label;

            mainControl = new Composite(parent, SWT.NONE);
            mainControl.setLayout(new GridLayout(3, false));

            label = new Label(mainControl, SWT.CENTER);
            label.setText("Select Directory");

            directoryText = new Text(mainControl, SWT.BORDER);
            directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            directoryText.setText(hgPath);
            directoryText.addSelectionListener(this);

            changeDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
            changeDirButton.setText("Change Dir");
            changeDirButton.addSelectionListener(this);

            restoreDefaultDirButton = new Button(mainControl, SWT.CENTER
                    | SWT.PUSH);
            restoreDefaultDirButton.setText("Use Project Root");
            restoreDefaultDirButton.addSelectionListener(this);

            if (!newMercurialProject) {
                restoreExistingDirButton = new Button(mainControl, SWT.CENTER
                        | SWT.PUSH);
                restoreExistingDirButton.setText("Use Existing .hg directory");
                restoreExistingDirButton.addSelectionListener(this);
            }

            setControl(mainControl);
            setPageComplete(true);

        }

        public void widgetSelected(SelectionEvent e) {
            if (e.widget == changeDirButton) {
                DirectoryDialog directoryDialog;
                directoryDialog = new DirectoryDialog(new Shell());
                directoryDialog.setText("Select mercurial root");
                directoryDialog
                        .setMessage("Select mercurial root, should be project root.");

                hgPath = directoryDialog.open();
                directoryText.setText(hgPath);
                // directoryDialog.close();
            } else if (e.widget == restoreDefaultDirButton) {
                hgPath = hgPathOrginal;
                directoryText.setText(hgPath);
            } else if (e.widget == directoryText) {
                hgPath = directoryText.getText();
                directoryText.setText(hgPath);
            } else if ((!newMercurialProject)
                    && (e.widget == restoreExistingDirButton)) {
                hgPath = foundhgPath;
                directoryText.setText(hgPath);
            }
        }

        public void widgetDefaultSelected(SelectionEvent e) {
            // System.out.println("widgetDefaultSelected");
            if (e.widget == directoryText) {
                hgPath = directoryText.getText();
                directoryText.setText(hgPath);
            }
            // This should not happend from a button widget.
        }
    }

    private IProject project;
    private String hgPath; 
    private String hgPathOrginal;
    private String foundhgPath;
    private Text directoryText;
    private NewWizardPage page;

    public MercurialConfigurationWizard() {
        setWindowTitle("MercurialConfigurationWizard");
        setNeedsProgressMonitor(true);
    }

    // (non-Javadoc)
    // @see org.eclipse.jface.wizard.IWizard#addPages()
    //
    @Override
    public void addPages() {
        String mercurialRootDir = null;
        try {
            mercurialRootDir = MercurialUtilities.search4MercurialRoot(project);
        } catch (HgException e) {
            // do nothing
        }
        if (mercurialRootDir == null) {
            foundhgPath = null;
            hgPathOrginal = project.getLocation().toString();
            hgPath = hgPathOrginal;
            page = new NewWizardPage(true);
            addPage(page);
        } else {
            foundhgPath = mercurialRootDir;
            hgPathOrginal = mercurialRootDir;
            hgPath = hgPathOrginal;
            addPage(new NewWizardPage(false));
        }
    }

    // (non-Javadoc)
    // @see org.eclipse.jface.wizard.IWizard#performFinish()
    //
    @Override
    public boolean performFinish() {
        if (directoryText != null) {
            hgPath = directoryText.getText();
        }
        try {
            getContainer().run(true, false,
                    new InitOperation(getContainer(), project, foundhgPath,
                            hgPath));
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
            page.setErrorMessage(e.getCause().getLocalizedMessage());
            return false;
        }
        return true;
    }

    // (non-Javadoc)
    // @see
    // org.eclipse.team.ui.IConfigurationWizard#init(org.eclipse.ui.IWorkbench,
    // org.eclipse.core.resources.IProject)
    //
    public void init(IWorkbench workbench, IProject proj) {
        this.project = proj;
        if (MercurialUtilities.isHgExecutableCallable() == false) {
            MercurialUtilities.configureHgExecutable();
        }
    }

}
