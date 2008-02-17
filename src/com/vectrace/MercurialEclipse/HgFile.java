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
