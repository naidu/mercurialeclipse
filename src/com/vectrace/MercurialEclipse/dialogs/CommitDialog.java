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
package com.vectrace.MercurialEclipse.dialogs;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.actions.StatusContainerAction;


/**
 * @author @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * A commit dialog box allowing choosing of what files to commit and a 
 * commit message for those files. Untracked files may also be chosen.
 *
 */
public class CommitDialog extends Dialog
{
  private static String FILE_MODIFIED = "Modified";
  private static String FILE_ADDED = "Added";
  private static String FILE_REMOVED = "Removed";
  private static String FILE_UNTRACKED = "Untracked";
  private static String FILE_DELETED = "Already Deleted";

  private static String DEFAULT_COMMIT_MESSAGE = "(no commit message)";
  
  private class UntrackedFilesFilter extends ViewerFilter
  {

    public UntrackedFilesFilter()
    {
      super();
    }

    /**
     * Filter out untracked files.
     */
    public boolean select(Viewer viewer, Object parentElement, Object element)
    {
      if (element instanceof CommitResource)
      {
        String str = ((CommitResource)element).getStatus();
        return str.startsWith(FILE_UNTRACKED) != true;
      }

      return true;
    }
  }

  private class CommittableFilesFilter extends ViewerFilter
  {

    public CommittableFilesFilter()
    {
      super();
    }

    /**
     * Filter out un commitable files (i.e. ! -> deleted but still tracked)
     */
    public boolean select(Viewer viewer, Object parentElement, Object element)
    {
      if (element instanceof CommitResource)
      {
        String str = ((CommitResource)element).getStatus();
        return str.startsWith(FILE_DELETED) != true;
      }

      return true;
    }
  }

  private class CommitResource
  {
    private String status;
    private String path;
    
    private String convertStatus(String path)
    {
      if(path.startsWith("M"))
      {
        return FILE_MODIFIED;
      }
      else if(path.startsWith("A"))
      {
        return FILE_ADDED;
      }
      else if(path.startsWith("R"))
      {
        return FILE_REMOVED;
      }
      else if(path.startsWith("?"))
      {
        return FILE_UNTRACKED;
      }
      else if(path.startsWith("!"))
      {
        return FILE_DELETED;
      }
      else
      {
        return "status error: " + path;
      }
    }

    public CommitResource(String status, String path)
    {
      this.status = convertStatus(status);
      this.path   = path;
    }
    
    public String getStatus()
    {
      return status;
    }
    
    public String getPath()
    {
      return path;
    }
  }
  
  public class AdaptableCommitList implements IAdaptable, IWorkbenchAdapter
  {
    private CommitResource[] resources;

    public AdaptableCommitList(CommitResource[] resources)
    {
      this.resources = resources;
    }

    public Object[] getChildren(Object o)
    {
      return resources;
    }

    public ImageDescriptor getImageDescriptor(Object object)
    {
      return null;
    }

    public String getLabel(Object o)
    {
      return o == null ? "" : o.toString();//$NON-NLS-1$
    }

    public Object getParent(Object o)
    {
      return null;
    }

    public Object getAdapter(Class adapter)
    {
      if (adapter == IWorkbenchAdapter.class) return this;
      return null;
    }    
  }
  
  private Text commitTextBox;
  private Label commitTextLabel;

  private Label commitFilesLabel;
  private CheckboxTableViewer commitFilesList;

  private Button showUntrackedFilesButton;
  private Label showUntrackedFilesLabel;
  
  private Button selectAllButton;
  private Label  selectAllLabel;

  private UntrackedFilesFilter untrackedFilesFilter;
  private CommittableFilesFilter committableFilesFilter;
  
  private IProject project;
  
  private CommitResource[] commitResources;
  
  private String[] filesToAdd;
  private String[] filesToCommit;
  private String  commitMessage;
  
  private MouseListener commitMouseListener;
  private KeyListener commitKeyListener;

  /**
   * @param shell
   */
  public CommitDialog(Shell shell, IProject project)
  {
    super(shell);
    
    this.project = project;
    this.untrackedFilesFilter = new UntrackedFilesFilter();
    this.committableFilesFilter = new CommittableFilesFilter();
  }
  
  public String[] getFilesToCommit()
  {
    return filesToCommit;
  }
  
  public String getCommitMessage()
  {
    return commitMessage;
  }
  
  public String[] getFilesToAdd()
  {
    return filesToAdd;
  }

  protected Control createDialogArea(Composite parent)
  {
    Composite superComposite = (Composite)super.createDialogArea(parent);
    Shell shell = getShell();
    shell.setText("Commit files to Mercurial Repository");
    
    Composite outerContainer = new Composite(parent, SWT.NONE);
    outerContainer.setLayout(new GridLayout(1, false));
    outerContainer.setLayoutData(
        new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

    commitTextLabel = new Label(outerContainer, SWT.NONE);
    commitTextLabel.setText("Commit comments:");
    
    GridData commitTextData = new GridData();
    commitTextData.widthHint = 300;
    commitTextData.heightHint = 150;
    commitTextBox = new Text(outerContainer, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
    commitTextBox.setLayoutData(commitTextData);
    
    // Provide a default "no comment" message that is automatically cleared
    // by either typing something in the box or by clicking the mouse in the box.
    commitTextBox.setCapture(true);
    setupDefaultCommitMessage();

    createFilesList(outerContainer);
    
    // Checkboxes have a separate layout.
    Composite checkBoxContainer = new Composite(outerContainer, SWT.NONE);
    checkBoxContainer.setLayout(new GridLayout(2, false));

    showUntrackedFilesButton = new Button(checkBoxContainer, SWT.CHECK);
    showUntrackedFilesLabel  = new Label(checkBoxContainer,SWT.HORIZONTAL);
    showUntrackedFilesLabel.setText("Show untracked files.");

    showUntrackedFilesButton.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e)
        {
          if(showUntrackedFilesButton.getSelection())
          {
            commitFilesList.removeFilter(untrackedFilesFilter);
          }
          else
          {
            commitFilesList.addFilter(untrackedFilesFilter);
          }
          commitFilesList.refresh(true);
        }
      }
    );
    showUntrackedFilesButton.setSelection(true); // Start selected.
    
    selectAllButton = new Button(checkBoxContainer, SWT.CHECK );
    selectAllLabel  = new Label(checkBoxContainer, SWT.HORIZONTAL);
    selectAllLabel.setText("Check/Uncheck all.");
    selectAllButton.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e)
        {
          if(selectAllButton.getSelection())
          {
            commitFilesList.setAllChecked(true);
          }
          else
          {         
            commitFilesList.setAllChecked(false);
          }
        }
      } 
    );
    selectAllButton.setSelection(true); //Start selected 
    
    return superComposite;
  }
  
  private void setupDefaultCommitMessage()
  {
    commitTextBox.setText(DEFAULT_COMMIT_MESSAGE);
    commitMouseListener = new MouseListener() {

      public void mouseDown(MouseEvent e)
      {
        // On the first mouse down in the area clear the default commit message.
        commitTextBox.setText("");
        commitTextBox.removeMouseListener(commitMouseListener);
        commitTextBox.removeKeyListener(commitKeyListener);
      }

      public void mouseDoubleClick(MouseEvent e)
      {
        // Nothing
      }
      public void mouseUp(MouseEvent e)
      {
        // Nothing
      }      
    };

    commitKeyListener = new KeyListener()
    {

      public void keyPressed(KeyEvent e)
      {
        // On the first key press, deleted the default commit message and
        // then remove the handlers.
        commitTextBox.setText("");
        commitTextBox.removeMouseListener(commitMouseListener);
        commitTextBox.removeKeyListener(commitKeyListener);
      }

      public void keyReleased(KeyEvent e)
      {
        // Nothing
      }
    };
    commitTextBox.addMouseListener(commitMouseListener);
    commitTextBox.addKeyListener(commitKeyListener);
  }

  private void createFilesList(Composite container)
  {
    commitFilesLabel = new Label(container, SWT.NONE);
    commitFilesLabel.setText("Select files:");

    Table table = new Table(container, SWT.H_SCROLL | SWT.V_SCROLL | 
                                       SWT.FULL_SELECTION | SWT.MULTI | 
                                       SWT.CHECK | SWT.BORDER);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    TableLayout layout = new TableLayout();    

    TableColumn col;

    // Check mark
    col = new TableColumn(table, SWT.NONE | SWT.BORDER);
    col.setResizable(false);
    col.setText("");
    layout.addColumnData(new ColumnPixelData(20, false));

    // File status
    col = new TableColumn(table, SWT.NONE);
    col.setResizable(false);
    col.setText("File");
    layout.addColumnData(new ColumnPixelData(220, true));
    
    // File name
    col = new TableColumn(table, SWT.NONE);
    col.setResizable(true);
    col.setText("Status");
    layout.addColumnData(new ColumnPixelData(100, true));

    table.setLayout(layout);
    commitFilesList = new CheckboxTableViewer(table);

    GridData commitFilesData = new GridData(GridData.FILL_BOTH);
    commitFilesData.widthHint = 280;
    commitFilesData.heightHint = 300;
    commitFilesList.getTable().setLayoutData(commitFilesData);

    commitFilesList.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return commitResources;
      }
      public void dispose() {
      }
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }     
    });

    commitFilesList.setLabelProvider(new ITableLabelProvider() {

      public Image getColumnImage(Object element, int columnIndex)
      {
        // No images.
        return null;
      }

      public String getColumnText(Object element, int columnIndex)
      {
        if((element instanceof CommitResource) != true)
        {
          return "Type Error";
        }
        CommitResource resource = (CommitResource)element;
        
        if(columnIndex == 0)
        {
          return "";
        }
        else if(columnIndex == 1)
        {
          return resource.getPath();
        }
        else if(columnIndex == 2)
        {
          return resource.getStatus();
        }
        
        return "Col Error";
      }

      public void addListener(ILabelProviderListener listener)
      {
      }

      public void dispose()
      {
      }

      public boolean isLabelProperty(Object element, String property)
      {
        return false;
      }

      public void removeListener(ILabelProviderListener listener)
      {
      }
    
    });

    commitFilesList.setInput(new AdaptableCommitList(fillFileList()));
    commitFilesList.addFilter(committableFilesFilter);
    commitFilesList.setAllChecked(true);
  }
  
  private CommitResource[] fillFileList()
  {
    // Get the path to the project go we can get everything underneath
    // that has changed. Once we get that, filter on the appropriate
    // items.
    IResource[] projectArray = {project};
    StatusContainerAction statusAction = 
      new StatusContainerAction(null, projectArray);

    try
    {
      statusAction.run();
      String result = statusAction.getResult();

      return spliceList(result);
    } catch (Exception e)
    {
      System.out.println("Unable to get status " + e.getMessage());
      return null;
    }
  }
  
  private CommitResource[] spliceList(String string)
  {
    //System.out.println("Changed resources: " + string);

    ArrayList list = new ArrayList();
    StringTokenizer st = new StringTokenizer(string);
    
    // Tokens are always in pairs as lines are in the form "A TEST_FOLDER\test_file2.c"
    // where the first token is the status and the 2nd is the path relative to the project.
    while(st.hasMoreTokens())
    {
      String str = st.nextToken();
      list.add(new CommitResource(str,st.nextToken()));
    }
    
    commitResources = (CommitResource[])list.toArray(new CommitResource[0]);
    return commitResources;
  }
  
  private String[] convertToFiles(Object[] objs)
  {
    ArrayList list = new ArrayList();

    for(int res=0; res < objs.length; res++)
    {
      if(objs[res] instanceof CommitResource != true)
      {
        return null;
      }

      CommitResource resource = (CommitResource)objs[res];
      list.add(resource.getPath());
    }
    
    return (String[])list.toArray(new String[0]);
  }
  
  private String[] getToAddList(Object[] objs)
  {
    ArrayList list = new ArrayList();

    for(int res=0; res < objs.length; res++)
    {
      if(objs[res] instanceof CommitResource != true)
      {
        return null;
      }

      CommitResource resource = (CommitResource)objs[res];
      if(resource.getStatus() == CommitDialog.FILE_UNTRACKED)
      {
        list.add(resource.getPath());
      }
    }
    
    return (String[])list.toArray(new String[0]);
  }
  
  /**
   * Override the OK button pressed to capture the info we want first
   * and then call super.
   */
  protected void okPressed() {
    filesToAdd    = getToAddList(commitFilesList.getCheckedElements());
    filesToCommit = convertToFiles(commitFilesList.getCheckedElements());
    commitMessage = commitTextBox.getText();

    super.okPressed();
  }
}
