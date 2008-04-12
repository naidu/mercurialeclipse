/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError 
 *     Stefan C                  - Code cleanup
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.Vector;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;
import com.vectrace.MercurialEclipse.model.ChangeLog;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author zingo
 *
 */
public class MercurialHistory extends FileHistory
{
  private IResource resource;
  protected IFileRevision[] revisions;
  ChangeLog changeLog;


  public MercurialHistory(IResource resource)
  {
    super();
    this.resource = resource;
  }
  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getContributors(org.eclipse.team.core.history.IFileRevision)
   */
  public IFileRevision[] getContributors(IFileRevision revision)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistory::getContributors()");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevision(java.lang.String)
   */
  public IFileRevision getFileRevision(String id)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistory::getFileRevision(" + id + ")");
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevisions()
   */
  public IFileRevision[] getFileRevisions()
  {
//    System.out.println("MercurialHistory::getFileRevisions()");
    return revisions;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getTargets(org.eclipse.team.core.history.IFileRevision)
   */
  public IFileRevision[] getTargets(IFileRevision revision)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialHistory::getTargets()");
    return null;
  }

  public void refresh(IProgressMonitor monitor) throws CoreException 
  {
//    System.out.println("MercurialHistory::refresh() (home made)");
    RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
    if (provider != null && provider instanceof MercurialTeamProvider) 
    {
      
      changeLog = new ChangeLog(resource);
      
//      changeLog.ChangeChangeLog(in_resource);
      Vector<ChangeSet> changeSets = changeLog.getChangeLog();
      
      
      revisions = new IFileRevision[changeSets.size()];
      for(int i=0;i<changeSets.size();i++)
      {
        revisions[i]=new MercurialRevision(changeSets.get(i),resource);
      }
    }
  } 
  
}
