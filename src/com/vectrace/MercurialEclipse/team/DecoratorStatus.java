/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2006-jun-08
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author zingo
 * 
 */
public class DecoratorStatus extends LabelProvider implements ILightweightLabelDecorator, IResourceChangeListener 
{

  /** Used to store the last known status of a resource */
  private static Map<IResource, String> statusMap = new HashMap<IResource, String>();

  /** Used to store which projects have already been parsed */
  private static Set<IProject> knownStatus = new HashSet<IProject>();

  /**
   * 
   */
  public DecoratorStatus() 
  {
    super();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
  }
  /** 
   * Clears the known status of all resources and projects.
   * and calls for a update of decoration
   *  
   */
  public static void refresh() 
  {
    /* While this clearing of status is a "naive" implementation, it is simple. */
    IWorkbench workbench = PlatformUI.getWorkbench();
    String decoratorId = DecoratorStatus.class.getName();
    workbench.getDecoratorManager().update(decoratorId);
    statusMap.clear();
    knownStatus.clear();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object,
   *      org.eclipse.jface.viewers.IDecoration)
   */

  public void decorate(Object element, IDecoration decoration) 
  {
    if (!(element instanceof IResource)) 
    {
      return;
    }

    IResource objectResource = (IResource) element;
    IProject objectProject = objectResource.getProject();
    if (null == RepositoryProvider.getProvider(objectProject,MercurialTeamProvider.ID)) 
    {
      return;
    }

    if (MercurialUtilities.isResourceInReposetory(objectResource, true) != true) 
    {
      // Resource could be inside a link or something do nothing
      // in the future this could check is this is another repository
      // System.out.println("4
      // MercurialEclipsePlugin:DecoratorStatus.decorate()");
      return;
    }

    if (!knownStatus.contains(objectProject)) 
    {
       /* hg status on project (all files) instead of per file basis*/
      try 
      {
        refresh(objectProject);
      } 
      catch (HgException ex) 
      {
        MercurialEclipsePlugin.logError(ex);
        return;
      }
    }

    String output = statusMap.get(element);
    if (output != null) 
    {
      decoration.addOverlay(DecoratorImages.getImageDescriptor(output));
    } 
    else 
    {
      decoration.addOverlay(DecoratorImages.managedDescriptor);
    }
  }

  /**
  * @param project
  * @throws HgException
  */
  private void refresh(IProject project) throws HgException 
  {
    String output = MercurialUtilities.ExecuteCommand(getHgCommand(project), new File(getAbsolutePath(project).toOSString()), false);
    parseStatusCommand(project, output);
  }

  /**
  * @param output
  */
  private void parseStatusCommand(IProject ctr, String output) 
  {
    knownStatus.add(ctr);
    Scanner scanner = new Scanner(output);
    while (scanner.hasNext()) 
    {
      String status = scanner.next();
      String localName = scanner.nextLine();
      IResource member = ctr.getFile(localName.trim());

      statusMap.put(member, status);
      if (!status.startsWith("C")) 
      {
        IResource parent = member.getParent();
        statusMap.put(parent, "M");
      }
    }
  }

  private String[] getHgCommand(IProject project) 
  {
    return getHgCommand(project, new IResource[0]);
  }

  private String[] getHgCommand(IProject project, IResource[] resources) 
  {
    Assert.isNotNull(project);
    List<String> launchCmd = new ArrayList<String>();
    launchCmd.add(MercurialUtilities.getHGExecutable());
    launchCmd.add("status");
    // skip -A flag, use null as managed instead
    //		launchCmd.add("-A");

    for (IResource r : resources) 
    {
      launchCmd.add(r.getFullPath().toOSString());
    }
    return (String[]) launchCmd.toArray(new String[launchCmd.size()]);
  }

  /**
  * @param resource
  * @return
  */
  private IPath getAbsolutePath(final IResource resource) 
  {
    IPath root = resource.getWorkspace().getRoot().getRawLocation();
    IPath projectPath = root.append(resource.getFullPath());
    return projectPath;
  }

  /*
  * (non-Javadoc)
  * 
  * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
  */
  public void addListener(ILabelProviderListener listener) 
  {
  }

  /*
  * (non-Javadoc)
  * 
  * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
  */
  public void dispose() 
  {		
  ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
  }

  /*
  * (non-Javadoc)
  * 
  * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object,
  *      java.lang.String)
  */
  public boolean isLabelProperty(Object element, String property) 
  {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
   */
  public void removeListener(ILabelProviderListener listener) 
  {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
   */
  public void resourceChanged(final IResourceChangeEvent event) 
  {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) 
    {
      IResourceDelta[] children = event.getDelta().getAffectedChildren();
      for (IResourceDelta delta : children) 
      {
        IResource res = delta.getResource();
        if (null != RepositoryProvider.getProvider(res.getProject(),MercurialTeamProvider.ID)) 
        {
          // Atleast one resource in a project managed by MEP has
          // changed, schedule a refresh();

          new SafeUiJob("Update Decorations")
            {
              @Override
              protected IStatus runSafe(IProgressMonitor monitor) 
              {
                refresh();
                return super.runSafe(monitor);
              }
            }.schedule();
          return;
        }
      }
    }
  }
}
