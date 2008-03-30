/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *******************************************************************************/
package com.vectrace.MercurialEclipse.views;


import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeLog;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialRepositorySubscriber;

/**
 * ChangeLog view based on the view example for now
 * 
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class ChangeLogView extends ViewPart 
{
	private TableViewer viewer;
  private ChangeLog changeLog;
  private IResource resource;
  private Table changeLogTable;
  
	private Action action1;
	private Action action2;
	private Action doubleClickAction;

  
  private ChangeLogContentProvider changeLogViewContentProvider;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	class ChangeLogContentProvider implements IStructuredContentProvider
	{ 
    
		public void inputChanged(Viewer v, Object oldInput, Object newInput) 
		{
		}
		
		public void dispose() 
		{
		}

		public Object[] getElements(Object parent) 
		{
//      System.out.println("Object[] ChangeLogViewContentProvider::getElements()");      
      return changeLog.getChangeLog().toArray();
		}

    public void setChangeLog(IResource in_resource)
    {
//      System.out.println("ChangeLogViewContentProvider::setChangeLog()");
      resource=in_resource;
      changeLog.ChangeChangeLog(in_resource);
      viewer.refresh();
    }

	
	}

	class ChangeSetLabelProvider extends LabelProvider implements ITableLabelProvider
	{
	  		
    public String getColumnText(Object obj, int index) 
		{
		  String ret;

//      System.out.println("ViewLabelProvider::getColumnText(obj," + index + ")");
		  
      if((obj instanceof ChangeSet) != true)
      {
        return "Type Error";
      }
	  
      ChangeSet changeSet = (ChangeSet) obj;
     
      switch (index)
      {
        case 0:
          ret= changeSet.getChangeset();
          break;
        case 1:
          ret= changeSet.getTag();
          break;
        case 2:
          ret= changeSet.getUser();
          break;
        case 3:
          ret= changeSet.getDate();
          break;
        case 4:
          ret= changeSet.getFiles();
          break;
        case 5:
          ret= changeSet.getDescription();
          break;
        default:
          ret= null;
          break;
      }
//      System.out.println("ViewLabelProvider::getColumnText(" + changeSet.getChangeset() +"," + index + ")=" + ret);
      return ret;
		}
		public Image getColumnImage(Object obj, int index) 
		{
//      System.out.println("ViewLabelProvider::getColumnImage(obj," + index + ")");
			return null; 
		}
	}

	class NameSorter extends ViewerSorter 
	{


	  /* 
	    * @param viewer the viewer
	    * @param e1 the first element
	    * @param e2 the second element
	    * @return a negative number if the first element is less  than the 
	    *  second element; the value <code>0</code> if the first element is
	    *  equal to the second element; and a positive number if the first
	    *  element is greater than the second element
	    */

	  
	  /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    @Override

    public int compare(Viewer viewer, Object e1, Object e2)
    {
      if( ((e1 instanceof ChangeSet) != true) || ((e2 instanceof ChangeSet) != true) )
      {
        return super.compare(viewer, e1, e2);
      }
          
      int value1=((ChangeSet) e1).getChangesetIndex();
      int value2=((ChangeSet) e2).getChangesetIndex();
// we want it reverse sorted
      if(value1<value2)
      {
        return 1;
      }
      else if(value1==value2)
      {
        return 0;
      }
      else
      {
        return -1;
      }
    }	  
	}

	/**
	 * The constructor.
	 */
	public ChangeLogView() 
	{
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@Override
    public void createPartControl(Composite parent) 
	{
    viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    changeLogTable=viewer.getTable();

    changeLogTable.setLinesVisible(true);
    changeLogTable.setHeaderVisible(true);

    GridData gridData = new GridData(GridData.FILL_BOTH);
    changeLogTable.setLayoutData(gridData);

    TableLayout layout = new TableLayout();    
    changeLogTable.setLayout(layout);    

    
    TableColumn column = new TableColumn(changeLogTable,SWT.LEFT);
//    changesetTableColumn.setResizable(true);
    column.setText("Changeset");
    layout.addColumnData(new ColumnWeightData(15, true));
    column = new TableColumn(changeLogTable,SWT.LEFT);
    column.setText("Tag");
    layout.addColumnData(new ColumnWeightData(10, true));
    column = new TableColumn(changeLogTable,SWT.LEFT);
    column.setText("User");
    layout.addColumnData(new ColumnWeightData(7, true));
    column = new TableColumn(changeLogTable,SWT.LEFT);
    column.setText("Date");
    layout.addColumnData(new ColumnWeightData(18, true));
    column = new TableColumn(changeLogTable,SWT.LEFT);
    column.setText("Files");
    layout.addColumnData(new ColumnWeightData(25, true));
    column = new TableColumn(changeLogTable,SWT.LEFT);
    column.setText("Description");
    layout.addColumnData(new ColumnWeightData(25, true));

    viewer.setLabelProvider(new ChangeSetLabelProvider());
    

    changeLogViewContentProvider = new ChangeLogContentProvider(); 

    viewer.setContentProvider(changeLogViewContentProvider);
    viewer.setSorter(new NameSorter());
    changeLog=new ChangeLog();
    viewer.setInput(changeLog);   // getViewSite());
    
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	 public void showChangeLog(IResource in_resource) 
	 {
//     System.out.println("ChangeLogView::showChangeLog(" + in_resource.toString() + ")" );
	   changeLogViewContentProvider.setChangeLog(in_resource);	 
	 }

	private void hookContextMenu() 
	{
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() 
  		{
  			public void menuAboutToShow(IMenuManager manager) 
  			{
  				ChangeLogView.this.fillContextMenu(manager);
  			}
  		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() 
	{
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) 
	{ 
 		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
	}

	private void fillContextMenu(IMenuManager manager) 
	{
	  manager.add(action1);
		manager.add(action2);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

	}
	
	private void fillLocalToolBar(IToolBarManager manager) 
	{
 		manager.add(action1);
		manager.add(action2);
	}

	private void makeActions() 
	{

	  action1 = new Action() 
		{
			@Override
            public void run() 
			{
				showMessage("Action 1 executed");
			}
		};
		
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

    action2 = new Action() 
    {
      @Override
    public void run() 
      {
        showMessage("Action 2 executed");
      }
    };

		
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		doubleClickAction = new Action() 
		{
			@Override
            public void run() 
			{
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

	      if((obj instanceof ChangeSet) != true)
	      {
	        return; //Silently ignore unknown double clicks
	      }
	    
	      ChangeSet changeSet = (ChangeSet) obj;      

//        showMessage("Double-click detected on "+changesetHash);
        
        try
        {
          int rev1=changeSet.getChangesetIndex()-1;
          if(rev1>0)
          {
            rev1=0;
          }
          int rev2=changeSet.getChangesetIndex();
//          IStorage r1=(IStorage) resource;
          IStorage r1=new IStorageMercurialRevision( resource, rev1);
          IStorage r2=new IStorageMercurialRevision( resource, rev2);
          MercurialRepositorySubscriber subscriber = new MercurialRepositorySubscriber();
          SyncInfo syncInfo = subscriber.getSyncInfo(resource,r1, r2);
          SyncInfoCompareInput comparedialog = new SyncInfoCompareInput("diff:", syncInfo);
          
          CompareUI.openCompareEditor(comparedialog);            
          
			  }
        catch (TeamException e)
        {
        	MercurialEclipsePlugin.logError(e);
//          e.printStackTrace();
        }

				
				
			}
		};

	}

	private void hookDoubleClickAction() 
	{
	  viewer.addDoubleClickListener(new IDoubleClickListener() 
  		{
  			public void doubleClick(DoubleClickEvent event) 
  			{
  				doubleClickAction.run();
  			}
  		});
	}

	private void showMessage(String message) 
	{
		MessageDialog.openInformation(viewer.getControl().getShell(),"ChangLog View",	message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
    public void setFocus() 
	{
//		viewer.getControl().setFocus();
	}
}