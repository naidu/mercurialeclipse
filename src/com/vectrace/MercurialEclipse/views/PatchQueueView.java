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
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQPopClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQPushClient;
import com.vectrace.MercurialEclipse.commands.mq.HgQSeriesClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.menu.QNewHandler;
import com.vectrace.MercurialEclipse.menu.QRefreshHandler;
import com.vectrace.MercurialEclipse.model.Patch;
import com.vectrace.MercurialEclipse.ui.PatchTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

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
    public final static String ID = PatchQueueView.class.getName();

    /**
     * 
     */
    public PatchQueueView() {
    }
    

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        Composite composite = SWTWidgetHelper.createComposite(parent, 1);
        statusLabel = SWTWidgetHelper.createWrappingLabel(composite,
                "Repository: ", 0, 350);
        table = new PatchTable(composite);
        createToolBar();
        getSite().getPage().addSelectionListener(this);
    }
    

    private void createToolBar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

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
                    HgQPushClient.push(resource, false, table.getSelection().getName());
                    populateTable();
                } catch (HgException e) {                    
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPushAction.setEnabled(true);
        mgr.add(qPushAction);
        
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
                } catch (HgException e) {                    
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPushAllAction.setEnabled(true);
        mgr.add(qPushAllAction);
        
        qPopAction = new Action("qpop") {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                try {
                    HgQPopClient.pop(resource, false, table.getSelection().getName());
                    populateTable();
                } catch (HgException e) {                    
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPopAction.setEnabled(true);
        mgr.add(qPopAction);
        
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
                } catch (HgException e) {                    
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getLocalizedMessage());
                }
            }
        };
        qPopAllAction.setEnabled(true);
        mgr.add(qPopAllAction);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {
        try {
            if (resource != null) {
                populateTable();
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }

    }

    /**
     * @throws HgException
     */
    private void populateTable() throws HgException {
        if (resource != null) {
            List<Patch> patches = HgQSeriesClient.getPatchesInSeries(resource);
            table.setPatches(patches.toArray(new Patch[patches.size()]));
        }
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structured = (IStructuredSelection) selection;
            if (structured.getFirstElement() instanceof IAdaptable) {
                resource = (IResource) ((IAdaptable) structured
                        .getFirstElement()).getAdapter(IResource.class);
                try {
                    if (resource != null) {
                        statusLabel.setText("Repository: "
                                + HgRootClient.getHgRoot(resource));
                    }
                } catch (HgException e) {
                    MercurialEclipsePlugin.logError(e);
                    statusLabel.setText(e.getMessage());
                }
            }
        }
        if (part instanceof IEditorPart) {
            IEditorInput input = ((IEditorPart) part).getEditorInput();
            IFile file = (IFile) input.getAdapter(IFile.class);
            if (file != null) {
                resource = file;
            }
        }
        try {
            populateTable();
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
            statusLabel.setText(e.getLocalizedMessage());
        }
    }

    public static PatchQueueView getView() {
        return (PatchQueueView) PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().findView(ID);
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
