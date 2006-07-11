/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2006-jun-08
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * @author zingo
 *
 */
public class DecoratorStatus extends LabelProvider implements ILightweightLabelDecorator
{

  /**
   * 
   */
  public DecoratorStatus()
  {
    super();
//    System.out.println("MercurialEclipsePlugin:DecoratorStatus.DecoratorStatus()");
  }
   
  public static void refresh() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    String decoratorId = DecoratorStatus.class.getName();
    workbench.getDecoratorManager().update( decoratorId );
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
   */
  public void decorate(Object element, IDecoration decoration)
  {
    IResource objectResource;
    IProject objectProject;
    RepositoryProvider RepoProvider;
    
    objectResource = (IResource) element;
    if (objectResource == null)
    {
      return ;
    }
    
    objectProject = objectResource.getProject();
    if( RepositoryProvider.isShared( objectProject ) ) 
    {
      RepoProvider = RepositoryProvider.getProvider( objectProject );
      if( ! (RepoProvider instanceof MercurialTeamProvider))
      {
        //Resource not unsing this plugin
        return;
      }
    }

    
    // Decorating a Project   
    if (objectResource.getType() == IResource.PROJECT)
    {
//        decoration.addSuffix( "{PROJECT}" );
    }

    // Decorating a Folder
    if (objectResource.getType() == IResource.FOLDER)
    {
      // Folders should not be decorated..
//      decoration.addSuffix( "{FOLDER}" );
    }
    
    if (objectResource.getType() == IResource.FILE)
    {
      // Only files are decorated
//      decoration.addPrefix("{nofile->}" );
      
      IProject proj;
      String Repository;
      String FullPath;
      proj=objectResource.getProject();
      Repository=MercurialUtilities.getRepositoryPath(proj);
      if(Repository==null)
      {
        Repository="."; //never leave this empty add a . to point to current path
      }
      //Setup and run command
//        System.out.println("hg --cwd " + Repository + " status");

      FullPath=( objectResource.getLocation() ).toString();
      
      String launchCmd[] = { MercurialUtilities.getHGExecutable(),"--cwd", Repository ,"status", FullPath };
      String output=MercurialUtilities.ExecuteCommand(launchCmd,false);
      if(output!=null)
      {
        if(output.length()!=0)
        {
//        decoration.addSuffix( "{" + output.substring(0,1)  + "}" );
//          System.out.println("MercurialEclipsePlugin:DecoratorStatus.decorate(" + element.toString() + ", "+ output.substring(0,1) + ")");
          decoration.addOverlay(DecoratorImages.getImageDescriptor(output));
        }
        else
        {
          //Managed and unchanged (No output from status)
//          System.out.println("MercurialEclipsePlugin:DecoratorStatus.decorate(" + element.toString() + ", No output (managed?))");
          decoration.addOverlay(DecoratorImages.managedDescriptor);      
        }
      }  
    }
    
//    System.out.println("MercurialEclipsePlugin:DecoratorStatus.decorate(" + element.toString() + ", "+ decoration.toString() + ")");
  }
  
  
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
   */
  public void addListener(ILabelProviderListener listener)
  {
//    System.out.println("MercurialEclipsePlugin:DecoratorStatus.addListener()");

  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
   */
  public void dispose()
  {
//    System.out.println("MercurialEclipsePlugin:DecoratorStatus.dispose()");

  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
   */
  public boolean isLabelProperty(Object element, String property)
  {
//    System.out.println("MercurialEclipsePlugin:DecoratorStatus.isLabelProperty()");
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
   */
  public void removeListener(ILabelProviderListener listener)
  {
//    System.out.println("MercurialEclipsePlugin:DecoratorStatus.removeListener()");

  }

}
