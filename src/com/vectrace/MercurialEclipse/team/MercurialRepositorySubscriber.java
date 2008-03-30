/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;
import org.eclipse.team.core.variants.ThreeWaySynchronizer;

public class MercurialRepositorySubscriber extends ThreeWaySubscriber 
{

  public MercurialRepositorySubscriber()
  {
    super(new ThreeWaySynchronizer(new QualifiedName(MercurialTeamProvider.ID,"MercurialEclipse-sync")));
  }
  
  @Override
public String getName()
  {
    return "MercurialRepositorySubscriber";
  }

  @Override
public boolean isSupervised(IResource resource) throws TeamException
  {
    RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
    if (provider != null && provider instanceof MercurialTeamProvider) 
    {
      return true;
    }
    return false;
  }

  @Override
public IResource[] members(IResource resource) throws TeamException 
  {
    try 
    {
      if(resource.getType() == IResource.FILE)
      {
        return new IResource[0];
      }
      IContainer container = (IContainer)resource;
      ArrayList<IResource> existingChildren = new ArrayList<IResource>(Arrays.asList(container.members()));
      existingChildren.addAll(  Arrays.asList(container.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, null)));
      return existingChildren.toArray(new IResource[existingChildren.size()]);
    } 
    catch (CoreException e) 
    {
      throw TeamException.asTeamException(e);
    }
  }
  @Override
public IResource[] roots() 
  {
    ArrayList<IResource> ret = new ArrayList<IResource>();
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
    return ret.toArray(new IProject[ret.size()]);
  }

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
            
      SyncInfo info = new SyncInfo(resourceLocal, fileHistBase,fileHistRemote, getResourceComparator());
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

  @Override
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
    return new MercurialFileHistoryVariant(new IStorageMercurialRevision(resource));
  }

}
