/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     StefanC                   - some updates, code cleanup
 *     Stefan Groschupf          - logError
 *     Subclipse project committers - reference
 *     Charles O'Farrell         - comparison diff
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.BaseSelectionListenerAction;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.OpenMercurialRevisionAction;
import com.vectrace.MercurialEclipse.commands.HgUpdateClient;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.ResourceProperties;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.wizards.Messages;

public class MercurialHistoryPage extends HistoryPage {
    public MercurialHistory getMercurialHistory() {
        return mercurialHistory;
    }

    private GraphLogTableViewer viewer;
    private IResource resource;
    private ChangeLogContentProvider changeLogViewContentProvider;
    private MercurialHistory mercurialHistory;
    private IFileRevision[] entries;
    private RefreshMercurialHistory refreshFileHistoryJob;
    private ChangedPathsPage changedPaths;

    class RefreshMercurialHistory extends Job {
        MercurialHistory mercurialHistory;
        private int from;

        public RefreshMercurialHistory(int from, MercurialHistory fileHistory) {
            super("Fetching Mercurial revisions..."); //$NON-NLS-1$
            this.from = from;
            this.mercurialHistory = fileHistory;
        }

        public void setFileHistory(MercurialHistory mercurialHistory) {
            this.mercurialHistory = mercurialHistory;
        }

        @Override
        public IStatus run(IProgressMonitor monitor) {

            IStatus status = Status.OK_STATUS;

            if (mercurialHistory != null) {
                try {
                    mercurialHistory.refresh(monitor, from);
                } catch (CoreException e) {
                    MercurialEclipsePlugin.logError(e);
                }

                final Runnable runnable = new Runnable() {
                    public void run() {
                        viewer.setInput(mercurialHistory);
                    }
                };

                // Internal code copied here from Utils.asyncExec
                if (viewer == null) {
                    return status;
                }

                final Control ctrl = viewer.getControl();
                if (ctrl != null && !ctrl.isDisposed()) {
                    ctrl.getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            if (!ctrl.isDisposed()) {
                                BusyIndicator.showWhile(ctrl.getDisplay(),
                                        runnable);
                            }
                        }
                    });
                }
            }

            return status;
        }
    }

    class ChangeLogContentProvider implements IStructuredContentProvider {

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            entries = null;
        }

        public void dispose() {
        }

        public Object[] getElements(Object parent) {
            if (entries != null) {
                return entries;
            }

            final IFileHistory fileHistory = (IFileHistory) parent;
            entries = fileHistory.getFileRevisions();

            return entries;
        }
    }

    class ChangeSetLabelProvider extends LabelProvider implements
            ITableLabelProvider {

        public String getColumnText(Object obj, int index) {
            String ret;

            if ((obj instanceof MercurialRevision) != true) {
                return "Type Error"; //$NON-NLS-1$
            }

            MercurialRevision mercurialFileRevision = (MercurialRevision) obj;
            ChangeSet changeSet = mercurialFileRevision.getChangeSet();            

            switch (index) {
            case 1:
                ret = changeSet.toString();
                break;
            case 2:
                ret = changeSet.getTag();
                break;
            case 3:
                ret = changeSet.getBranch();
                break;
            case 4:
                ret = changeSet.getUser();
                break;
            case 5:
                ret = changeSet.getDate();
                break;
            case 6:
                ret = changeSet.getSummary();
                break;
            default:
                ret = null;
                break;
            }
            return ret;
        }

        public Image getColumnImage(Object obj, int index) {
            return null;
        }
    }

    public MercurialHistoryPage(IResource resource) {
        super();
        if (isValidInput(resource)) {
            this.resource = resource;
        }
    }

    @Override
    public boolean inputSet() {
        mercurialHistory = new MercurialHistory(resource);
        refresh();
        return true;
    }

    @Override
    public void createControl(Composite parent) {
        changedPaths = new ChangedPathsPage(this, parent);
        createTableHistory(changedPaths.getControl());
        changedPaths.createControl();
        getSite().setSelectionProvider(viewer);
        getSite().getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), new Action() {
            @Override
            public void run() {
                copyToClipboard();
            }
        });
    }

    private void createTableHistory(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout0 = new GridLayout();
        layout0.marginHeight = 0;
        layout0.marginWidth = 0;
        composite.setLayout(layout0);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.grabExcessVerticalSpace = true;
        composite.setLayoutData(data);

        viewer = new GraphLogTableViewer(composite, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.VIRTUAL, this);
        Table changeLogTable = viewer.getTable();

        changeLogTable.setLinesVisible(true);
        changeLogTable.setHeaderVisible(true);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        changeLogTable.setLayoutData(gridData);

        TableLayout layout = new TableLayout();
        changeLogTable.setLayout(layout);

        TableColumn column = new TableColumn(changeLogTable, SWT.CENTER);
        column.setText(Messages.getString("MercurialHistoryPage.columnHeader.graph")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(7, true));
        column = new TableColumn(changeLogTable, SWT.LEFT);
        column.setText(Messages.getString("MercurialHistoryPage.columnHeader.changeset")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(15, true));
        column = new TableColumn(changeLogTable, SWT.LEFT);
        column.setText(Messages.getString("MercurialHistoryPage.columnHeader.tag")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(10, true));
        column = new TableColumn(changeLogTable, SWT.LEFT);
        column.setText(Messages.getString("MercurialHistoryPage.columnHeader.branch")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(10, true));
        column = new TableColumn(changeLogTable, SWT.LEFT);
        column.setText(Messages.getString("MercurialHistoryPage.columnHeader.user")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(7, true));
        column = new TableColumn(changeLogTable, SWT.LEFT);
        column.setText(Messages.getString("MercurialHistoryPage.columnHeader.date")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(13, true));
        column = new TableColumn(changeLogTable, SWT.LEFT);
        column.setText(Messages.getString("MercurialHistoryPage.columnHeader.summary")); //$NON-NLS-1$
        layout.addColumnData(new ColumnWeightData(25, true));

        viewer.setLabelProvider(new ChangeSetLabelProvider());
        changeLogViewContentProvider = new ChangeLogContentProvider();
        viewer.setContentProvider(changeLogViewContentProvider);

        contributeActions();
    }
    
    private void copyToClipboard() {
        IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
        Iterator iterator = selection.iterator();
        StringBuilder text = new StringBuilder();
        for(int columnIndex = 1; columnIndex < viewer.getTable().getColumnCount(); columnIndex++) {
            text.append(viewer.getTable().getColumn(columnIndex).getText()).append('\t');
        }
        
        text.append(System.getProperty("line.separator")); //$NON-NLS-1$

        while(iterator.hasNext()) {
            Object next = iterator.next();
            ITableLabelProvider labelProvider = (ITableLabelProvider) viewer.getLabelProvider();
            for(int columnIndex = 1; columnIndex < viewer.getTable().getColumnCount(); columnIndex++) {
                text.append(labelProvider.getColumnText(next, columnIndex)).append('\t');
            }
            text.append(System.getProperty("line.separator")); //$NON-NLS-1$
        }
        new Clipboard(getSite().getShell().getDisplay()).setContents(new String[]{text.toString()}, 
                new Transfer[]{ TextTransfer.getInstance() });
    }

    private void contributeActions() {
        final BaseSelectionListenerAction openAction = getOpenAction();
        final Action compareAction = getCompareAction();

        final Action updateAction = new Action(Messages.getString("MercurialHistoryPage.updateAction.name")) { //$NON-NLS-1$
            private MercurialRevision rev;

            @Override
            public void run() {
                try {
                    IProject project = resource.getProject();
                    Assert.isNotNull(project);
                    HgUpdateClient.update(project, rev.getChangeSet()
                            .getChangeset(), true);
                    // update ends merges, so reset merge properties
                    project.setPersistentProperty(ResourceProperties.MERGING,
                            null);
                    project.setSessionProperty(
                            ResourceProperties.MERGE_COMMIT_OFFERED, null);
                    new RefreshStatusJob(
                            Messages.getString("MercurialHistoryPage.refreshJob.name"), //$NON-NLS-1$
                            project).schedule();
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }

            @Override
            public boolean isEnabled() {
                IStructuredSelection selection = (IStructuredSelection) viewer
                        .getSelection();
                Object[] revs = selection.toArray();
                if (revs != null && revs.length == 1) {
                    rev = (MercurialRevision) revs[0];
                    return true;
                }
                return false;
            }
        };

        // Contribute actions to popup menu
        final MenuManager menuMgr = new MenuManager();
        Menu menu = menuMgr.createContextMenu(viewer.getTable());
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager menuMgr1) {
                menuMgr1
                        .add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
                menuMgr1.add(openAction);
                if (resource == null || resource.getType() != IResource.FILE) {
                    openAction.setEnabled(false);
                } else {
                    openAction.setEnabled(true);
                }
                // TODO This is a HACK but I can't get the menu to update on
                // selection :-(
                compareAction.setEnabled(compareAction.isEnabled());
                menuMgr1.add(compareAction);
                updateAction.setEnabled(updateAction.isEnabled());
                menuMgr1.add(updateAction);
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        viewer.getTable().setMenu(menu);
    }

    private OpenMercurialRevisionAction getOpenAction() {
        final OpenMercurialRevisionAction openAction = new OpenMercurialRevisionAction(
                "Open"); //$NON-NLS-1$
        viewer.getTable().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openAction.selectionChanged((IStructuredSelection) viewer
                        .getSelection());
            }
        });
        openAction.setPage(this);
        return openAction;
    }

    private Action getCompareAction() {
        return new Action(Messages.getString("CompareAction.label")) { //$NON-NLS-1$) {
            @Override
            public void run() {
                try {
                    CompareUtils.openEditor(getStorage(0), getStorage(1),
                            false, false);
                } catch (Exception e) {
                    MercurialEclipsePlugin.logError(e);
                }
            }

            @Override
            public boolean isEnabled() {
                return IFile.class.isAssignableFrom(getInput().getClass())
                        && ((IStructuredSelection) viewer.getSelection())
                                .size() == 2;
            }

            private IStorageMercurialRevision getStorage(int i)
                    throws CoreException {
                IStructuredSelection selection = (IStructuredSelection) viewer
                        .getSelection();
                Object[] revs = selection.toArray();
                if (i >= revs.length) {
                    return null;
                }
                MercurialRevision rev = (MercurialRevision) revs[i];
                return (IStorageMercurialRevision) rev.getStorage(null);
            }
        };
    }

    @Override
    public Control getControl() {
        return changedPaths.getControl();
    }

    @Override
    public void setFocus() {
        // Nothing to see here
    }

    public String getDescription() {
        return resource.getFullPath().toOSString();
    }

    public String getName() {
        return resource.getFullPath().toOSString();
    }

    public boolean isValidInput(Object object) {
        return true;
    }

    public void refresh() {
        if (refreshFileHistoryJob == null) {
            refreshFileHistoryJob = new RefreshMercurialHistory(
                    Integer.MAX_VALUE,
                    mercurialHistory);
        }

        if (refreshFileHistoryJob.getState() != Job.NONE) {
            refreshFileHistoryJob.cancel();
        }
        refreshFileHistoryJob.setFileHistory(mercurialHistory);
        refreshFileHistoryJob.schedule();
    }

    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        return null;
    }

    public TableViewer getTableViewer() {
        return viewer;
    }
}
