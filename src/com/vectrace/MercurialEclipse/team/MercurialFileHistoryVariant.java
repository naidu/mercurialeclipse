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
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.CachedResourceVariant;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class MercurialFileHistoryVariant extends CachedResourceVariant 
{
  private final IStorage myIStorage;
  
  public MercurialFileHistoryVariant(IStorage res) 
  {
    this.myIStorage = res;
    //   System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::FileHistoryVariant()" );
  }

  
  public String getName() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getName()" );
    return myIStorage.getName();
  }

  public boolean isContainer() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::isContainer()" );
    if(myIStorage instanceof IStorageMercurialRevision)
    {
      IStorageMercurialRevision iStorageMercurialRev = (IStorageMercurialRevision) myIStorage;
      return ! (iStorageMercurialRev.resource.getType() == IResource.FILE);
    }
    return false;
  }
  
  public MercurialFileHistoryVariant[] members()
  {
    if (isContainer())
    {
      try 
      {
        if(myIStorage instanceof IStorageMercurialRevision)
        {
          IStorageMercurialRevision iStorageMercurialRev = (IStorageMercurialRevision) myIStorage;
          IResource resource = iStorageMercurialRev.resource;
          if(resource.getType() == IResource.FILE)
          {
            return new MercurialFileHistoryVariant[0];
          }
          IContainer container = (IContainer)resource;
          List existingChildren = new ArrayList(Arrays.asList(container.members()));
          existingChildren.addAll(  Arrays.asList(container.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, null)));
          return (MercurialFileHistoryVariant[]) existingChildren.toArray(new MercurialFileHistoryVariant[existingChildren.size()]);
        }
      } 
      catch (CoreException e) 
      {
        MercurialEclipsePlugin.logError(e);
      }
    }
    return new MercurialFileHistoryVariant[0];
  }

  @Override
public IStorage getStorage(IProgressMonitor monitor) throws TeamException 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getStorage()" );
    return myIStorage;
  }

  public String getContentIdentifier() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getContentIdentifier()" );
    return myIStorage.getFullPath().toString();
  }

  public byte[] asBytes() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::asBytes()" );
    return myIStorage.getFullPath().toString().getBytes();
  }


  @Override
  protected void fetchContents(IProgressMonitor monitor) throws TeamException
  {
    try
    {
      setContents(myIStorage.getContents(), monitor);
    }
    catch (CoreException e)
    {
      MercurialEclipsePlugin.logError(e);
    }
  }


  @Override
  protected String getCacheId()
  {
    return MercurialTeamProvider.ID;
  }


  @Override
  protected String getCachePath()
  {
    /* Path with apended revision */
    return myIStorage.getFullPath().toString();
  }
}  
