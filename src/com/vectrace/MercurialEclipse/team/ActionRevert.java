/**
 * com.vectrace.MercurialEclipse (c) Vectrace Sep 12, 2006
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.dialogs.CommitResource;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceUtil;
import com.vectrace.MercurialEclipse.dialogs.RevertDialog;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author zingo
 * 
 */

public class ActionRevert implements IWorkbenchWindowActionDelegate 
{
    private IWorkbenchWindow window;
    // private IWorkbenchPart targetPart;
    private IStructuredSelection selection;

    public ActionRevert() 
    {
        super();
    }

    /**
     * We can use this method to dispose of any system resources we previously
     * allocated.
     * 
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() 
    {
    }

    /**
     * We will cache window object in order to be able to provide parent shell
     * for the message dialog.
     * 
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) 
    {
      this.window = window;
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     * 
     * @see IWorkbenchWindowActionDelegate#run
     */

    public void run(IAction action) 
    {
        Shell shell;
        IWorkbench workbench;
        IProject proj;
        proj = MercurialUtilities.getProject(selection);

        //System.out.println("Revert:");

        Object obj;
        
        String repository;
        // IProject proj;
//        System.out.println("Revert in runnable");
        proj = MercurialUtilities.getProject(selection);
        repository = MercurialUtilities.getRepositoryPath(proj);
        if (repository == null) 
        {
            repository = "."; // never leave this empty add a . to point to
            // current path
        }

        // do the actual work in here
        Iterator itr = selection.iterator();
        List<IResource> resources = new ArrayList<IResource>();
        while (itr.hasNext()) 
        {
            obj = itr.next();

            if (obj instanceof IResource) 
            {
                IResource resource = (IResource) obj;
                if (MercurialUtilities.isResourceInReposetory(resource, true) == true) 
                {
                    resources.add(resource);
                }
            }
        }

        CommitResource[] commitResources = new CommitResourceUtil(proj).getCommitResources(resources.toArray(new IResource[resources.size()]));
     
        //System.out.println("commitResources.length=" + Integer.toString(commitResources.length));

        //Check to see if there are any that are untracked.
        int count=0;
        for(int i=0;i<commitResources.length;i++)
        {
          if(!commitResources[i].getStatus().startsWith(CommitDialog.FILE_UNTRACKED))
          {
            count++;
          }
        }

        if(count!=0)
        {
          RevertDialog chooser = new RevertDialog(Display.getCurrent().getActiveShell());
          chooser.setFiles(commitResources);
          if (chooser.open() == Window.OK) 
          {
              final List<CommitResource> result = chooser.getSelection();
              new SafeWorkspaceJob("Revert files") 
              {
                  @Override
                  protected IStatus runSafe(IProgressMonitor monitor) 
                  {
                      doRevert(monitor,result);
                      return Status.OK_STATUS;
                  }
              }.schedule();
          }
        }
        else
        {
          //Get shell & workbench
          if((window !=null) && (window.getShell() != null))
          {
            shell=window.getShell();
          }
          else
          {
            workbench = PlatformUI.getWorkbench();
            shell = workbench.getActiveWorkbenchWindow().getShell();
          }
          MessageDialog.openInformation(shell, "Mercurial Eclipse hg revert", "No files to revert!");
        }
    }

    private void doRevert(IProgressMonitor monitor, List<CommitResource> resources) 
    {
        
        // the last argument will be replaced with a path
        String launchCmd[] = { MercurialUtilities.getHGExecutable(), "revert","--","" };
        for (Iterator iter = resources.iterator(); iter.hasNext();) 
        {
            IResource resource = ((CommitResource) iter.next()).getResource();
            // Resource could be inside a link or something do nothing
            // in the future this could check is this is another repository

            // Setup and run command
            File workingDir = MercurialUtilities.getWorkingDir(resource);
            launchCmd[3] = MercurialUtilities.getResourceName(resource);
            // System.out.println("Revert = " + FullPath);
            // IResourceChangeEvent event = new IResourceChangeEvent();
            try 
            {
                MercurialUtilities.ExecuteCommand(launchCmd, workingDir, true);
            } 
            catch (HgException e) 
            {
               MercurialEclipsePlugin.logError(e);
            } 
        }
        for (Iterator iter = resources.iterator(); iter.hasNext();) 
        {
            IResource resource = ((CommitResource) iter.next()).getResource();
            try 
            {
                resource.touch(monitor);
            } 
            catch (CoreException e) 
            {
                MercurialEclipsePlugin.logError(e);
            }
        }
        
        // notify();
        new SafeUiJob("Updating status") 
        {
            @Override
            protected IStatus runSafe(IProgressMonitor monitor) 
            {
                DecoratorStatus.refresh();
                return super.runSafe(monitor);
            }
        }.schedule();
    }

    /**
     * Selection in the workbench has been changed. We can change the state of
     * the 'real' action here if we want, but this can only happen after the
     * delegate has been created.
     * 
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection in_selection) 
    {
        if (in_selection != null && in_selection instanceof IStructuredSelection) 
        {
            selection = (IStructuredSelection) in_selection;
        }
    }

}
