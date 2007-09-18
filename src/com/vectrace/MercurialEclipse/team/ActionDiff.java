/**
 * com.vectrace.MercurialEclipse (c) Vectrace Jan 31, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.SyncInfoCompareInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.compare.CompareUI;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author zingo
 *
 */
public class ActionDiff implements IWorkbenchWindowActionDelegate 
{
   
//  private IWorkbenchWindow window;
//    private IWorkbenchPart targetPart;
    private IStructuredSelection selection;
    
	public ActionDiff() 
	{
		super();
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() 
	{

	}


	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) 
	{
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
//		String Repository;
    
    proj=MercurialUtilities.getProject(selection);
//		Repository=MercurialUtilities.getRepositoryPath(proj);
//		if(Repository==null)
//		{
//			Repository="."; //never leave this empty add a . to point to current path
//		}

		Object obj;
	  Iterator itr; 
    // the last argument will be replaced with a path
    itr=selection.iterator();
    while(itr.hasNext())
    {
    	obj=itr.next();
    	if (obj instanceof IResource)
    	{
    	  
    	  SyncInfoCompareInput comparedialog = getCompareInput((IResource)obj);
    	  if(comparedialog!=null)
    	  {
          CompareUI.openCompareEditor(comparedialog);
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

  public SyncInfoCompareInput getCompareInput(IResource obj)
  {
    //Setup and run command identify, this us used to get the base changeset to diff against
    //tip can't be used since work can be done in older revison ( hg up <old rev> )
    //String FullPath = ( ((IResource) obj).getLocation() ).toString();
   
    String launchCmd[] = { MercurialUtilities.getHGExecutable(),"identify"};
    File workingDir=MercurialUtilities.getWorkingDir( obj );

    IProject proj= obj.getProject();

    
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

      MercurialRepositorySubscriber subscriber = new MercurialRepositorySubscriber();
      SyncInfo syncInfo = subscriber.getSyncInfo((IResource) obj, (IStorage) obj, new IStorageMercurialRevision( proj, (IResource) obj, changeset));
      SyncInfoCompareInput comparedialog = new SyncInfoCompareInput("diffelidiff", syncInfo);
      return comparedialog;
    } 
    catch (HgException e)
    {
      System.out.println(e.getMessage());
    } 
    catch (TeamException e)
    {
      e.printStackTrace();
    }
    return null;
  }
	
}
