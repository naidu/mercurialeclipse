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
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;

public class MercurialRepositorySubscriber extends Subscriber 
{

  public class LocalHistoryVariantComparator implements IResourceVariantComparator 
  {
    public boolean compare(IResource local, IResourceVariant remote) 
    {
      return false;
    }

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
    comparatorObj = new LocalHistoryVariantComparator();
  }
  
  public String getName()
  {
    return "MercurialRepositorySubscriber";
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

  public SyncInfo getSyncInfo(IResource resource, IStorage r1, IStorage r2) throws TeamException 
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
      MercurialFileHistoryVariant fileHist1=null;
      MercurialFileHistoryVariant fileHist2=null;
      if(r1 != null)
      {
        fileHist1 = new MercurialFileHistoryVariant(r1);
      }
      if(r2 != null)
      {
        fileHist2 = new MercurialFileHistoryVariant(r2);
      }
      
      SyncInfo info = new SyncInfo(resource, fileHist1,fileHist2, comparatorObj);
      info.init();
      return info;
    } 
    catch (CoreException e) 
    {
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
