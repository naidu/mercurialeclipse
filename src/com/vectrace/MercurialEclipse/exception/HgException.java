package com.vectrace.MercurialEclipse.exception;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class HgException extends TeamException
{
  private static final long serialVersionUID = 1L; // Get rid of warning 
  
  public static final int OPERATION_FAILED = -100;
  public static final String OPERATION_FAILED_STRING = "Mercurial Operation failed";

  public HgException(IStatus status)
  {
    super(status);
  }

  public HgException(String message)
  {
    super(new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, OPERATION_FAILED,
                     message, null));
  }

  public HgException(CoreException e)
  {
    super(e);
    // TODO Auto-generated constructor stub
  }

  public HgException(String message, Throwable e)
  {
    super(new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, OPERATION_FAILED,
                     message, e));
  }
  
  public HgException(int code, String message)
  {
    super(new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, code, message, null));
  }

}
