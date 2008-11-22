/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public final class CommitResourceLabelProvider extends LabelProvider implements ITableLabelProvider
{

  public Image getColumnImage(Object element, int columnIndex)
  {
    // No images.
    return null;
  }

  public String getColumnText(Object element, int columnIndex)
  {
    if ((element instanceof CommitResource) != true)
    {
      return "Type Error"; //$NON-NLS-1$
    }
    CommitResource resource = (CommitResource) element;

    switch (columnIndex)
    {
    case 0:
      return ""; //$NON-NLS-1$
    case 1:
      return resource.getPath().toString();
    case 2:
      return resource.getStatus();
    default:
      return "Col Error: " + columnIndex; //$NON-NLS-1$
    }
  }
}