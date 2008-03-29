/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Indra Talip               - Browser dir dialog
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/*
 * This file implements a wizard page which will allow the user to create a repository
 * location.
 * 
 */


public class ImportPage extends SyncRepoPage
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


  String repoName;
  /**
   * @param pageName
   */
  public ImportPage(String pageName, String title, String description,String repoName, ImageDescriptor titleImage ) 
  {
    super(pageName, title, titleImage);
    this.repoName=repoName;
    setDescription( description);
  }

  public boolean canFlipToNextPage()
  {
    return isPageComplete() && (getWizard().getNextPage(this) != null);
  }

  public boolean isPageComplete()
  {
    // This page has no smarts when it comes to parsing. As far as it is concerned
    /// having any text is grounds for completion.
    return ( locationCombo.getText() != null );
  }
  
  private boolean isPageComplete( String file )
  {
    if(file != null)
    {
       return true;
    }
    else
    {
      return false;
    }
  }

  private boolean validateAndSetComplete( String file )
  {
    boolean validLocation = isPageComplete( file );

    ((SyncRepoWizard)getWizard()).setLocationUrl(validLocation ? file : null);

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
    layout.numColumns = 3;
    outerContainer.setLayout(layout);
    outerContainer.setLayoutData(
    new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    // Box to enter the repo location
    locationLabel = new Label(outerContainer, SWT.NONE);
    locationLabel.setText("Patch file:");
    locationData = new GridData();
    locationData.widthHint = 300;
    locationCombo = new Combo(outerContainer, SWT.DROP_DOWN);
    locationCombo.setLayoutData(locationData);
    locationCombo.addListener( SWT.Selection, new Listener() 
    {
      public void handleEvent(Event event) 
      {
        validateAndSetComplete( locationCombo.getText());          
      }      
    });
    locationCombo.addListener( SWT.Modify, new Listener() 
    {
      public void handleEvent(Event event) 
      {
        validateAndSetComplete( locationCombo.getText());          
      }      
    });
	Button browseButton = new Button (outerContainer, SWT.PUSH);
	browseButton.setText ("Browse...");
	browseButton.addSelectionListener(new SelectionAdapter() 
	{
		public void widgetSelected(SelectionEvent e) 
		{
			FileDialog dialog = new FileDialog (getShell());
      dialog.setText("Select a file to import from");     
			String file = dialog.open();
			if (file != null)
				locationCombo.setText(file);
		}
	});

  projectNameLabel = new Label(outerContainer, SWT.NONE);
  projectNameLabel.setText("Name of project to Import to:" + repoName);
  setControl(outerContainer);
  setPageComplete(false);
  }

  public void dispose()
  {
    locationLabel.dispose();
    locationCombo.dispose();
    if(cloneParametersLabel != null)
    {
      cloneParametersLabel.dispose();
    }

    if(projectNameLabel != null)
    {
      projectNameLabel.dispose();
    }
    if(projectNameCombo != null)
    {
      projectNameCombo.dispose();
    }
  }
}
