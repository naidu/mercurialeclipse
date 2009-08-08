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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.team.core.Team;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQFoldClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQPopClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQPushClient;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQSeriesClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.QDeleteHandler;
import com.vectrace.MercurialEclipse.menu.QImportHandler;
import com.vectrace.MercurialEclipse.menu.QNewHandler;
import com.vectrace.MercurialEclipse.menu.QRefreshHandler;
import com.vectrace.MercurialEclipse.model.HgRoot;
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
    private Action qNewAction;
    private Action qRefreshAction;
    private Action qPushAction;
    private Action qPushAllAction;
    private Action qPopAction;
    private Action qPopAllAction;
    private Action qDeleteAction;
    private Action qFoldAction;
    private Action qImportAction;
    private HgRoot currentHgRoot;
    public final static String ID = PatchQueueView.class.getName();

    public PatchQueueView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        table = new PatchTable(parent);
        createActions();
        createToolBar();
        createMenus();
        getSite().getPage().addSelectionListener(this);
    }

    private void createToolBar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(qImportAction);
        mgr.add(qNewAction);
        mgr.add(qRefreshAction);
        mgr.add(qPushAction);
        mgr.add(qPopAction);
        mgr.add(qPushAllAction);
        mgr.add(qPopAllAction);
        mgr.add(qFoldAction);
        mgr.add(qDeleteAction);
    }

    private void createActions() {
        qImportAction = new Action("qimport") { //$NON-NLS-1$
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

        qNewAction = new Action("qnew") { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    QNewHandler.openWizard(resource, getSite().getShell());
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }
        };
        qNewAction.setEnabled(true);

        qRefreshAction = new Action("qrefresh") { //$NON-NLS-1$
            @Override
            public void run() {
                QRefreshHandler.openWizard(resource, getSite().getShell());
            }
        };
        qRefreshAction.setEnabled(true);

        qPushAction = new Action("qpush") { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    HgQPushClient.push(resource, false, table
                            .getSelection()
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

        qPopAction = new Action("qpop") { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    HgQPopClient.pop(resource, false, table
                            .getSelection()
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

        qPushAllAction = new Action("qpush all") { //$NON-NLS-1$
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

        qPopAllAction = new Action("qpop all") { //$NON-NLS-1$
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

        qFoldAction = new Action("qfold") { //$NON-NLS-1$
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

        qDeleteAction = new Action("qdel") { //$NON-NLS-1$
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
    }

    private void createMenus() {
        final IMenuManager menuMgr = getViewSite().getActionBars()
                .getMenuManager();
        menuMgr.add(qImportAction);
        menuMgr.add(qNewAction);
        menuMgr.add(qRefreshAction);
        menuMgr.add(qPushAction);
        menuMgr.add(qPopAction);
        menuMgr.add(qPushAllAction);
        menuMgr.add(qPopAllAction);
        menuMgr.add(qFoldAction);
        menuMgr.add(qDeleteAction);
    }

    @Override
    public void setFocus() {
        populateTable();
    }

    public void populateTable() {
        if (resource != null && resource.isAccessible()
                && !resource.isDerived() && !resource.isLinked()
                && !Team.isIgnoredHint(resource)) {
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
                            && MercurialUtilities.hgIsTeamProviderFor(resource,
                                    false) && newResource != null
                            && newResource.equals(resource)) {
                        return;
                    }

                    if (newResource != null
                            && newResource.isAccessible()
                            && MercurialUtilities.hgIsTeamProviderFor(
                                    newResource, false)) {
                        HgRoot newRoot = HgRootClient.getHgRoot(newResource);
                        if (!newRoot.equals(currentHgRoot)) {
                            currentHgRoot = newRoot;
                            resource = newResource;
                            populateTable();
                            statusLabel.setText(Messages.getString("PatchQueueView.repository") + currentHgRoot); //$NON-NLS-1$
                        }
                    }

                }
            }
            if (part instanceof IEditorPart) {
                IEditorInput input = ((IEditorPart) part).getEditorInput();
                IFile file = (IFile) input.getAdapter(IFile.class);
                if (file != null && file.isAccessible()
                        && MercurialUtilities.hgIsTeamProviderFor(file, false)) {
                    HgRoot newRoot = HgRootClient.getHgRoot(file);
                    if (!newRoot.equals(currentHgRoot)) {
                        currentHgRoot = newRoot;
                        resource = file;
                        statusLabel.setText(Messages.getString("PatchQueueView.repository") + currentHgRoot); //$NON-NLS-1$
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

    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(this);
        super.dispose();
    }

}
