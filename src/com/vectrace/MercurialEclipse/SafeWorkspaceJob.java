
/**
 * 
 */
package com.vectrace.MercurialEclipse;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * @author StefanC
 * 
 */
public class SafeWorkspaceJob extends WorkspaceJob {

  /**
   * @param name
   */
  public SafeWorkspaceJob(String name) {
    super(name);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public IStatus runInWorkspace(IProgressMonitor monitor)
      throws CoreException {
    try {
      return runSafe(monitor);
    } catch (RuntimeException ex) {
      handleException(ex);
      return Status.CANCEL_STATUS;
    }
  }

  /**
   * @return
   */
  protected IStatus runSafe(IProgressMonitor monitor) {
    return Status.OK_STATUS;
  }
  
  protected void handleException(Throwable ex) {
    MercurialEclipsePlugin.logError(ex);
    
  }


}
