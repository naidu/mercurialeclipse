/*******************************************************************************
 * Copyright (c) 2008 Vectrace (Zingo Andersen) 
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;

public class MercurialRepositorySubscriber extends ThreeWaySubscriber 
{

  public class LocalHistoryVariantComparator implements IResourceVariantComparator 
  {
    /* Compare current with newer/older */
    public boolean compare(IResource local, IResourceVariant remote) 
    {
      return false;
    }

    /* Compare newer/older with newer/older */
    public boolean compare(IResourceVariant base, IResourceVariant remote) 
    {
      return true;
    }

    public boolean isThreeWay() 
    {
      return false;
    }
  }
/*
  public class LocalHistorySyncInfo extends SyncInfo 
  {
    
    public LocalHistorySyncInfo(IResource local, IResourceVariant remote1, IResourceVariant remote2, IResourceVariantComparator comparator) 
    {
      super(local, remote1, remote2, comparator);
    }

    protected int calculateKind() throws TeamException 
    {
       if (getRemote() == null)
        return IN_SYNC;
      else
        return super.calculateKind();
    }
  }

*/
  
  
  LocalHistoryVariantComparator comparatorObj; 
  
  public MercurialRepositorySubscriber()
  {
    super(new ThreeWaySynchronizer(new QualifiedName(MercurialTeamProvider.ID,"MercurialEclipse-sync")));
    comparatorObj = new LocalHistoryVariantComparator();
  }
  
  public String getName()
  {
    return "MercurialRepositorySubscriber";
  }

  public boolean isSupervised(IResource resource) throws TeamException
  {
    // TODO Auto-generated method stub
    RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
    if (provider != null && provider instanceof MercurialTeamProvider) 
    {
      return true;
    }
    return false;
  }

  public IResource[] members(IResource resource) throws TeamException 
  {
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
    List ret = new ArrayList();
    IProject[] allProjects;
    allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    for (int i = 0; i < allProjects.length; i++) 
    {
      IProject oneProject = allProjects[i];
      if(oneProject.isAccessible()) 
      {
        RepositoryProvider provider = RepositoryProvider.getProvider(oneProject);
        if (provider != null && provider instanceof MercurialTeamProvider)  
        {
          ret.add(oneProject);
        }
      }
    }
    return (IProject[]) ret.toArray(new IProject[ret.size()]);
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.subscribers.Subscriber#getSyncInfo(org.eclipse.core.resources.IResource)
   */
//  public SyncInfo getSyncInfo(IResource resource) throws TeamException
//  {
    // TODO Auto-generated method stub
//    return getSyncInfo(resource,null,null);
//  }

  public SyncInfo getSyncInfo(IResource resourceLocal, IStorage storageBase, IStorage storageRemote) throws TeamException 
  {
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
      MercurialFileHistoryVariant fileHistBase=null;
      MercurialFileHistoryVariant fileHistRemote=null;
      if(storageBase != null)
      {
        fileHistBase = new MercurialFileHistoryVariant(storageBase);
      }
      if(storageRemote != null)
      {
        fileHistRemote = new MercurialFileHistoryVariant(storageRemote);
      }
      
      SyncInfo info = new SyncInfo(resourceLocal, fileHistBase,fileHistRemote, comparatorObj);
      info.init();
      return info;
    } 
    catch (CoreException e) 
    {
      throw TeamException.asTeamException(e);
    }
  }

//  public IResourceVariantComparator getResourceComparator()
//  {
//    return comparatorObj;
//  }

  public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected ThreeWayRemoteTree createRemoteTree()
  {
    return new MercurialRemoteTree(this);
  }

  @Override
  public IResourceVariant getResourceVariant(IResource resource, byte[] bytes) throws TeamException
  {
    if (bytes == null)
    {
      return null;
    }
    return new MercurialFileHistoryVariant(new IStorageMercurialRevision(resource.getProject(),resource,"tip"));
  }

}
