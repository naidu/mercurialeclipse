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

public class HgCompareEditorInput extends CompareEditorInput
{
  private static final Differencer DIFFERENCER = new Differencer();

  private final ResourceNode left;
  private final ResourceNode ancestor;
  private final ResourceNode right;

  public HgCompareEditorInput(CompareConfiguration configuration,
      IResource resource, ResourceNode left, ResourceNode right)
  {
    super(configuration);
    this.left = left;
    this.ancestor = null;
    this.right = right;
    setTitle(resource.getName());
    configuration.setLeftLabel(left.getName());
    configuration.setRightLabel(right.getName());
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