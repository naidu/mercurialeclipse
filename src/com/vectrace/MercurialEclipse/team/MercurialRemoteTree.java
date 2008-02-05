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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.ThreeWayRemoteTree;
import org.eclipse.team.core.variants.ThreeWaySubscriber;

/**
 * @author zingo
 *
 */
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
    // TODO Auto-generated method stub
    return null;
  }

}
