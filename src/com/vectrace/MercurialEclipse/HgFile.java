/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse;

import org.eclipse.core.resources.IFile;

public class HgFile
{
  private IFile file;
  
  public HgFile(IFile file) {
    this.file = file;
  }
  
  public IFile getFile()
  {
    return file;
  }

  public String getName()
  {
    return file.getName();
  }

  public boolean exists()
  {
    return file.exists();
  }
}
