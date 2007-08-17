package com.vectrace.MercurialEclipse.views;


import java.util.Arrays;
import java.util.Vector;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
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
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeLog;

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
//	private TableViewer viewer;
  private ChangeLog changeLog;
  private String fullPath;
  private Table changeLogTable;
	
	/*
	private Action action1;
	private Action action2;
	private Action doubleClickAction;
*/

  
//  private ChangeLogViewContentProvider changeLogViewContentProvider;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
/*	 
	class ChangeLogViewContentProvider implements IStructuredContentProvider
	{
    private String fullPath;
//	  private String changeLog;
	  
		public void inputChanged(Viewer v, Object oldInput, Object newInput) 
		{

		  if (newInput != null)
	     {
	      if((newInput instanceof ChangeSet) == true)
	      {
	        ChangeSet changeSet = (ChangeSet) newInput;
//	        changeSet.addChangeListener(this);
	      }	    
	     }
       if (oldInput != null)
       {
         if((oldInput instanceof ChangeSet) == true)
         {
           ChangeSet changeSet = (ChangeSet) oldInput;
//           changeSet.removeChangeListener(this);
         }     
       }
		}
		public void dispose() 
		{
		  if(changeLog != null)
		  {
//  		  changeLog.removeChangeListener(this);
		  }
		}

    public void updateTask(ChangeSet changeSet) 
    {
      viewer.update(changeSet, null); 
    }
    
    public void addTask(ChangeSet changeSet) 
    {
      viewer.add(changeSet);
  }
		
		public Object[] getElements(Object parent) 
		{
		  if(changeLog == null)
		  {
		    changeLog=new ChangeLog(null);
		  }
      System.out.println("ChangeLogViewContentProvider::getElements()");
      
      return changeLog.getChangeLog().toArray();
		}

    public void setChangeLog(String fullpath,String changelog)
    {
      System.out.println("ChangeLogViewContentProvider::setChangeLog()");
      this.fullPath=fullpath;
      changeLog=new ChangeLog(changelog);
    }

	
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider 
	{
	  
		public String getColumnText(Object obj, int index) 
		{
		  String ret;
		  
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
          ret= changeSet.getUser();
          break;
        case 2:
          ret= changeSet.getDate();
          break;
        case 3:
          ret= changeSet.getFiles();
          break;
        case 4:
          ret= changeSet.getDescription();
          break;
        default:
          ret= "Hello=" + index;
          break;
      }
      System.out.println("ViewLabelProvider::getColumnText(" + changeSet.getChangeset() +"," + index + ")=" + ret);

      return ret; //tableViewer.setInput(taskList);
			//return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) 
		{
      System.out.println("ViewLabelProvider::getColumnImage(obj," + index + ")");
			return null; //getImage(obj);
		}
		
    public Image getImage(Object obj) 
		{
			return null;//PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}

    public void addListener(ILabelProviderListener listener)
    {
    }

    public void dispose()
    {
    }

    public boolean isLabelProperty(Object element, String property)
    {
      return false;
    }

    public void removeListener(ILabelProviderListener listener)
    {
    }
	}
*/
	class NameSorter extends ViewerSorter 
	{
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
	public void createPartControl(Composite parent) 
	{
//	  changeLogViewContentProvider = new ChangeLogViewContentProvider(); 
	  
    changeLogTable = new Table(parent,SWT.SINGLE | SWT.H_SCROLL| SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.HIDE_SELECTION);

//    GridData gridData = new GridData(GridData.FILL_BOTH);
//    gridData.grabExcessVerticalSpace = true;
//    gridData.horizontalSpan = 3;
//    changeLogTable.setLayoutData(gridData);
    
    changeLogTable.setLinesVisible(true);
    changeLogTable.setHeaderVisible(true);
//    changeLogTable.setBounds(10, 340, 270, 200);

    TableLayout layout = new TableLayout();    

       
    TableColumn changesetTableColumn = new TableColumn(changeLogTable,SWT.LEFT);
//    changesetTableColumn.setResizable(true);
    changesetTableColumn.setText("Changeset");
    changesetTableColumn.setWidth(80);
    layout.addColumnData(new ColumnPixelData(80, true));
    TableColumn tagTableColumn = new TableColumn(changeLogTable,SWT.LEFT);
    tagTableColumn.setText("Tag");
    tagTableColumn.setWidth(80);
    layout.addColumnData(new ColumnPixelData(80, true));
    TableColumn userTableColumn = new TableColumn(changeLogTable,SWT.LEFT);
    userTableColumn.setText("User");
    userTableColumn.setWidth(80);
    layout.addColumnData(new ColumnPixelData(80, true));
    TableColumn dateTableColumn = new TableColumn(changeLogTable,SWT.LEFT);
    dateTableColumn.setText("Date");
    dateTableColumn.setWidth(80);
    layout.addColumnData(new ColumnPixelData(80, true));
    TableColumn filesTableColumn = new TableColumn(changeLogTable,SWT.LEFT);
    filesTableColumn.setText("Files");
    filesTableColumn.setWidth(80);
    layout.addColumnData(new ColumnPixelData(80, true));
    TableColumn descriptionTableColumn = new TableColumn(changeLogTable,SWT.LEFT);
    descriptionTableColumn.setText("Description");
    descriptionTableColumn.setWidth(80);
    layout.addColumnData(new ColumnPixelData(80, true));

    changeLogTable.setLayout(layout);
    
//    viewer = new TableViewer(changeLogTable, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);

//    GridData commitFilesData = new GridData(GridData.FILL_BOTH);
//    commitFilesData.widthHint = 280;
//    commitFilesData.heightHint = 300;
//    viewer.getTable().setLayoutData(commitFilesData);
    
//    viewer.setUseHashlookup(true);
//    viewer.setContentProvider(changeLogViewContentProvider);
//    viewer.setLabelProvider(new ViewLabelProvider());
//    viewer.setSorter(new NameSorter());
//    viewer.setInput(new ChangeLog(null));   // getViewSite());

/*    
    
    TableItem item1 = new TableItem(changeLogTable,SWT.NONE);
    item1.setText(new String[] {"Sarah","15","390 Sussex Ave"} );
    TableItem item2 = new TableItem(changeLogTable,SWT.NONE);
    item2.setText(new String[] {"Joseph","56","7 streeetelistreet"} );
    TableItem item3 = new TableItem(changeLogTable,SWT.NONE);
    item3.setText(new String[] {"Anders","34","8 gatan"} );
*/
    
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	 public void showChangeLog(String fullpath,String changelog_string) 
	 {
//	   changeLogViewContentProvider.setChangeLog(fullpath,changelog_string);
//     viewer.setInput(new ChangeLog(changelog_string));   // getViewSite());
//	   viewer.refresh();

	   //System.out.println("showChangeLog()");

     this.fullPath=fullpath;
	   changeLog = new ChangeLog(changelog_string);
	   Vector<ChangeSet> changeSets = changeLog.getChangeLog();
	   ChangeSet changeSet;
	   TableItem item;
	   
	   changeLogTable.removeAll();
	   
	   for(int index=0;index<changeSets.size();index++)
	   {
	     changeSet=changeSets.get(index);
	     item = new TableItem(changeLogTable,SWT.NONE);
	     item.setText(new String[] {changeSet.getChangeset(),changeSet.getTag(),changeSet.getUser(),changeSet.getDate(),changeSet.getFiles(),changeSet.getDescription()} );
	   }
	   

	 
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
//		Menu menu = menuMgr.createContextMenu(viewer.getControl());
//		viewer.getControl().setMenu(menu);
//		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() 
	{
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) 
	{
/*
  
 		manager.add(action1);
		manager.add(new Separator());
		manager.add(action2);
*/
	}

	private void fillContextMenu(IMenuManager manager) 
	{
/*
	  manager.add(action1);
		manager.add(action2);
*/
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

	}
	
	private void fillLocalToolBar(IToolBarManager manager) 
	{
/*
 
 		manager.add(action1);
		manager.add(action2);
*/
	}

	private void makeActions() 
	{
/*
	  action1 = new Action() 
		{
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
			public void run() 
			{
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				showMessage("Double-click detected on "+obj.toString());
			}
		};
*/
	}

	private void hookDoubleClickAction() 
	{
/*
	  viewer.addDoubleClickListener(new IDoubleClickListener() 
  		{
  			public void doubleClick(DoubleClickEvent event) 
  			{
  				doubleClickAction.run();
  			}
  		});
*/
	}
	private void showMessage(String message) 
	{
//		MessageDialog.openInformation(viewer.getControl().getShell(),"ChangLog View",	message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() 
	{
//		viewer.getControl().setFocus();
	}
}