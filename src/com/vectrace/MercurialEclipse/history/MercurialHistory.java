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

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileHistory;

import com.vectrace.MercurialEclipse.commands.HgGLogClient;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.GChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author zingo
 *
 */
public class MercurialHistory extends FileHistory
{
  private IResource resource;
  protected IFileRevision[] revisions;

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
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevision(java.lang.String)
   */
  public IFileRevision getFileRevision(String id)
  {
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getFileRevisions()
   */
  public IFileRevision[] getFileRevisions()
  {
    return revisions;
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.core.history.IFileHistory#getTargets(org.eclipse.team.core.history.IFileRevision)
   */
  public IFileRevision[] getTargets(IFileRevision revision)
  {
    return null;
  }

  public void refresh(IProgressMonitor monitor) throws CoreException 
  {
    RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
    if (provider != null && provider instanceof MercurialTeamProvider) 
    {
    	// We need these to be in order for the GChangeSets to display properly
    	SortedSet<ChangeSet> changeSets = new TreeSet<ChangeSet>(new Comparator<ChangeSet>(){
    		public int compare(ChangeSet o1, ChangeSet o2) {
    			return o2.getChangesetIndex() - o1.getChangesetIndex();
    		}
    	});
    	changeSets.addAll(MercurialStatusCache.getInstance().getLocalChangeSets(resource));
    	List<GChangeSet> gChangeSets = new HgGLogClient(resource).update(changeSets).getChangeSets();
    	revisions = new IFileRevision[changeSets.size()];
    	int i = 0;
    	for (ChangeSet cs : changeSets) {
    		revisions[i] = new MercurialRevision(
    				cs,
    				gChangeSets.get(i),
    				resource);
    		i++;
    	}
    }
  } 

}
