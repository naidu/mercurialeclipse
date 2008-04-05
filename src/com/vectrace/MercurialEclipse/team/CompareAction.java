/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     StefanC                   - many updates
 *     Stefan Groschupf          - logError
 *     Jerome Negre              - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;


import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.compare.HgCompareEditorInput;
import com.vectrace.MercurialEclipse.compare.RevisionNode;

/**
 * @author zingo, Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class CompareAction extends SingleFileAction {

  private boolean dialog;
  
  public CompareAction() {
    super();
  }
  
  public CompareAction(boolean dialog) {
    this.dialog = dialog;
  }
  
  @Override
	public void run(IFile file) throws TeamException {
		openEditor(file);
	}
	
  public void openEditor(IResource file) {
    openEditor(null, new IStorageMercurialRevision(file));
  }
  
	public void openEditor(IResource file, String changeset) {
	  openEditor(null, new IStorageMercurialRevision(file, changeset)); 
	}
	
	public void openEditor(IStorageMercurialRevision left, IStorageMercurialRevision right) {
	  openEditor(getNode(left), getNode(right));
	}

	public void openEditor(ResourceNode left, ResourceNode right) {
	  Assert.isNotNull(right);
		CompareEditorInput compareInput = getCompareInput(left, right);
		if (compareInput != null) {
		  if(dialog) {
		    CompareUI.openCompareDialog(compareInput);
		  } else {
		    CompareUI.openCompareEditor(compareInput);
		  }
		}
	}

	private CompareEditorInput getCompareInput(ResourceNode left, ResourceNode right) {
	  IResource resource = right.getResource();
    return new HgCompareEditorInput(new CompareConfiguration(), resource,
	      left != null ? left : right,
	      left != null ? right : new ResourceNode(resource)
	  );
	}
	
	private RevisionNode getNode(IStorageMercurialRevision rev) {
	  return rev == null ? null : new RevisionNode(rev);
	}
}
