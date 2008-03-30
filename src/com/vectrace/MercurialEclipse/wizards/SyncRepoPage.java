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


public class SyncRepoPage extends WizardPage implements IWizardPage
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


  private boolean clone; //if true then clone if false push/pull
  String repoName;
  /**
   * @param pageName
   */
  public SyncRepoPage( boolean clone,String pageName, String title, String description,String repoName, ImageDescriptor titleImage ) 
  {
    super(pageName, title, titleImage);
    this.clone=clone;
    this.repoName=repoName;
    setDescription( description);
  }

  public SyncRepoPage( String pageName, String title, ImageDescriptor titleImage ) 
  {
    super(pageName, title, titleImage);
  }

  
  @Override
public boolean canFlipToNextPage()
  {
    return isPageComplete() && (getWizard().getNextPage(this) != null);
  }

  @Override
public boolean isPageComplete()
  {
    // This page has no smarts when it comes to parsing. As far as it is concerned
    /// having any text is grounds for completion.
    if(clone)
    {
      return isPageComplete( locationCombo.getText(), projectNameCombo.getText());
    }
    return HgRepositoryLocation.validateLocation( locationCombo.getText() );
  }
  
  private boolean isPageComplete( String url, String repoName )
  {
    return HgRepositoryLocation.validateLocation( url ) && repoName.trim().length() > 0;
  }

  private boolean isPageComplete( String url )
  {
    return HgRepositoryLocation.validateLocation( url );
  }

  
  private boolean validateAndSetComplete( String url, String repoName )
  {
    boolean validLocation = isPageComplete( url, repoName );

    ((SyncRepoWizard)getWizard()).setLocationUrl(validLocation ? url : null);
    ((SyncRepoWizard)getWizard()).setProjectName(validLocation ? repoName : null);

    setPageComplete( validLocation );

    return validLocation;
  }

  private boolean validateAndSetComplete( String url )
  {
    boolean validLocation = isPageComplete( url );

    ((SyncRepoWizard)getWizard()).setLocationUrl(validLocation ? url : null);

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
    locationLabel.setText("Repository Location:");
    locationData = new GridData();
    locationData.widthHint = 300;
    locationCombo = new Combo(outerContainer, SWT.DROP_DOWN);
    locationCombo.setLayoutData(locationData);
    locationCombo.addListener( SWT.Selection, new Listener() 
    {
      public void handleEvent(Event event) 
      {
        if(clone)
        {
          validateAndSetComplete( locationCombo.getText(), projectNameCombo.getText() );
        }
        else
        {
          validateAndSetComplete( locationCombo.getText());          
        }
      }      
    });
    locationCombo.addListener( SWT.Modify, new Listener() 
    {
      public void handleEvent(Event event) 
      {
        if(clone)
        {
          validateAndSetComplete( locationCombo.getText(), projectNameCombo.getText() );
        }
        else
        {
          validateAndSetComplete( locationCombo.getText());          
        }
      }      
    });
    // Add any previously existing URLs to the combo box for ease of use.
    Iterator locIter = MercurialEclipsePlugin.getRepoManager().getAllRepoLocations().iterator();
    while( locIter.hasNext() )
    {
      HgRepositoryLocation loc = ((HgRepositoryLocation)locIter.next());
      locationCombo.add( loc.getUrl() );
    }
	Button browseButton = new Button (outerContainer, SWT.PUSH);
	browseButton.setText ("Browse repos");
	browseButton.addSelectionListener(new SelectionAdapter() 
	{
		@Override
        public void widgetSelected(SelectionEvent e) 
		{
			DirectoryDialog dialog = new DirectoryDialog (getShell());
      if(clone)
      {
			  dialog.setMessage("Select a repository to clone");
      }
      else
      {
        dialog.setMessage("Select a repository to pull/push");     
      }
			String dir = dialog.open();
			if (dir != null) {
                locationCombo.setText(dir);
            }
		}
	});
  if(clone)
  {    
    // Box to enter additional parameters for the clone command.
    // TODO: In the future this should go away and be replaced with something
    //       more graphical including, probably, a repo browser.
    cloneParametersLabel = new Label(outerContainer, SWT.NONE);
    cloneParametersLabel.setText("Clone command additional parameters:");
    cloneParameters = new Text(outerContainer, SWT.BORDER);
    parameterData = new GridData();
    parameterData.widthHint = 285;
    parameterData.horizontalSpan = 2;
    cloneParameters.setLayoutData(parameterData);
    cloneParameters.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        // TODO: Should be interface not wizard class
        ((SyncRepoWizard)getWizard()).setParameters(cloneParameters.getText());
      }     
    });

    // Box to enter additional parameters for the clone command.
    // TODO: In the future this should have some population smarts. Not sure what that might be right now.
    projectNameLabel = new Label(outerContainer, SWT.NONE);
    projectNameLabel.setText("Name of project to create:");
    projectNameCombo = new Combo(outerContainer, SWT.DROP_DOWN);
    projectNameData = new GridData();
    projectNameData.widthHint = 300;
    projectNameData.horizontalSpan = 2;
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
  } 
  else //if(clone)
  {
    projectNameLabel = new Label(outerContainer, SWT.NONE);
    projectNameLabel.setText("Name of project to pull to:" + repoName);
  } //if(clone)
  setControl(outerContainer);
  setPageComplete(false);
  }

  @Override
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
