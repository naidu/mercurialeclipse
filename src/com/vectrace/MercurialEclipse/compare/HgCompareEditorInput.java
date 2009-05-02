/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

public class HgCompareEditorInput extends CompareEditorInput
{
  private static final Differencer DIFFERENCER = new Differencer();

  private final ResourceNode left;
  private final ResourceNode ancestor;
  private final ResourceNode right;

  /*
   * Does either a 2-way or 3-way compare, depending on if one is an ancestor
   * of the other. If they are divergent, then it finds the common ancestor
   * and does 3-way compare.
   */
  public HgCompareEditorInput(CompareConfiguration configuration,
      IResource resource, ResourceNode left, ResourceNode right)
  {
    super(configuration);
    this.left = left;
    this.ancestor = findParentNodeIfExists(resource, left, right);
    this.right = right;
    setTitle(resource.getName());
    configuration.setLeftLabel(left.getName());
    configuration.setLeftEditable(false);
    configuration.setRightLabel(right.getName());
    configuration.setRightEditable(false);
  }


  private ResourceNode findParentNodeIfExists(IResource resource, ResourceNode l, ResourceNode r) {
      if (!(l instanceof RevisionNode && r instanceof RevisionNode))
          return null;
      
      String lId = ((RevisionNode)l).getRevision();
      String rId = ((RevisionNode)r).getRevision();
      
      try {
        int commonAncestor = HgParentClient.findCommonAncestor(resource.getProject().getLocation().toFile(), lId, rId);
        if (String.valueOf(commonAncestor).equals(lId))
            return null;
        if (String.valueOf(commonAncestor).equals(rId))
            return null;
        return new RevisionNode(new IStorageMercurialRevision(resource, commonAncestor));
    } catch (HgException e) {
        MercurialEclipsePlugin.logError(e);
        return null;
    }
  }

  public HgCompareEditorInput(CompareConfiguration configuration,
            IResource leftResource, ResourceNode ancestor, ResourceNode right, boolean localEditable) {
        super(configuration);
        this.left = new ResourceNode(leftResource);
        this.ancestor = ancestor;
        this.right = right;
        setTitle(left.getName());
        configuration.setLeftLabel(left.getName());
        configuration.setLeftEditable(localEditable);
        configuration.setAncestorLabel(ancestor.getName());
        configuration.setRightLabel(right.getName());
        configuration.setRightEditable(false);
    }

  @Override
  protected Object prepareInput(IProgressMonitor monitor)
      throws InvocationTargetException, InterruptedException
  {
    return DIFFERENCER.findDifferences(ancestor != null, monitor, null, ancestor, left, right);
  }
  
  @Override
  public void saveChanges(IProgressMonitor monitor) throws CoreException
  {
    boolean save = isSaveNeeded();
    super.saveChanges(monitor);
    if(save) {
      ((IFile)left.getResource()).setContents(left.getContents(), true, true, monitor);
    }
  }
}