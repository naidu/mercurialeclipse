/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;


public class MercurialRemoteTree extends ThreeWayRemoteTree
{

  
  
  public MercurialRemoteTree(ThreeWaySubscriber subscriber)
  {
    super(subscriber);
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.variants.AbstractResourceVariantTree#fetchMembers(org.eclipse.team.core.variants.IResourceVariant, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected IResourceVariant[] fetchMembers(IResourceVariant variant, IProgressMonitor progress) throws TeamException
  {
    System.out.println("MercurialRemoteTree.fetchMembers(" + variant.toString() + ",monitor)");
    if(variant instanceof MercurialFileHistoryVariant)
    {
      return ((MercurialFileHistoryVariant)variant).members();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.variants.AbstractResourceVariantTree#fetchVariant(org.eclipse.core.resources.IResource, int, org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  protected IResourceVariant fetchVariant(IResource resource, int depth, IProgressMonitor monitor) throws TeamException
  {
    System.out.println("MercurialRemoteTree.fetchVariant(" + resource.toString() + "," + depth +",monitor)");
/*
    RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
    if (provider != null && provider instanceof MercurialTeamProvider) 
    {
      return new MercurialFileHistoryVariant(new IStorageMercurialRevision(resource,0,depth));
    }
*/
    return null;
  }

}
