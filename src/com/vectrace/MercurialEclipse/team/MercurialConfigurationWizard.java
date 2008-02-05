/*******************************************************************************
 * Copyright (c) 2008 Vectrace (Zingo Andersen) 
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;



/**
 * @author zingo
 *
 */

public class MercurialConfigurationWizard extends Wizard implements IConfigurationWizard 
{
	public class NewWizardPage extends WizardPage implements SelectionListener 
	{
		Button changeDirButton;
		Button restoreDefaultDirButton;
		Button restoreExistingDirButton;
		boolean newMercurialProject;
		
		NewWizardPage(boolean newMercurialProject) 
		{
		    super( WizardPage.class.getName() );
		    this.newMercurialProject=newMercurialProject;
		    if(newMercurialProject)
		    {
		    	setTitle( "Mercurial Setup wizard" );
		    	setDescription( "Put this project under mercurial version control." );
		    }
		    else
		    {
		    	setTitle( "Mercurial Connect to existing repository" );
		    	setDescription( "Connect to an existing mercurial version control" );
		    }
		    
		    //		    String imgKey = "icons/sample.gif";
//		    setImageDescriptor( new ImageDescriptor( ( imgKey ) );
		    setPageComplete(true); // Thel that it has the needed info
		  } 
		  
		  // interface methods of CreateRepositoryPage
		  ////////////////////////////////////////////
		  
		  public void createControl( final Composite parent ) 
		  {
			Composite mainControl;
			Label label;
			
			mainControl = new Composite(parent, SWT.NONE);
		    mainControl.setLayout( new GridLayout(3, false) );

		    label = new Label( mainControl, SWT.CENTER);
		    label.setText("Select Directory");

		    directoryText = new Text(mainControl, SWT.BORDER);
		    directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    directoryText.setText(hgPath);
		    directoryText.addSelectionListener(this);
		    
		    changeDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
		    changeDirButton.setText("Change Dir");
		    changeDirButton.addSelectionListener(this);

			restoreDefaultDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
			restoreDefaultDirButton.setText("Use Project Root");
			restoreDefaultDirButton.addSelectionListener(this);

		    if(!newMercurialProject)
		    {
		    	restoreExistingDirButton = new Button(mainControl, SWT.CENTER | SWT.PUSH);
		    	restoreExistingDirButton.setText("Use Existing .hg directory");
		    	restoreExistingDirButton.addSelectionListener(this);
		    }
		    
		    setControl(mainControl);
		    setPageComplete(true);
	  
		  }

		public void widgetSelected(SelectionEvent e) 
		{
			if(e.widget==changeDirButton )
			{
				DirectoryDialog directoryDialog;
			    directoryDialog = new DirectoryDialog( new Shell() );
			    directoryDialog.setText("Select mercurial root");
			    directoryDialog.setMessage("Select mercurial root, should be project root.");
			 
			    hgPath=directoryDialog.open();
				directoryText.setText(hgPath);
				//directoryDialog.close();
			}
			else if(e.widget == restoreDefaultDirButton)
			{
				hgPath=hgPathOrginal;
				directoryText.setText(hgPath);
			}
			else if(e.widget == directoryText)
			{
				hgPath=directoryText.getText();
				directoryText.setText(hgPath);
			}
			else if((!newMercurialProject) && (e.widget == restoreExistingDirButton))
			{
				hgPath=foundhgPath;
				directoryText.setText(hgPath);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) 
		{
			//System.out.println("widgetDefaultSelected");
			if(e.widget == directoryText)
			{
				hgPath=directoryText.getText();
				directoryText.setText(hgPath);
			}
//      	This should not happend from a button widget.
		}
	}
	
	private IProject project;
	private String hgPath; // TODO: Not sure if this is required.
	private String hgPathOrginal;
	private String foundhgPath;
//	private String pathProject;
	private Text directoryText;
	
	
	public MercurialConfigurationWizard() {
		//super();
	    setWindowTitle( "MercurialConfigurationWizard" );
//		System.out.println("MercurialConfigurationWizard.MercurialConfigurationWizard()");
	}

	// (non-Javadoc)
	// @see org.eclipse.jface.wizard.IWizard#addPages()
	//
	public void addPages() {
//		System.out.println("MercurialConfigurationWizard.addPages()");

//	    IPath projectLocation = project.getLocation();
	    String MercurialRootDir;
	    MercurialRootDir=MercurialUtilities.search4MercurialRoot(project);
	    if (MercurialRootDir == null)
	    {   
	    	foundhgPath=null;
	    	hgPathOrginal=project.getLocation().toString();
	    	hgPath=hgPathOrginal;
			addPage( new NewWizardPage(true) );
	    }
	    else
	    {
	    	foundhgPath=MercurialRootDir;
	    	hgPathOrginal=MercurialRootDir;
	    	hgPath=hgPathOrginal;
			addPage( new NewWizardPage(false) );
	    }
	}

	// (non-Javadoc)
	// @see org.eclipse.jface.wizard.IWizard#performFinish()
	//
	public boolean performFinish() {
		if(directoryText!=null)
		{
			hgPath=directoryText.getText();
		}
//		System.out.println("MercurialConfigurationWizard.preformFinish()");
//		System.out.println("Path:" + hgPath);
		if( (foundhgPath==null) ||  (! foundhgPath.equals(hgPath) ) )
		{
			String launchCmd[] = { MercurialUtilities.getHGExecutable(true), "init", hgPath };
			try 
			{
				String line;
				Process process = Runtime.getRuntime().exec(launchCmd); 
				BufferedReader input = new BufferedReader( new InputStreamReader(process.getInputStream()));
		        while ((line = input.readLine()) != null) 
		        {
//TODO output this text nice:er
		        	System.out.println(line);
				}
				input.close();
				process.waitFor();
			} 
			catch (IOException e) 
			{
				MercurialEclipsePlugin.logError(e);
//				e.printStackTrace();
				return false;
			} 
			catch (InterruptedException e) 
			{
				MercurialEclipsePlugin.logError(e);
//				e.printStackTrace();
				return false;
			}			
		}
		try 
		{
			RepositoryProvider.map( project, MercurialTeamProvider.class.getName() );			
		} 
		catch (TeamException e) 
		{
			MercurialEclipsePlugin.logError(e);
//			e.printStackTrace();
			return false;
		}
    DecoratorStatus.refresh();
		return true;
	}
	
	
	// (non-Javadoc)
	// @see org.eclipse.team.ui.IConfigurationWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.core.resources.IProject)
	//
	public void init(IWorkbench workbench, IProject project) {
		//System.out.println("MercurialConfigurationWizard.init()");
		this.project=project;
		if(MercurialUtilities.isExecutableConfigured() == false) {
			MercurialUtilities.configureExecutable();
		}
	}

}
