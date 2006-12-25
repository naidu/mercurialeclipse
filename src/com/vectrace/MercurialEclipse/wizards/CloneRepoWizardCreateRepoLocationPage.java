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

import java.util.Iterator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/*
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * This file implements a wizard page which will allow the user to create a repository
 * location.
 * 
 */


public class CloneRepoWizardCreateRepoLocationPage extends WizardPage implements IWizardPage
{

  private Label    locationLabel;
  private Combo    locationCombo;
  private GridData locationData;

  private Label    cloneParametersLabel;
  private Text     cloneParameters;
  private GridData parameterData;

  private Label    projectNameLabel;
  private Combo    projectNameCombo;
  private GridData projectNameData;

  /**
   * @param pageName
   */
  public CloneRepoWizardCreateRepoLocationPage( String pageName,
                                                String title,
                                                ImageDescriptor titleImage ) {
    super(pageName, title, titleImage);
    setDescription( "Create a repository location to clone");
  }

  public boolean canFlipToNextPage()
  {
    return isPageComplete() && (getWizard().getNextPage(this) != null);
  }

  public boolean isPageComplete()
  {
    // This page has no smarts when it comes to parsing. As far as it is concerned
    /// having any text is grounds for completion.
    return isPageComplete( locationCombo.getText(), projectNameCombo.getText());
  }
  
  private boolean isPageComplete( String url, String repoName )
  {
    return HgRepositoryLocation.validateLocation( url ) &&
           repoName.trim().length() > 0;
  }
  
  private boolean validateAndSetComplete( String url, String repoName )
  {
    boolean validLocation = isPageComplete( url, repoName );

    ((CloneRepoWizard)getWizard()).setLocationUrl(validLocation ? url : null);
    ((CloneRepoWizard)getWizard()).setProjectName(validLocation ? repoName : null);

    setPageComplete( validLocation );

    return validLocation;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent)
  {
    Composite outerContainer = new Composite(parent,SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    outerContainer.setLayout(layout);
    outerContainer.setLayoutData(
    new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    // Box to enter the repo location
    locationLabel = new Label(outerContainer, SWT.NONE);
    locationLabel.setText("Repository Location");
    locationData = new GridData();
    locationData.widthHint = 300;
    locationCombo = new Combo(outerContainer, SWT.DROP_DOWN);
    locationCombo.setLayoutData(locationData);
    locationCombo.addListener( SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        validateAndSetComplete( locationCombo.getText(), projectNameCombo.getText() );
      }      
    });
    locationCombo.addListener( SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        validateAndSetComplete( locationCombo.getText(), projectNameCombo.getText() );
      }      
    });
    // Add any previously existing URLs to the combo box for ease of use.
    Iterator locIter = MercurialEclipsePlugin.getRepoManager().getAllRepoLocations().iterator();
    while( locIter.hasNext() )
    {
      HgRepositoryLocation loc = ((HgRepositoryLocation)locIter.next());
      locationCombo.add( loc.getUrl() );
    }

    
    // Box to enter additional parameters for the clone command.
    // TODO: In the future this should go away and be replaced with something
    //       more graphical including, probably, a repo browser.
    cloneParametersLabel = new Label(outerContainer, SWT.NONE);
    cloneParametersLabel.setText("Clone command additional parameters");
    cloneParameters = new Text(outerContainer, SWT.BORDER);
    parameterData = new GridData();
    parameterData.widthHint = 300;
    cloneParameters.setLayoutData(parameterData);
    cloneParameters.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        // TODO: Should be interface not wizard class
        ((CloneRepoWizard)getWizard()).setCloneParameters(cloneParameters.getText());
      }     
    });

    // Box to enter additional parameters for the clone command.
    // TODO: In the future this should have some population smarts. Not sure what that might be right now.
    projectNameLabel = new Label(outerContainer, SWT.NONE);
    projectNameLabel.setText("Name of project to create");
    projectNameCombo = new Combo(outerContainer, SWT.DROP_DOWN);
    projectNameData = new GridData();
    projectNameData.widthHint = 300;
    projectNameCombo.setLayoutData(projectNameData);
    projectNameCombo.addListener( SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        validateAndSetComplete( locationCombo.getText(), projectNameCombo.getText() );
      }      
    });
    projectNameCombo.addListener( SWT.Modify, new Listener() {
      public void handleEvent(Event event) {
        validateAndSetComplete( locationCombo.getText(), projectNameCombo.getText() );
      }      
    });

    setControl(outerContainer);

    setPageComplete(false);
  }

  public void dispose()
  {
    locationLabel.dispose();
    locationCombo.dispose();

    cloneParametersLabel.dispose();

    projectNameLabel.dispose();
    projectNameCombo.dispose();
  }
}
