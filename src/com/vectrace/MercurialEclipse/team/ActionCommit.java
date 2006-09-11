/**
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;


/**
 * @author zingo
 *
 */
public class ActionCommit implements IWorkbenchWindowActionDelegate
{

  
	private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
	public ActionCommit() {
		super();
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {

	}


	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
//		System.out.println("ActionCommit:init(window)");
		this.window = window;
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	

	public void run(IAction action) {
		IProject proj;
		String Repository;
    InputDialog commitDialog;
    Shell shell;
    final Shell CommitWindow;
    final Button Ok,Cancel; 
    final Text CommitTextBox;
    IWorkbench workbench;
    final boolean [] ButtonOk = new boolean [1];
    final String [] commitText = new String [1];
    
    
		proj=MercurialUtilities.getProject(selection);
		Repository=MercurialUtilities.getRepositoryPath(proj);
		if(Repository==null)
		{
			Repository="."; //never leave this empty add a . to point to current path
		}
		//Setup and run command
    
  if((window !=null) && (window.getShell() != null))
  {
    shell=window.getShell();
  }
  else
  {
    workbench = PlatformUI.getWorkbench();
    shell = workbench.getActiveWorkbenchWindow().getShell();
  }

  Display display=shell.getDisplay();
  
//  CommitWindow= new Shell(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL); 
  CommitWindow= new Shell(shell, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL); 
  FormLayout formLayout = new FormLayout();
  CommitWindow.setLayout( formLayout );
//  GridLayout gridLayout = new GridLayout(3,false);
//  CommitWindow.setLayout( gridLayout );
  CommitWindow.setText("Mercurial Eclipse Commit");
  CommitWindow.setMinimumSize(200,130);
  CommitWindow.setSize(300,130);
  Label textBoxLabel=new Label(CommitWindow , SWT.NONE);
  textBoxLabel.setText("Enter Commit message");

//  GridData gridDataLabel = new GridData(GridData.FILL_BOTH);
//  gridDataLabel.horizontalSpan = 3;
//  gridDataLabel.verticalSpan = 1;
//  textBoxLabel.setLayoutData( gridDataLabel );
  
  CommitTextBox = new Text(CommitWindow,SWT.MULTI | SWT.BORDER );
  CommitTextBox.setCapture(true);   
//  CommitTextBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
//  GridData gridDataTextBox = new GridData(GridData.FILL_BOTH);
//  gridDataTextBox.horizontalSpan = 3;
//  gridDataTextBox.verticalSpan = 3;
//  gridDataTextBox.minimumWidth=100;
//  gridDataTextBox.minimumHeight=30;
//  gridDataTextBox.widthHint=300;
//  gridDataTextBox.heightHint=50;
//  CommitTextBox.setLayoutData( gridDataTextBox );
  
  Ok = new Button(CommitWindow,SWT.PUSH);
  Ok.setText("Ok");
  Cancel = new Button(CommitWindow,SWT.PUSH);
  Cancel.setText("Cancel"); 
  
//  FormData formLabel = new FormData();
  FormData formTextBox = new FormData(100,30);
  FormData formOk = new FormData();
  FormData formCancel = new FormData();
  
//  formLabel.top = new FormAttachment(0,0);
//  formLabel.left = new FormAttachment(0,0);
//  formLabel.right = new FormAttachment(0,0);
//  formLabel.bottom = new FormAttachment(0,0);
//  textBoxLabel.setLayoutData(formLabel);
  
  formTextBox.top = new FormAttachment(textBoxLabel,8);
  formTextBox.left = new FormAttachment(textBoxLabel,3,SWT.LEFT);
  formTextBox.right = new FormAttachment(100,-3);
  formTextBox.bottom = new FormAttachment(100,-30);    //this should be Cancle_size_y+3 
  //formTextBox.bottom = new FormAttachment(Cancel,-3,SWT.TOP);
  CommitTextBox.setLayoutData(formTextBox);
  
  formOk.top = new FormAttachment(CommitTextBox,0,SWT.BOTTOM);
  formOk.right = new FormAttachment(Cancel,-8,SWT.LEFT);
//  formOk.bottom = new FormAttachment(100,-3);
  Ok.setLayoutData(formOk);
  
  formCancel.top = new FormAttachment(Ok,0,SWT.TOP);
  formCancel.right = new FormAttachment(CommitTextBox,0,SWT.RIGHT);
//  formCancel.bottom = new FormAttachment(100,-3);
  Cancel.setLayoutData(formCancel);
  
  Listener buttonListener = new Listener () 
  {
    public void handleEvent (Event event) 
    {
      ButtonOk[0] = event.widget == Ok;
      commitText[0]=CommitTextBox.getText();  
      CommitWindow.close();

    }
  };
  Ok.addListener (SWT.Selection, buttonListener);
  Cancel.addListener (SWT.Selection, buttonListener);

  CommitWindow.setDefaultButton(Ok);
  CommitWindow.pack();
  CommitWindow.open();

  while( !CommitWindow.isDisposed() ) 
  {
    if( !display.readAndDispatch() ) 
    {
      display.sleep ();
    }
  }
  
//  CommitWindow.dispose();
  
  
  //commitDialog = new InputDialog(shell,"Mercurial Eclipse Commit","Enter commit message",null,null);
  //commitDialog.open();
//  if(commitDialog.getValue() != null) 
//  {
//    String launchCmd[] = { MercurialUtilities.getHGExecutable(),"--cwd", Repository ,"commit", "--message",commitDialog.getValue(), "--user",MercurialUtilities.getHGUsername()};

 //System.out.println("Commit:" + commitText[0] );

  
  if( ButtonOk[0] == true && commitText[0] != null) 
  { //OK wa pressed and not Cancel
    //System.out.println("InputDialog: <OK> " + commitDialog.getValue());
    String launchCmd[] = { MercurialUtilities.getHGExecutable(),"--cwd", Repository ,"commit", "--message",commitText[0], "--user",MercurialUtilities.getHGUsername()};
    String output = MercurialUtilities.ExecuteCommand(launchCmd,false);
    if(output!=null)
    {
      //output output in a window
      if(output.length()!=0)
      {
        MessageDialog.openInformation(shell,"Mercurial Eclipse Commit output",  output);
      }
    }

  }
  
    
    
  DecoratorStatus.refresh();
	}
	
  
	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection in_selection) 
	{
		if( in_selection != null && in_selection instanceof IStructuredSelection )
		{
			selection = ( IStructuredSelection )in_selection;
		}
	}


	
}
