/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     StefanC - many updates
 *     VecTrace (Zingo Andersen) - some updates
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;


import com.vectrace.MercurialEclipse.TableColumnSorter;
import com.vectrace.MercurialEclipse.team.ActionDiff;

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
  public static String FILE_MODIFIED = "Modified";
  public static String FILE_ADDED = "Added";
  public static String FILE_REMOVED = "Removed";
  public static String FILE_UNTRACKED = "Untracked";
  public static String FILE_DELETED = "Already Deleted";
  private static String DEFAULT_COMMIT_MESSAGE = "(no commit message)";

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

  private Text commitTextBox;
  private Label commitTextLabel;
  private Label commitFilesLabel;
  private CheckboxTableViewer commitFilesList;
  private Button showUntrackedFilesButton;
  private Button selectAllButton;
  private UntrackedFilesFilter untrackedFilesFilter;
  private CommittableFilesFilter committableFilesFilter;
  private IProject project;
//  CommitResource[] commitResources;
  private File[] filesToAdd;
  private File[] filesToCommit;
  private IResource[] resourcesToCommit;
  private String commitMessage;
  private MouseListener commitMouseListener;
  private KeyListener commitKeyListener;
private IResource[] inResources;

  /**
   * @param shell
   */
  public CommitDialog(Shell shell, IProject project, IResource[] inResources) 
  {
    super(shell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.setProject(project);
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
    commitFilesList.addDoubleClickListener(new IDoubleClickListener() 
      {
        public void doubleClick(DoubleClickEvent event) 
        {
          IStructuredSelection sel = (IStructuredSelection) commitFilesList.getSelection();
          if (sel.getFirstElement() instanceof CommitResource) 
          {
            CommitResource resource = (CommitResource) sel.getFirstElement();
            ActionDiff diff = new ActionDiff();            
            SyncInfoCompareInput compareInput = diff.getCompareInput(resource.getResource());
            if(compareInput!=null)
            {
              CompareUI.openCompareDialog(compareInput);
            }
          }
        }
      });

    setupDefaultCommitMessage();
    
    final Table table = commitFilesList.getTable();
        TableColumn[] columns = table.getColumns();
        for (int ci = 0; ci < columns.length; ci++) {
            TableColumn column = columns[ci];
            final int colIdx = ci;
            TableColumnSorter cSorter = new TableColumnSorter(commitFilesList,
                    column) {
                protected int doCompare(Viewer v, Object e1, Object e2) {
                    StructuredViewer viewer = (StructuredViewer) v;
                    ITableLabelProvider lp = ((ITableLabelProvider) viewer
                            .getLabelProvider());
                    String t1 = lp.getColumnText(e1, colIdx);
                    String t2 = lp.getColumnText(e2, colIdx);
                    return t1.compareTo(t2);
                }
            };
        }
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

    commitFilesList.setInput(new CommitResourceUtil(getProject()).getCommitResources(inResources));
    commitFilesList.addFilter(committableFilesFilter);
    // commitFilesList.removeFilter(untrackedFilesFilter);

    commitFilesList.setAllChecked(true);
    // commitFilesList.setAllChecked(false);
    // commitFilesList.refresh(true);
    return commitFilesList;
  }

  private File[] convertToFiles(Object[] objs) 
  {
    ArrayList<File> list = new ArrayList<File>();

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
    ArrayList<IResource> list = new ArrayList<IResource>();

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
    ArrayList<File> list = new ArrayList<File>();

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

  protected void setProject(IProject project) {
    this.project = project;
  }

  protected IProject getProject() {
    return project;
  }
}