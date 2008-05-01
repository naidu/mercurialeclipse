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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
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

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/*
 * This file implements a wizard page which will allow the user to create a repository
 * location.
 * 
 */


public class PullPage extends SyncRepoPage
{

  private Label    locationLabel;
  private Combo    locationCombo;
  private GridData locationData;

  private Label    cloneParametersLabel;
  private Label    projectNameLabel;
  private Combo    projectNameCombo;
  private IProject project;
  String repoName;
  /**
   * @param pageName
   */
  public PullPage( String pageName, String title, String description,IProject project, ImageDescriptor titleImage ) 
  {
    super(pageName, title, titleImage);
    this.repoName = project.getName();
    this.project = project;
    setDescription( description);
  }

  public PullPage( String pageName, String title, ImageDescriptor titleImage ) 
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
    return HgRepositoryLocation.validateLocation( locationCombo.getText() );
  }
  
  @Override
protected boolean isPageComplete(String url) {
        return HgRepositoryLocation.validateLocation(url);
    }

    @Override
    protected boolean validateAndSetComplete(String url) {
        boolean validLocation = isPageComplete(url);

        ((SyncRepoWizard) getWizard()).setLocationUrl(validLocation ? url
                : null);

        setPageComplete(validLocation);

        return validLocation;
    }

  
  /* (non-Javadoc)
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
public void createControl(Composite parent)
  {
    Composite outerContainer = new Composite(parent,SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 4;
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
    // Add any previously existing URLs to the combo box for ease of use.
    Iterator<HgRepositoryLocation> locIter = MercurialEclipsePlugin.getRepoManager().getAllRepoLocations().iterator();
    while( locIter.hasNext() )
    {
      HgRepositoryLocation loc = locIter.next();
      locationCombo.add( loc.getUrl() );
    }
    setDefaultLocation(locationCombo);
    
	Button browseButton = new Button (outerContainer, SWT.PUSH);
	browseButton.setText ("Browse repos");
	browseButton.addSelectionListener(new SelectionAdapter() 
	{
		@Override
        public void widgetSelected(SelectionEvent e) 
		{
			DirectoryDialog dialog = new DirectoryDialog (getShell());
      dialog.setMessage("Select a repository to pull/push");     
			String dir = dialog.open();
			if (dir != null) {
                locationCombo.setText(dir);
            }
			}
	 });
  
    Button browsefileButton = new Button (outerContainer, SWT.PUSH);
    browsefileButton.setText ("Browse bundles");
    browsefileButton.addSelectionListener(new SelectionAdapter() 
    {
      @Override
    public void widgetSelected(SelectionEvent e) 
      {
        FileDialog dialog = new FileDialog (getShell());
        dialog.setText("Select a bundle to pull/push from/to");     
        String dir = dialog.open();
        if (dir != null) {
            locationCombo.setText(dir);
        }
      }
    });
 
    projectNameLabel = new Label(outerContainer, SWT.NONE);
    projectNameLabel.setText("Name of project to pull to:" + repoName);
    
    // Dummy labels because of bad choice of GridLayout
    new Label(outerContainer, SWT.NONE);
    new Label(outerContainer, SWT.NONE);
    new Label(outerContainer, SWT.NONE);
    
    // toggle wether the wizard should perform a update or not on finish
    final Button toggleUpdate = new Button(outerContainer,SWT.CHECK);
    toggleUpdate.addSelectionListener(new SelectionAdapter() {
        @Override
         public void widgetSelected(SelectionEvent e) {
            PullRepoWizard container = (PullRepoWizard) getWizard();
            container.setDoUpdate(toggleUpdate.getSelection());
         } 
    });
    toggleUpdate.setText("Update After Pull");

    setControl(outerContainer);
    setPageComplete(false);
  }

  private void setDefaultLocation(Combo locations) {
      try {
            String defaultUrl = null;
            Map<String, HgRepositoryLocation> paths = HgPathsClient
                    .getPaths(project);
            if (paths.containsKey(HgPathsClient.DEFAULT_PULL)) {
                defaultUrl = paths.get(HgPathsClient.DEFAULT_PULL).getUrl();
            } else if (paths.containsKey(HgPathsClient.DEFAULT)) {
                defaultUrl = paths.get(HgPathsClient.DEFAULT).getUrl();
            }
            if (defaultUrl != null) {
                locations.add(defaultUrl);
                locations.select(locations.indexOf(defaultUrl));
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
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
