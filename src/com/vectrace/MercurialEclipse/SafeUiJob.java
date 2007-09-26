/**
 * 
 */
package com.vectrace.MercurialEclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

/**
 * @author StefanC
 * 
 */
public class SafeUiJob extends UIJob {

  /**
   * @param name
   */
  public SafeUiJob(String name) {
    super(name);
  }

  /**
   * @param jobDisplay
   * @param name
   */
  public SafeUiJob(Display jobDisplay, String name) {
    super(jobDisplay, name);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public final IStatus runInUIThread(IProgressMonitor monitor) {
    try {
      return runSafe(monitor);
    } catch (RuntimeException error) {
      MercurialEclipsePlugin.logError(error);
      return Status.CANCEL_STATUS;
    }
  }

  /**
   * @param monitor
   * @return
   */
  protected IStatus runSafe(IProgressMonitor monitor) {
    return Status.OK_STATUS;
  }

}
