/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2007 Aug 29
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
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
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

import com.vectrace.MercurialEclipse.actions.OpenMercurialRevisionAction;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author zingo
 *
 */
public class MercurialHistoryPage extends HistoryPage
{

  private TableViewer viewer;
//  private ChangeLog changeLog;
  private IResource resource;
  private Table changeLogTable;
  private ChangeLogContentProvider changeLogViewContentProvider;
  private Composite composite;
  MercurialHistory mercurialHistory;
  IFileRevision[] entries;
  OpenMercurialRevisionAction openAction;
  
  private RefreshMercurialHistory refreshFileHistoryJob;

  private class RefreshMercurialHistory extends Job 
  {
    MercurialHistory mercurialHistory;

    public RefreshMercurialHistory() 
    {
      super("Fetching Mercurial revisions...");  //$NON-NLS-1$
    }

    public void setFileHistory(MercurialHistory mercurialHistory) 
    {
      this.mercurialHistory = mercurialHistory;
    }

    public IStatus run(IProgressMonitor monitor) 
    {

      IStatus status = Status.OK_STATUS;

      if (mercurialHistory != null ) 
      {
        try
        {
          mercurialHistory.refresh(monitor);
        }
        catch (CoreException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        //Internal code used for convenience - you can use 
        //your own here
        Utils.asyncExec(new Runnable() 
          {
            public void run() 
            {
              viewer.setInput(mercurialHistory);
//              changeLogViewContentProvider.setChangeLog(mercurialFileHistory);
            }
          }, viewer);
      }

      return status;
    }
  }
  
  
  class ChangeLogContentProvider implements IStructuredContentProvider
  { 
        
    public void inputChanged(Viewer v, Object oldInput, Object newInput) 
    {
      entries = null;
    }
    
    public void dispose() 
    {
    }

    public Object[] getElements(Object parent) 
    {
//      System.out.println("Object[] ChangeLogViewContentProvider::getElements()");      
//      return changeLog.getChangeLog().toArray();
      if (entries != null)
        return entries;

      final IFileHistory fileHistory = (IFileHistory) parent;
      entries = fileHistory.getFileRevisions();

      return entries;
    }
/*
    public void setChangeLog(IResource in_resource)
    {
//      System.out.println("ChangeLogViewContentProvider::setChangeLog()");

      if(isValidInput(in_resource))
      {
        resource=in_resource;        
        changeLog.ChangeChangeLog(in_resource);
        viewer.refresh();
      }      
    }
*/
  }

  class ChangeSetLabelProvider extends LabelProvider implements ITableLabelProvider
  {
        
    public String getColumnText(Object obj, int index) 
    {
      String ret;

//      System.out.println("ViewLabelProvider::getColumnText(obj," + index + ")");

      if((obj instanceof MercurialRevision) != true)
      {
        return "Type Error";
      }

      MercurialRevision mercurialFileRevision = (MercurialRevision) obj;         
      ChangeSet changeSet = mercurialFileRevision.getChangeSet();
     
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

      if(((e1 instanceof MercurialRevision) != true) || ((e1 instanceof MercurialRevision) != true))
      {
        return super.compare(viewer, e1, e2);
      }

      MercurialRevision mercurialFileRevision1 = (MercurialRevision) e1;         
      MercurialRevision mercurialFileRevision2 = (MercurialRevision) e2;     
          
      int value1=mercurialFileRevision1.getChangeSet().getChangesetIndex();
      int value2=mercurialFileRevision2.getChangeSet().getChangesetIndex();
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

  public MercurialHistoryPage(IResource resource)
  {
    super();
    if(isValidInput(resource))
    {
      this.resource = resource;
    }
  }

  
  
  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.HistoryPage#inputSet()
   */
  @Override
  public boolean inputSet()
  {
//    System.out.println("MercurialHistoryPage::inputSet()");
    
    
    if(isValidInput(resource))
    {
      mercurialHistory = new MercurialHistory((IFile)resource);
      refresh();
      return true;
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.Page#createControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createControl(Composite parent)
  {
//    System.out.println("MercurialHistoryPage::createControl()");

    
    composite = new Composite(parent, SWT.NONE);
    GridLayout layout0 = new GridLayout();
    layout0.marginHeight = 0;
    layout0.marginWidth = 0;
    composite.setLayout(layout0);
    GridData data = new GridData(GridData.FILL_BOTH);
    data.grabExcessVerticalSpace = true;
    composite.setLayoutData(data);

    
    viewer = new TableViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
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
//    changeLog=new ChangeLog();
//    viewer.setInput(changeLog);   // getViewSite());
    
    contributeActions();
  }


  private void contributeActions() 
  {
    openAction = new OpenMercurialRevisionAction("Open");  //$NON-NLS-1$
    viewer.getTable().addSelectionListener(new SelectionAdapter() 
      {
        public void widgetSelected(SelectionEvent e) 
        {
          openAction.selectionChanged((IStructuredSelection) viewer.getSelection());
        }
      });
    openAction.setPage(this);
    //Contribute actions to popup menu
    MenuManager menuMgr = new MenuManager();
    Menu menu = menuMgr.createContextMenu(viewer.getTable());
    menuMgr.addMenuListener(new IMenuListener() 
    {
      public void menuAboutToShow(IMenuManager menuMgr) 
      {
        menuMgr.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
        menuMgr.add(openAction);
      }
    });
    menuMgr.setRemoveAllWhenShown(true);
    viewer.getTable().setMenu(menu);
  }
  
  
  /* (non-Javadoc)
   * @see org.eclipse.ui.part.Page#getControl()
   */
  @Override
  public Control getControl()
  {
//    System.out.println("MercurialHistoryPage::getControl()");
    return composite;
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.part.Page#setFocus()
   */
  @Override
  public void setFocus()
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistoryPage::setFocus()");

  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPage#getDescription()
   */
  public String getDescription()
  {
//    System.out.println("MercurialHistoryPage::getDescription()");
    return resource.getFullPath().toOSString();
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPage#getName()
   */
  public String getName()
  {
//    System.out.println("MercurialHistoryPage::getName()");
    return resource.getFullPath().toOSString();
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPage#isValidInput(java.lang.Object)
   */
  public boolean isValidInput(Object object)
  {
//    System.out.println("MercurialHistoryPage::isValidInput()");
    if (object instanceof IResource && ((IResource) object).getType() == IResource.FILE) 
    {
      RepositoryProvider provider = RepositoryProvider.getProvider(((IFile) object).getProject());
      if (provider instanceof MercurialTeamProvider)
      {
        return true;
      }
    }
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.history.IHistoryPage#refresh()
   */
  public void refresh()
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistoryPage::refresh()");
    if(isValidInput(resource))
    {
//      changeLogViewContentProvider.setChangeLog(resource);

      if (refreshFileHistoryJob == null)
      {
        refreshFileHistoryJob = new RefreshMercurialHistory();
      }

      if (refreshFileHistoryJob.getState() != Job.NONE) 
      {
        refreshFileHistoryJob.cancel();
      }
      refreshFileHistoryJob.setFileHistory(mercurialHistory);
      IHistoryPageSite parentSite = getHistoryPageSite();
      //Internal code used for convenience - you can use your own here
      IWorkbenchPart part = parentSite.getPart();
      IWorkbenchPartSite site;
      if (part != null)
      {
        site= part.getSite();
      }
      else
      {
        site=null;
      }

      Utils.schedule(refreshFileHistoryJob, site);
    
    
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter)
  {
    // TODO Auto-generated method stub
    System.out.println("MercurialHistoryPage::getAdapter()");
    return null;
  }
}
