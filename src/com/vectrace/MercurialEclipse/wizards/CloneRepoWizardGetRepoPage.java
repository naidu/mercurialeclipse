/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/*
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * This file implements a wizard page which will have the user choose a
 * repository location or opt to create one. The creation of a repository
 * location, if desired, is left to another wizard page.
 * 
 */

public class CloneRepoWizardGetRepoPage extends WizardPage implements IWizardPage {
	private Button useExistingRepoLocButton;
	private Button createNewRepoLocButton;

	public CloneRepoWizardGetRepoPage( String pageName,
                                     String title,
                                     ImageDescriptor titleImage ) {
		super(pageName, title, titleImage);
		setDescription("Clone a repository into the workspace.");
    setPageComplete(false);
  }

  public boolean canFlipToNextPage()
  {
    // There is a next page if creating a new repo location.
    // TODO: This should be implemented by wizard.
    return (createNewRepoLocButton != null) &&
           (createNewRepoLocButton.getSelection()); 
  }
  
  public boolean isPageComplete()
  {
    // Finish may be invoked if an existing repo location is used.
    // TODO: Extend to requiring a repo to be chosen from a table.
    // TODO: This should be implemented by wizard.
    return (useExistingRepoLocButton != null) &&
            useExistingRepoLocButton.getSelection();
  }

  public void createControl(Composite parent) {
    Composite outerContainer = new Composite(parent,SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    outerContainer.setLayout(layout);
    outerContainer.setLayoutData(
    new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
    
    createNewRepoLocButton = new Button(outerContainer, SWT.RADIO);
    createNewRepoLocButton.setText("Create new repository location");
    
    useExistingRepoLocButton = new Button(outerContainer, SWT.RADIO);
    useExistingRepoLocButton.setText("Use existing repository location");

    createNewRepoLocButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        if (createNewRepoLocButton.getSelection()) {
          setPageComplete(true);
        } else {
          setPageComplete(false);
        }
      }
    });

    setControl(outerContainer);

    // This will trigger a setPageComplete call to set the next button appropriately.
    createNewRepoLocButton.setSelection(true); 
    useExistingRepoLocButton.setSelection(false);
  }

}
