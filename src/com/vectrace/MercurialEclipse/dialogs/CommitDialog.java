/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc. 
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Edited by Zingo Andersen
 * 
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

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.core.Team;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.actions.StatusContainerAction;

/**
 * @author
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 * 
 * A commit dialog box allowing choosing of what files to commit and a commit
 * message for those files. Untracked files may also be chosen.
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

  private static final class CommitResourceLabelProvider extends LabelProvider implements ITableLabelProvider 
  {
    public Image getColumnImage(Object element, int columnIndex) 
    {
      // No images.
      return null;
    }

    public String getColumnText(Object element, int columnIndex) 
    {
      if ((element instanceof CommitResource) != true) 
      {
        return "Type Error";
      }
      CommitResource resource = (CommitResource) element;

      if (columnIndex == 0) 
      {
        return "";
      } 
      else if (columnIndex == 1) 
      {
        return resource.getPath().toString();
      } 
      else if (columnIndex == 2) 
      {
        return resource.getStatus();
      }
      return "Col Error";
    }
  }

  private class UntrackedFilesFilter extends ViewerFilter 
  {
    public UntrackedFilesFilter() 
    {
      super();
    }

    /**
     * Filter out untracked files.
     */
    public boolean select(Viewer viewer, Object parentElement,Object element) 
    {
      if (element instanceof CommitResource) 
      {
        String str = ((CommitResource) element).getStatus();
        return str.startsWith(FILE_UNTRACKED) != true;
      }
      return true;
    }
  }

  class CommittableFilesFilter extends ViewerFilter 
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
      /* TODO should this be deleted from repository also?? */
      if (element instanceof CommitResource) 
      {
        String str = ((CommitResource) element).getStatus();
        return str.startsWith(FILE_DELETED) != true;
      }
      return true;
    }
  }

  private class CommitResource 
  {
    private String status;
    private File path;
    private IResource resource;
    private String convertStatus(String statusToken) 
    {
      if (statusToken.startsWith("M")) 
      {
        // System.out.println("FILE_MODIFIED:path <" +
        // statusToken.toString() + ">");
        return FILE_MODIFIED;
      } 
      else if (statusToken.startsWith("A")) 
      {
        return FILE_ADDED;
      } 
      else if (statusToken.startsWith("R")) 
      {
        return FILE_REMOVED;
      } 
      else if (statusToken.startsWith("?")) 
      {
        return FILE_UNTRACKED;
      }
      else if (statusToken.startsWith("!")) 
      {
        // System.out.println("FILE_DELETED:path <" +
        // statusToken.toString() + ">");
        return FILE_DELETED;
      }
      else 
      {
        return "status error: " + statusToken.toString();
      }
    }

    public CommitResource(String status, IResource resource, File path) 
    {
      this.status = convertStatus(status);
      this.resource = resource;
      this.path = path;
    }

    public String getStatus() 
    {
      return status;
    }

    public IResource getResource() 
    {
      return resource;
    }

    public File getPath() 
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
      if (adapter == IWorkbenchAdapter.class)
      {
        return this;
      }
      return null;
    }
  }

  private Text commitTextBox;
  private Label commitTextLabel;
  private Label commitFilesLabel;
  private CheckboxTableViewer commitFilesList;
  private Button showUntrackedFilesButton;
  private Button selectAllButton;
  private UntrackedFilesFilter untrackedFilesFilter;
  private CommittableFilesFilter committableFilesFilter;
  private IProject project;
  private IResource[] inResources;
  private CommitResource[] commitResources;
  private File[] filesToAdd;
  private File[] filesToCommit;
  private IResource[] resourcesToCommit;
  private String commitMessage;
  private MouseListener commitMouseListener;
  private KeyListener commitKeyListener;

  /**
   * @param shell
   */
  public CommitDialog(Shell shell, IProject project, IResource[] inResources) 
  {
    super(shell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.project = project;
    this.inResources = inResources;
    this.untrackedFilesFilter = new UntrackedFilesFilter();
    this.committableFilesFilter = new CommittableFilesFilter();
  }

  public String getCommitMessage() 
  {
    return commitMessage;
  }

  public File[] getFilesToCommit() 
  {
    return filesToCommit;
  }

  public IResource[] getResourcesToCommit() 
  {
    return resourcesToCommit;
  }

  public File[] getFilesToAdd() 
  {
    return filesToAdd;
  }

  protected Control createDialogArea(Composite parent) 
  {
    Composite container = (Composite) super.createDialogArea(parent);
    container.setLayout(new FormLayout());

    commitTextLabel = new Label(container, SWT.NONE);
    final FormData fd_commitTextLabel = new FormData();
    fd_commitTextLabel.bottom = new FormAttachment(0, 33);
    fd_commitTextLabel.top = new FormAttachment(0, 20);
    fd_commitTextLabel.right = new FormAttachment(0, 95);
    fd_commitTextLabel.left = new FormAttachment(0, 9);
    commitTextLabel.setLayoutData(fd_commitTextLabel);
    commitTextLabel.setText("Commit comments");

    commitTextBox = new Text(container, SWT.V_SCROLL | SWT.MULTI | SWT.BORDER | SWT.WRAP);
    final FormData fd_commitTextBox = new FormData();
    fd_commitTextBox.right = new FormAttachment(100, -9);
    fd_commitTextBox.top = new FormAttachment(0, 40);
    fd_commitTextBox.left = new FormAttachment(0, 9);
    commitTextBox.setLayoutData(fd_commitTextBox);

    commitFilesLabel = new Label(container, SWT.NONE);
    fd_commitTextBox.bottom = new FormAttachment(commitFilesLabel, -7, SWT.DEFAULT);
    final FormData fd_commitFilesLabel = new FormData();
    fd_commitFilesLabel.right = new FormAttachment(0, 66);
    fd_commitFilesLabel.left = new FormAttachment(0, 9);
    commitFilesLabel.setLayoutData(fd_commitFilesLabel);
    commitFilesLabel.setText("Select Files:");

    commitFilesList = createFilesList(container);
    Table table = commitFilesList.getTable();
    fd_commitFilesLabel.bottom = new FormAttachment(table, -7, SWT.DEFAULT);
    final FormData fd_table = new FormData();
    fd_table.bottom = new FormAttachment(100, -57);
    fd_table.right = new FormAttachment(100, -9);
    fd_table.top = new FormAttachment(0, 189);
    fd_table.left = new FormAttachment(0, 9);
    table.setLayoutData(fd_table);

    selectAllButton = new Button(container, SWT.CHECK);
    final FormData fd_selectAllButton = new FormData();
    fd_selectAllButton.bottom = new FormAttachment(100, -11);
    fd_selectAllButton.right = new FormAttachment(0, 115);
    fd_selectAllButton.left = new FormAttachment(0, 9);
    selectAllButton.setLayoutData(fd_selectAllButton);
    selectAllButton.setText("Check/uncheck all");

    showUntrackedFilesButton = new Button(container, SWT.CHECK);
    final FormData fd_showUntrackedFilesButton = new FormData();
    fd_showUntrackedFilesButton.bottom = new FormAttachment(100, -34);
    fd_showUntrackedFilesButton.right = new FormAttachment(0, 129);
    fd_showUntrackedFilesButton.left = new FormAttachment(0, 9);
    showUntrackedFilesButton.setLayoutData(fd_showUntrackedFilesButton);
    showUntrackedFilesButton.setText("Show untracked files");

    makeActions();
    return container;
  }

  private void makeActions() 
  {
    commitTextBox.setCapture(true);
    selectAllButton.setSelection(true); // Start selected
    showUntrackedFilesButton.setSelection(true); // Start selected.

    showUntrackedFilesButton.addSelectionListener(new SelectionAdapter() 
      {
        public void widgetSelected(SelectionEvent e) 
        {
          if (showUntrackedFilesButton.getSelection()) 
          {
              commitFilesList.removeFilter(untrackedFilesFilter);
          } 
          else 
          {
              commitFilesList.addFilter(untrackedFilesFilter);
          }
          commitFilesList.refresh(true);
        }
      });
    selectAllButton.addSelectionListener(new SelectionAdapter() 
      {
        public void widgetSelected(SelectionEvent e) 
        {
          if (selectAllButton.getSelection()) 
          {
              commitFilesList.setAllChecked(true);
          } 
          else 
          {
              commitFilesList.setAllChecked(false);
          }
        }
      });       
    setupDefaultCommitMessage();
  }

  private void setupDefaultCommitMessage() 
  {
    commitTextBox.setText(DEFAULT_COMMIT_MESSAGE);
    commitMouseListener = new MouseListener() 
      {

        public void mouseDown(MouseEvent e) 
        {
          // On the first mouse down in the area clear the default commit
          // message.
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
          // On the first key press, deleted the default commit message
          // and
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

  private CheckboxTableViewer createFilesList(Composite container) 
  {
    Table table = new Table(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK | SWT.BORDER);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    TableLayout layout = new TableLayout();

    TableColumn col;

    // Check mark
    col = new TableColumn(table, SWT.NONE | SWT.BORDER);
    col.setResizable(false);
    col.setText("");
    layout.addColumnData(new ColumnPixelData(20, false));

    // File name
    col = new TableColumn(table, SWT.NONE);
    col.setResizable(true);
    col.setText("File");
    layout.addColumnData(new ColumnPixelData(220, true));

    // File status
    col = new TableColumn(table, SWT.NONE);
    col.setResizable(true);
    col.setText("Status");
    layout.addColumnData(new ColumnPixelData(100, true));

    table.setLayout(layout);
    commitFilesList = new CheckboxTableViewer(table);

    commitFilesList.setContentProvider(new ArrayContentProvider());

    commitFilesList.setLabelProvider(new CommitResourceLabelProvider());

    commitFilesList.setInput(fillFileList());
    commitFilesList.addFilter(committableFilesFilter);
    // commitFilesList.removeFilter(untrackedFilesFilter);

    commitFilesList.setAllChecked(true);
    // commitFilesList.setAllChecked(false);
    // commitFilesList.refresh(true);
    return commitFilesList;
  }

  private CommitResource[] fillFileList() 
  {
    // Get the path to the project go we can get everything underneath
    // that has changed. Once we get that, filter on the appropriate
    // items.
    // IResource[] projectArray = {project};
    // StatusContainerAction statusAction = new StatusContainerAction(null,
    // projectArray);
    StatusContainerAction statusAction = new StatusContainerAction(null, inResources);
    File workingDir = statusAction.getWorkingDir();
    try 
    {
      statusAction.run();
      String result = statusAction.getResult();
      return spliceList(result, workingDir);
    }
    catch (Exception e) 
    {
      System.out.println("CommitDialog::fillFileList() Error:");
      System.out.println("Project:" + project.toString());
      System.out.println("Unable to get status " + e.getMessage());
      return null;
    }
  }

  /**
   * Finds if there is a IFile that matches the fileName Warning Recursive!!!
   * 
   * @param string
   * @param fileNameWithWorkingDir
   *                Use this to try to match the outpack to the IResource in
   *                the inResources array
   * @param inResource
   *                the resourse to check if it is a IFolder we to a recursive
   *                search...
   * @return matching IResource or null
   */

  private IResource findIResource(String fileName, String fileNameWithWorkingDir, IResource inResource) 
  {
    IResource thisResource = null;
    if (inResource instanceof IFile) 
    {
      IFile thisIFile = (IFile) inResource;
      // System.out.println(" IFile:" +
      // thisIFile.getLocation().toOSString());
      if (thisIFile.getLocation().toOSString().compareTo(fileNameWithWorkingDir) == 0) 
      {
        return thisIFile; // Found a match
      }
    } 
    else if (inResource instanceof IFolder) 
    {
      IFolder thisIFolder = (IFolder) inResource;
      // System.out.println(" IFolder:" +
      // thisIFolder.getLocation().toOSString());
      IResource folderResources[];
      try 
      {
        folderResources = thisIFolder.members();
        for (int res = 0; res < folderResources.length; res++) 
        {
          // Mercurial doesn't control directories or projects and so
          // will just return that they're
          // untracked.

          thisResource = findIResource(fileName, fileNameWithWorkingDir, folderResources[res]);
          if (thisResource != null) 
          {
              return thisResource; // Found a resource
          }
        }
      } 
      catch (CoreException e) 
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return thisResource;
  }

  /**
   * 
   * @param string
   * @param workingDir
   *                Use this to try to match the outpack to the IResource in
   *                the inResources array
   * @return
   */

  private CommitResource[] spliceList(String string, File workingDir) 
  {
    /*
     * System.out.println("Changed resources: ");
     * System.out.println(string); System.out.println("workingDir:" +
     * workingDir.toString()); System.out.println(" IResources:"); for(int
     * res = 0; res < inResources.length; res++) { // Mercurial doesn't
     * control directories or projects and so will just return that they're //
     * untracked. System.out.println(" <" +
     * inResources[res].getLocation().toOSString() + ">"); }
     */
    ArrayList list = new ArrayList();
    StringTokenizer st = new StringTokenizer(string);
    String status;
    String fileName;
    IResource thisResource;
    String fileNameWithWorkingDir;
    String eol = System.getProperty("line.separator");

    // Tokens are always in pairs as lines are in the form "A
    // TEST_FOLDER\test_file2.c"
    // where the first token is the status and the 2nd is the path relative
    // to the project.
    while (st.hasMoreTokens()) 
    {
      status = st.nextToken(" ");
      fileName = st.nextToken(eol);
      if (status.startsWith(eol)) 
      {
        status = status.substring(eol.length());
      }
      if (fileName.startsWith(" ")) 
      {
        fileName = fileName.substring(1);
      }
      thisResource = null;
      fileNameWithWorkingDir = workingDir + File.separator + fileName;

      for (int res = 0; res < inResources.length; res++) 
      {
        // Mercurial doesn't control directories or projects and so will
        // just return that they're
        // untracked.

        thisResource = findIResource(fileName, fileNameWithWorkingDir, inResources[res]);
        if (thisResource == null) 
        {
          continue; // Found a resource
        }
      }

      if (thisResource == null) 
      {
        // Create a resource could be a deleted file we want to commit
        IPath projPath = project.getLocation();
        // System.out.println("projPath.toOSString() <" +
        // projPath.toOSString() + ">");
        // System.out.println("fileNameWithWorkingDir <" +
        // fileNameWithWorkingDir + ">");
        if (fileNameWithWorkingDir.startsWith(projPath.toOSString())) 
        { // Relative path from Project
          String fileNameWithWorkingDirFromProject = fileNameWithWorkingDir.substring(projPath.toOSString().length());
          IFile file = project.getFile(fileNameWithWorkingDirFromProject);
          thisResource = (IResource) file;
        } 
        else 
        { // This is a full path
          IFile file = project.getFile(fileNameWithWorkingDir);
          thisResource = (IResource) file;
        }
      }
      /*
       * if(thisResource.exists()) { System.out.println(" Output <" +
       * fileName + "> Resource <" + thisResource.toString() + ">"); }
       * else { System.out.println(" Output <" + fileName + "> Resource <" +
       * thisResource.toString() + "> Fake resource!"); }
       */
      if (!Team.isIgnoredHint(thisResource)) 
      {
        list.add(new CommitResource(status, thisResource, new File(fileName)));
      }
    }

    commitResources = (CommitResource[]) list.toArray(new CommitResource[0]);
    return commitResources;
  }

  private File[] convertToFiles(Object[] objs) 
  {
    ArrayList list = new ArrayList();

    for (int res = 0; res < objs.length; res++) 
    {
      if (objs[res] instanceof CommitResource != true) 
      {
        return null;
      }

      CommitResource resource = (CommitResource) objs[res];
      list.add(resource.getPath());
    }

    return (File[]) list.toArray(new File[0]);
  }

  private IResource[] convertToResource(Object[] objs) 
  {
    ArrayList list = new ArrayList();

    for (int res = 0; res < objs.length; res++) 
    {
      if (objs[res] instanceof CommitResource != true) 
      {
        return null;
      }

      CommitResource resource = (CommitResource) objs[res];
      IResource thisResource = resource.getResource();
      if (thisResource != null) 
      {
        list.add(thisResource);
      }
    }

    return (IResource[]) list.toArray(new IResource[0]);
  }

  private File[] getToAddList(Object[] objs) 
  {
    ArrayList list = new ArrayList();

    for (int res = 0; res < objs.length; res++) 
    {
      if (objs[res] instanceof CommitResource != true) 
      {
        return null;
      }

      CommitResource resource = (CommitResource) objs[res];
      if (resource.getStatus() == CommitDialog.FILE_UNTRACKED) 
      {
        list.add(resource.getPath());
      }
    }

    return (File[]) list.toArray(new File[0]);
  }

  /**
   * Override the OK button pressed to capture the info we want first and then
   * call super.
   */
  protected void okPressed() 
  {
    filesToAdd = getToAddList(commitFilesList.getCheckedElements());
    filesToCommit = convertToFiles(commitFilesList.getCheckedElements());
    resourcesToCommit = convertToResource(commitFilesList.getCheckedElements());
    commitMessage = commitTextBox.getText();

    super.okPressed();
  }

  protected Point getInitialSize() 
  {
    return new Point(477, 562);
  }
}