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
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.commands.HgResolveClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgIMergeClient;
import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.menu.UpdateHandler;
import com.vectrace.MercurialEclipse.model.FlaggedAdaptable;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

public class MergeView extends ViewPart implements ISelectionListener, Observer {

    public final static String ID = MergeView.class.getName();

    private Label statusLabel;
    private Table table;

    private Action abortAction;

    private IProject currentProject;

    private Action markResolvedAction;

    private Action markUnresolvedAction;

    @Override
    public void createPartControl(final Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        table = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
                | SWT.V_SCROLL | SWT.H_SCROLL);
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
                    FlaggedAdaptable flagged = (FlaggedAdaptable) item
                            .getData();
                    IFile file = (IFile) flagged.getAdapter(IFile.class);

                    String mergeNodeId = currentProject
                            .getPersistentProperty(ResourceProperties.MERGING);

                    String[] parents = HgParentClient
                            .getParentNodeIds(currentProject);
                    int ancestor = HgParentClient.findCommonAncestor(
                            currentProject.getLocation().toFile(), parents[0],
                            parents[1]);

                    RevisionNode mergeNode = new RevisionNode(
                            new MercurialRevisionStorage(file, mergeNodeId));
                    RevisionNode ancestorNode = new RevisionNode(
                            new MercurialRevisionStorage(file, ancestor));

                    HgCompareEditorInput compareInput = new HgCompareEditorInput(
                            new CompareConfiguration(), file, ancestorNode,
                            mergeNode, true);

                    int returnValue = CompareUtils
                            .openCompareDialog(compareInput);
                    if (returnValue == Window.OK
                            && markResolvedAction.isEnabled()) {
                        markResolvedAction.run();
                    }
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    MercurialEclipsePlugin.showError(e);
                }
            }
        });

        String[] titles = { Messages.getString("MergeView.column.status"), Messages.getString("MergeView.column.file") }; //$NON-NLS-1$ //$NON-NLS-2$
        int[] widths = { 100, 400 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }

        createToolBar();
        getSite().getPage().addSelectionListener(this);
        MercurialStatusCache.getInstance().addObserver(this);
    }

    private void createToolBar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        abortAction = new Action(Messages.getString("MergeView.abort")) { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    UpdateHandler update = new UpdateHandler();
                    update.setCleanEnabled(true);
                    update.setRevision(".");
                    update.setShell(table.getShell());
                    update.run(currentProject);
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        abortAction.setEnabled(false);
        mgr.add(abortAction);
        markResolvedAction = new Action(Messages.getString("MergeView.markResolved")) { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    IFile file = getSelection();
                    if (file != null) {
                        if (HgResolveClient.checkAvailable()) {
                            HgResolveClient.markResolved(file);
                        } else {
                            HgIMergeClient.markResolved(file);
                        }
                        populateView(true);
                    }
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        markResolvedAction.setEnabled(false);
        mgr.add(markResolvedAction);
        markUnresolvedAction = new Action(Messages.getString("MergeView.markUnresolved")) { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    IFile file = getSelection();
                    if (file != null) {
                        if (HgResolveClient.checkAvailable()) {
                            HgResolveClient.markUnresolved(file);
                        } else {
                            HgIMergeClient.markUnresolved(file);
                        }
                        populateView(true);
                    }
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        markUnresolvedAction.setEnabled(false);
        mgr.add(markUnresolvedAction);
    }

    private void populateView(boolean attemptToCommit) throws HgException {
        statusLabel.setText(currentProject.getName());
        List<FlaggedAdaptable> status = null;
        if (HgResolveClient.checkAvailable()) {
            //HgResolveClient.resolveAll(currentProject.getLocation().toFile());
            status = HgResolveClient.list(currentProject);
        } else {
            status = HgIMergeClient.getMergeStatus(currentProject);
        }
        table.removeAll();
        for (FlaggedAdaptable flagged : status) {
            TableItem row = new TableItem(table, SWT.NONE);
            row.setText(0, flagged.getStatus());
            IFile iFile = ((IFile) flagged.getAdapter(IFile.class));
            row.setText(1, iFile.getProjectRelativePath().toString());
            row.setData(flagged);
            if (flagged.getFlag() == MercurialStatusCache.CHAR_UNRESOLVED) {
                row.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
            }
        }
        abortAction.setEnabled(true);
        markResolvedAction.setEnabled(true);
        markUnresolvedAction.setEnabled(true);

        if(attemptToCommit) {
            attemptToCommitMerge();
        }
    }

    private void attemptToCommitMerge() {
        try {
            String mergeNode = currentProject.getPersistentProperty(ResourceProperties.MERGING);

            // offer commit of merge exactly once if no conflicts
            // are found
            boolean allResolved = areAllResolved();
            if (allResolved) {
                this.statusLabel.setText(currentProject.getName()
                                + Messages.getString("MergeView.PleaseCommitMerge") //$NON-NLS-1$
                                + mergeNode);
                if (currentProject
                        .getSessionProperty(ResourceProperties.MERGE_COMMIT_OFFERED) == null) {
                    new CommitMergeHandler()
                            .commitMergeWithCommitDialog(
                                    this.currentProject, getSite()
                                            .getShell());
                    currentProject
                            .setSessionProperty(
                                    ResourceProperties.MERGE_COMMIT_OFFERED,
                                    "true"); //$NON-NLS-1$
                }
            }
        } catch (Exception e) {
            MercurialEclipsePlugin.logError(e);
        }
    }

    public void clearView() {
        statusLabel.setText(""); //$NON-NLS-1$
        table.removeAll();
        currentProject = null;
        abortAction.setEnabled(false);
        markResolvedAction.setEnabled(false);
        markUnresolvedAction.setEnabled(false);
    }

    public void setCurrentProject(IProject project) {
        if (currentProject != project && project != null && project.isAccessible()) {
            try {
                if (project
                        .getPersistentProperty(ResourceProperties.MERGING) != null) {
                    currentProject = project;
                    populateView(false);
                } else {
                    clearView();
                }
            } catch (Exception e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
    }

    private boolean areAllResolved() {
        boolean allResolved = true;
        if (table.getItems() != null
                && table.getItems().length > 0) {
            for (TableItem item : table.getItems()) {
                FlaggedAdaptable fa = (FlaggedAdaptable) item
                        .getData();
                allResolved &= fa.getFlag() == MercurialStatusCache.CHAR_RESOLVED;
            }
        }
        return allResolved;
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
        MercurialStatusCache.getInstance().deleteObserver(this);
        super.dispose();
    }

    /**
     * @return
     */
    private IFile getSelection() {
        TableItem[] selection = table.getSelection();
        if (selection != null && selection.length > 0) {
            FlaggedAdaptable fa = (FlaggedAdaptable) table.getSelection()[0]
                    .getData();
            IFile iFile = ((IFile) fa.getAdapter(IFile.class));
            return iFile;
        }
        return null;
    }

    public void update(Observable o, Object arg) {
        if(currentProject == null || !(arg instanceof Set<?>)){
            return;
        }
        Set<?> set = (Set<?>) arg;

        if(set.contains(currentProject)){
            Display.getDefault().asyncExec(new Runnable() {

                public void run() {
                    IProject backup = currentProject;
                    clearView();
                    setCurrentProject(backup);
                }
            });
        }
    }

}
