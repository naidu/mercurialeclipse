/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/*
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * This class implements the import wizard extension and the new wizard
 * extension.
 * 
 */

public abstract class SyncRepoWizard extends Wizard implements IImportWizard, INewWizard
{
  
  SyncRepoPage syncRepoLocationPage;
  
  String locationUrl;
  String parameters;
  String projectName;
  
  public SyncRepoWizard() {
    super();
//    System.out.println( "new SyncRepoWizard()");
    setNeedsProgressMonitor(true);
  }

  @Override
public boolean canFinish()
  {
    return (locationUrl != null) && (projectName != null);
  }

  // TODO: This should become part of an interface
  public void setLocationUrl( String url )
  {
    this.locationUrl = url;
  }
  
  // TODO: This should become part of an interface
  public void setParameters( String parms )
  {
    this.parameters = parms;
  }
  
  // TODO: This should become part of an interface
  public void setProjectName( String projectName )
  {
    this.projectName = projectName;
  }

   
  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
   */
  public void init(IWorkbench workbench, IStructuredSelection selection) 
  {
    setWindowTitle(Messages.getString("ImportWizard.WizardTitle")); //$NON-NLS-1$
    setNeedsProgressMonitor(true);
    syncRepoLocationPage = new SyncRepoPage(true,Messages.getString("SyncRepoWizard.syncRepoLocationPage.name"),Messages.getString("SyncRepoWizard.syncRepoLocationPage.title"),Messages.getString("SyncRepoWizard.syncRepoLocationPage.description"),null,null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }
  
  
  @Override
public void dispose()
  {
    syncRepoLocationPage.dispose();
    
    super.dispose();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.wizard.IWizard#addPages()
   */
  @Override
public void addPages()
  {
    super.addPages();
    addPage(syncRepoLocationPage);
  }
}
