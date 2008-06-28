/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIMergeClient;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class MergeView extends ViewPart implements ISelectionListener {

    public final static String ID = MergeView.class.getName();

    private Label statusLabel;
    private Table table;

    private Action abortAction;

    private IProject currentProject;

    @Override
    public void createPartControl(final Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        table = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
                | SWT.H_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = 200;
        table.setLayoutData(data);

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent event) {
                try {
                    TableItem item = (TableItem) event.item;
                    FlaggedAdaptable flagged = (FlaggedAdaptable)item.getData();
                    IFile file = (IFile)flagged.getAdapter(IFile.class);
                    
                    String mergeNodeId = currentProject.getPersistentProperty(ResourceProperties.MERGING);
                    
                    String[] parents = HgParentClient.getParentNodeIds(currentProject);
                    int ancestor = HgParentClient.findCommonAncestor(currentProject, parents[0], parents[1]);
                                        
                    RevisionNode mergeNode = new RevisionNode(new IStorageMercurialRevision(file, mergeNodeId));
                    RevisionNode ancestorNode = new RevisionNode(new IStorageMercurialRevision(file, ancestor));
                    
                    HgCompareEditorInput compareInput = new HgCompareEditorInput(
                            new CompareConfiguration(),
                            file,
                            ancestorNode,
                            mergeNode,
                            true);
                    
                    CompareUI.openCompareEditor(compareInput);
                } catch (Exception e) {                    
                    MercurialEclipsePlugin.logError(e);
                    MercurialEclipsePlugin.showError(e);
                }
            }
        });

        String[] titles = { "Status", "File" };
        int[] widths = { 50, 400 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }
        
        createToolBar();
        getSite().getPage().addSelectionListener(this);
    }

    private void createToolBar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        abortAction = new Action("Abort") {
            @Override
            public void run() {
                try {
                    currentProject.setPersistentProperty(
                            ResourceProperties.MERGING, null);
                    HgUpdateClient.update(currentProject, null, true);
                    currentProject.refreshLocal(IResource.DEPTH_INFINITE, null);
                    clearView();
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }
        };
        abortAction.setEnabled(false);
        mgr.add(abortAction);
    }

    private void populateView() throws HgException {
        statusLabel.setText(currentProject.getName());

        List<FlaggedAdaptable> status = HgIMergeClient
                .getMergeStatus(currentProject);
        table.removeAll();
        for (FlaggedAdaptable flagged : status) {
            TableItem row = new TableItem(table, SWT.NONE);
            row.setText(0, flagged.getFlag() + "");
            row.setText(1, ((IFile) flagged.getAdapter(IFile.class))
                    .getProjectRelativePath().toString());
            row.setData(flagged);
        }
        abortAction.setEnabled(true);
    }

    public void clearView() {
        statusLabel.setText("");
        table.removeAll();
        currentProject = null;
        abortAction.setEnabled(false);
    }

    public void setCurrentProject(IProject project) {
        try {
            if (this.currentProject != project) {
                this.currentProject = project;
                if (project != null
                        && project
                                .getPersistentProperty(ResourceProperties.MERGING) != null) {
                    populateView();
                } else {
                    clearView();
                }
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection) selection;
            if (structured.getFirstElement() instanceof IAdaptable) {
                IResource resource = (IResource) ((IAdaptable) structured
                        .getFirstElement()).getAdapter(IResource.class);
                if (resource != null) {
                    setCurrentProject(resource.getProject());
                    return;
                }
            }
        }
        if (part instanceof IEditorPart) {
            IEditorInput input = ((IEditorPart) part).getEditorInput();
            IFile file = (IFile) input.getAdapter(IFile.class);
            if (file != null) {
                setCurrentProject(file.getProject());
                return;
            }
        }
    }

    @Override
    public void setFocus() {
        table.setFocus();
    }

    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(this);
        super.dispose();
    }

    public static MergeView getView() {
        return (MergeView) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().findView(ID);
    }

}
