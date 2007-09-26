package com.vectrace.MercurialEclipse.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.swt.SWT;
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
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;

import com.vectrace.MercurialEclipse.team.ActionDiff;


public class RevertDialog extends Dialog {

    private Table table;

    private List<CommitResource> resources;

    private List<CommitResource> selection;

    private CheckboxTableViewer selectFilesList;

    private Button selectAllButton;

    private Button deselectAllButton;

    /**
     * Create the dialog
     * 
     * @param parentShell
     */
    public RevertDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    /**
     * Create contents of the dialog
     * 
     * @param parent
     */
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new FormLayout());

        final Label label = new Label(container, SWT.NONE);
        final FormData fd_label = new FormData();
        fd_label.top = new FormAttachment(0, 10);
        fd_label.left = new FormAttachment(0, 5);
        label.setLayoutData(fd_label);
        label.setText("Checked resources will be reverted");

        createFilesList(container);
        final FormData fd_table = new FormData();
        fd_table.right = new FormAttachment(100, -5);
        fd_table.top = new FormAttachment(0, 35);
        fd_table.left = new FormAttachment(label, 0, SWT.LEFT);
        table.setLayoutData(fd_table);

        selectAllButton = new Button(container, SWT.NONE);
        fd_table.bottom = new FormAttachment(selectAllButton, -5, SWT.TOP);
        final FormData fd_selectAllButton = new FormData();
        fd_selectAllButton.bottom = new FormAttachment(100, -3);
        fd_selectAllButton.right = new FormAttachment(0, 90);
        fd_selectAllButton.left = new FormAttachment(0, 5);
        selectAllButton.setLayoutData(fd_selectAllButton);
        selectAllButton.setText("Select All");

        deselectAllButton = new Button(container, SWT.NONE);
        final FormData fd_deselectAllButton = new FormData();
        fd_deselectAllButton.left = new FormAttachment(selectAllButton, 10,
                SWT.DEFAULT);
        fd_deselectAllButton.bottom = new FormAttachment(100, -3);
        fd_deselectAllButton.right = new FormAttachment(0, 185);
        deselectAllButton.setLayoutData(fd_deselectAllButton);
        deselectAllButton.setText("Deselect All");
        //

        makeActions();
        return container;
    }

    private void createFilesList(Composite container) {
        table = new Table(container, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI | SWT.CHECK | SWT.BORDER);
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
        selectFilesList = new CheckboxTableViewer(table);

        selectFilesList.setContentProvider(new ArrayContentProvider());

        selectFilesList.setLabelProvider(new CommitResourceLabelProvider());
        selectFilesList.addFilter(new UntrackedFilesFilter());
        selectFilesList.setInput(resources);
        
        selectFilesList.setAllChecked(true);
    }

    private void makeActions() {
        
        deselectAllButton.addSelectionListener(new SelectionAdapter() {
           @Override
            public void widgetSelected(SelectionEvent e) {
                selectFilesList.setAllChecked(false);
            } 
        });
        
        selectAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
             public void widgetSelected(SelectionEvent e) {
                 selectFilesList.setAllChecked(true);
             } 
         });
        
        selectFilesList.addDoubleClickListener(new IDoubleClickListener() 
        {
          public void doubleClick(DoubleClickEvent event) 
          {
            IStructuredSelection sel = (IStructuredSelection) selectFilesList.getSelection();
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

    }

    /**
     * Create contents of the button bar
     * 
     * @param parent
     */
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
                true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    /**
     * Return the initial size of the dialog
     */
    protected Point getInitialSize() {
        return new Point(500, 375);
    }

    public void setFiles(List<CommitResource> resources) {
        this.resources = resources;

    }

    protected void okPressed() {
        this.selection = new ArrayList(Arrays.asList(selectFilesList.getCheckedElements()));
        super.okPressed();

    }

    public List<CommitResource> getSelection() {
        return selection;
    }

    public void setFiles(CommitResource[] commitResources) {
        setFiles(Arrays.asList(commitResources));
    }

}
