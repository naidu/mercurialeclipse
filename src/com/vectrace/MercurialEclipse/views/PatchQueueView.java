/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQFoldClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQPopClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQPushClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQSeriesClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.QDeleteHandler;
import com.vectrace.MercurialEclipse.menu.QImportHandler;
import com.vectrace.MercurialEclipse.menu.QNewHandler;
import com.vectrace.MercurialEclipse.menu.QRefreshHandler;
import com.vectrace.MercurialEclipse.model.Patch;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.ui.PatchTable;

/**
 * @author bastian
 * 
 */
public class PatchQueueView extends ViewPart implements ISelectionListener {

    private IResource resource;
    private PatchTable table;
    private Label statusLabel;
    private Action newAction;
    private Action qrefreshAction;
    private Action qPushAction;
    private Action qPushAllAction;
    private Action qPopAction;
    private Action qPopAllAction;
    private Action qDeleteAction;
    private Action qFoldAction;
    private Action qImportAction;
    private String currentHgRoot;
    public final static String ID = PatchQueueView.class.getName();

    /**
     * 
     */
    public PatchQueueView() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        table = new PatchTable(parent);
        createToolBar();
        getSite().getPage().addSelectionListener(this);
    }

    private void createToolBar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        qImportAction = new Action("qimport") {
            @Override
            public void run() {
                try {
                    QImportHandler.openWizard(resource, getSite().getShell());
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }
        };
        qImportAction.setEnabled(true);
        mgr.add(qImportAction);

        newAction = new Action("qnew") {
            @Override
            public void run() {
                try {
                    QNewHandler.openWizard(resource, getSite().getShell());
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }
        };
        newAction.setEnabled(true);
        mgr.add(newAction);

        qrefreshAction = new Action("qrefresh") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                QRefreshHandler.openWizard(resource, getSite().getShell());
            }
        };
        qrefreshAction.setEnabled(true);
        mgr.add(qrefreshAction);

        qPushAction = new Action("qpush") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                try {
                    HgQPushClient.push(resource, false, table.getSelection()
                            .getName());
                    populateTable();
                    resource.refreshLocal(IResource.DEPTH_INFINITE,
                            new NullProgressMonitor());
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPushAction.setEnabled(true);
        mgr.add(qPushAction);

        qPopAction = new Action("qpop") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                try {
                    HgQPopClient.pop(resource, false, table.getSelection()
                            .getName());
                    populateTable();
                    resource.refreshLocal(IResource.DEPTH_INFINITE,
                            new NullProgressMonitor());
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPopAction.setEnabled(true);
        mgr.add(qPopAction);

        qPushAllAction = new Action("qpush all") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                try {
                    HgQPushClient.pushAll(resource, false);
                    populateTable();
                    resource.refreshLocal(IResource.DEPTH_INFINITE,
                            new NullProgressMonitor());
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPushAllAction.setEnabled(true);
        mgr.add(qPushAllAction);

        qPopAllAction = new Action("qpop all") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                try {
                    HgQPopClient.popAll(resource, false);
                    populateTable();
                    resource.refreshLocal(IResource.DEPTH_INFINITE,
                            new NullProgressMonitor());
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPopAllAction.setEnabled(true);
        mgr.add(qPopAllAction);

        qFoldAction = new Action("qfold") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                try {
                    List<Patch> patches = table.getSelections();
                    if (patches.size() > 0) {
                        HgQFoldClient.fold(resource, true, null, patches);
                        populateTable();
                    }
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qFoldAction.setEnabled(true);
        mgr.add(qFoldAction);

        qDeleteAction = new Action("qdel") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                QDeleteHandler.openWizard(resource, getSite().getShell());
                populateTable();
                try {
                    resource.refreshLocal(IResource.DEPTH_INFINITE,
                            new NullProgressMonitor());
                } catch (CoreException e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qDeleteAction.setEnabled(true);
        mgr.add(qDeleteAction);

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        populateTable();
    }

    /**
     * @throws HgException
     */
    public void populateTable() {
        if (resource != null && resource.isAccessible()) {
            try {
                List<Patch> patches = HgQSeriesClient
                        .getPatchesInSeries(resource);
                table.setPatches(patches.toArray(new Patch[patches.size()]));
            } catch (HgException e) {
                statusLabel.setText(e.getLocalizedMessage());
                MercurialEclipsePlugin.logError(e);
            }
        }
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        try {
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection structured = (IStructuredSelection) selection;
                if (structured.getFirstElement() instanceof IAdaptable) {
                    IResource newResource = (IResource) ((IAdaptable) structured
                            .getFirstElement()).getAdapter(IResource.class);
                    if (resource != null
                            && resource.isAccessible()
                            && MercurialUtilities.hgIsTeamProviderFor(
                                    resource, false) && newResource != null
                            && newResource.equals(resource)) {
                        return;
                    }

                    if (newResource != null
                            && newResource.isAccessible()
                            && MercurialUtilities.hgIsTeamProviderFor(
                                    newResource, false)) {
                        String newRoot = HgRootClient.getHgRoot(newResource);
                        if (!newRoot.equals(currentHgRoot)) {
                            currentHgRoot = newRoot;
                            resource = newResource;
                            populateTable();
                            statusLabel.setText("Repository: " + currentHgRoot);
                        }
                    }

                }
            }
            if (part instanceof IEditorPart) {
                IEditorInput input = ((IEditorPart) part).getEditorInput();
                IFile file = (IFile) input.getAdapter(IFile.class);
                if (file != null
                        && file.isAccessible()
                        && MercurialUtilities.hgIsTeamProviderFor(file,
                                false)) {
                    String newRoot = HgRootClient.getHgRoot(file);
                    if (!newRoot.equals(currentHgRoot)) {
                        currentHgRoot = newRoot;
                        resource = file;
                        statusLabel.setText("Repository: " + currentHgRoot);
                    }
                }
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            statusLabel.setText(e.getMessage());
        }
    }

    public static PatchQueueView getView() {
        PatchQueueView view = (PatchQueueView) PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().findView(ID);
        if (view == null) {
            try {
                view = (PatchQueueView) PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage()
                        .showView(ID);
            } catch (PartInitException e) {
                MercurialEclipsePlugin.logError(e);
            }
        }
        return view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(this);
        super.dispose();
    }

}
