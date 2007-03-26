/**
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.compare.CompareUI;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author zingo
 *
 */
public class ActionDiff implements IWorkbenchWindowActionDelegate {
 

  public class FileHistoryVariant implements IResourceVariant {
    private final IStorage myIStorage;

    public FileHistoryVariant(IStorage res) 
    {
     this.myIStorage = res;
//     System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::FileHistoryVariant()" );
    }

    
    public String getName() 
    {
//      System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getName()" );
      return myIStorage.getName();
    }

    public boolean isContainer() 
    {
//      System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::isContainer()" );
      return false;
    }

    public IStorage getStorage(IProgressMonitor monitor) throws TeamException 
    {
//      System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getStorage()" );
      return myIStorage;
    }

    public String getContentIdentifier() 
    {
//      System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getContentIdentifier()" );
      return myIStorage.getFullPath().toString();
    }

    public byte[] asBytes() 
    {
//      System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::asBytes() what is this for?" );
      return null;
    }
  }
  
   
  public class MyRepositorySubscriber extends Subscriber 
  {

    public class LocalHistoryVariantComparator implements IResourceVariantComparator {
      public boolean compare(IResource local, IResourceVariant remote) {
        return false;
      }

      public boolean compare(IResourceVariant base, IResourceVariant remote) {
        return false;
      }

      public boolean isThreeWay() {
        return false;
      }
    }

    
    LocalHistoryVariantComparator comparatorObj; 
    
    public MyRepositorySubscriber()
    {
      comparatorObj = new LocalHistoryVariantComparator();
    }
    
    public String getName()
    {
      // TODO Auto-generated method stub
      return "MyRepositorySubscriber";
    }

    public boolean isSupervised(IResource resource) throws TeamException
    {
      // TODO Auto-generated method stub
      return false;
    }

    public IResource[] members(IResource resource) throws TeamException {
      try 
      {
        if(resource.getType() == IResource.FILE)
        {
          return new IResource[0];
        }
        IContainer container = (IContainer)resource;
        List existingChildren = new ArrayList(Arrays.asList(container.members()));
        existingChildren.addAll(  Arrays.asList(container.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, null)));
        return (IResource[]) existingChildren.toArray(new IResource[existingChildren.size()]);
      } 
      catch (CoreException e) 
      {
        throw TeamException.asTeamException(e);
      }
    }

    public IResource[] roots()
    {
      // TODO Auto-generated method stub
      return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.core.subscribers.Subscriber#getSyncInfo(org.eclipse.core.resources.IResource)
     */
    public SyncInfo getSyncInfo(IResource resource) throws TeamException
    {
      // TODO Auto-generated method stub
      return getSyncInfo(resource,null,null);
    }

    public SyncInfo getSyncInfo(IResource resource, IStorage r1, IStorage r2) throws TeamException {
      try 
      {
  /*
        IResourceVariant variant = null;
        if(resource.getType() == IResource.FILE) {
          IFile file = (IFile)resource;
          IFileState[] states = file.getHistory(null);
          if(states.length > 0) {
            // last state only
            variant = new LocalHistoryVariant(states[0]);
          } 
        }
        */
        FileHistoryVariant fileHist1=null;
        FileHistoryVariant fileHist2=null;
        if(r1 != null)
        {
          fileHist1 = new FileHistoryVariant(r1);
        }
        if(r2 != null)
        {
          fileHist2 = new FileHistoryVariant(r2);
        }
        
        SyncInfo info = new SyncInfo(resource, fileHist1,fileHist2, comparatorObj);
        info.init();
        return info;
      } catch (CoreException e) {
        throw TeamException.asTeamException(e);
      }
    }

    public IResourceVariantComparator getResourceComparator()
    {
      // TODO Auto-generated method stub
      return comparatorObj;
    }

    public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException
    {
      // TODO Auto-generated method stub
      
    }

  }
  
//  private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
	public ActionDiff() {
		super();
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {

	}


	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
//		System.out.println("ActionDiff:init(window)");
//		this.window = window;
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	

	public void run(IAction action) 
	{
		IProject proj;
		String Repository;
    
    proj=MercurialUtilities.getProject(selection);
		Repository=MercurialUtilities.getRepositoryPath(proj);
		if(Repository==null)
		{
			Repository="."; //never leave this empty add a . to point to current path
		}

		Object obj;
	  Iterator itr; 
    // the last argument will be replaced with a path
    itr=selection.iterator();
    while(itr.hasNext())
    {
    	obj=itr.next();
    	if (obj instanceof IResource)
    	{

        //Setup and run command identify, this us used to get the base changeset to diff against
        //tip can't be used since work can be done in older reviison ( hg up <old rev> )
        //String FullPath = ( ((IResource) obj).getLocation() ).toString();
       
        String launchCmd[] = { MercurialUtilities.getHGExecutable(),"identify"};
        File workingDir=MercurialUtilities.getWorkingDir((IResource) obj );

        
        try
        {
          String changeset = MercurialUtilities.ExecuteCommand(launchCmd,workingDir ,false);

          // It consists of the revision id (hash), optionally a '+' sign
          // if the working tree has been modified, followed by a list of tags.
          // => we need to strip it ...

          if (changeset.indexOf(" ") != -1) // is there a space?
          {
            changeset = changeset.substring(0, changeset.indexOf(" ")); // take the begining until the first space
          }
          if (changeset.indexOf("+") != -1) // is there a +?
          {
            changeset = changeset.substring(0, changeset.indexOf("+")); // take the begining until the first +
          }

          // Setup and run command diff

          MyRepositorySubscriber subscriber = new MyRepositorySubscriber();
          SyncInfo syncInfo = subscriber.getSyncInfo((IResource) obj, (IStorage) obj, new IStorageMercurialRevision( proj, (IResource) obj, changeset));
          SyncInfoCompareInput comparedialog = new SyncInfoCompareInput("diffelidiff", syncInfo);
          CompareUI.openCompareEditor(comparedialog);
        } 
        catch (HgException e)
        {
          System.out.println(e.getMessage());
        } 
        catch (TeamException e)
        {
          e.printStackTrace();
        }
      }
    }
	}
  
  
	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection in_selection) 
	{
		if( in_selection != null && in_selection instanceof IStructuredSelection )
		{
			selection = ( IStructuredSelection )in_selection;
		}
	}
	
}
