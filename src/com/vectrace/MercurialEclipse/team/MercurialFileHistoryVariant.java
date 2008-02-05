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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
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
