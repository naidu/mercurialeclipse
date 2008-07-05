/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Jérôme Nègre              - some fixes
 *     Stefan C                  - Code cleanup
 *******************************************************************************/


package com.vectrace.MercurialEclipse;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.vectrace.MercurialEclipse.commands.HgDebugInstallClient;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * The main plugin class to be used in the desktop.
 */
public class MercurialEclipsePlugin extends AbstractUIPlugin
{

  public static final String ID = "com.vectrace.MercurialEclipse";

  public static final String ID_ChangeLogView = "com.vectrace.MercurialEclipse.views.ChangeLogView";

  public static final String BUNDLE_FILE_PREFIX = "bundlefile";

  // The shared instance.
  private static MercurialEclipsePlugin plugin;

  // TODO: not quite sure this should be static
  private static HgRepositoryLocationManager repoManager = new HgRepositoryLocationManager();

  private boolean hgUsable = true;

//  private FlagManager flagManager;

  /**
   * The constructor.
   */
  public MercurialEclipsePlugin()
  {
    plugin = this;
    // System.out.println("MercurialEclipsePlugin.MercurialEclipsePlugin()");
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
    public void start(BundleContext context) throws Exception {
        try {
            super.start(context);
            getPreferenceStore();
            checkHgInstallation();            
        } catch (Exception e) {
            this.hgUsable = false;
            logError("Unable to start MercurialEclipsePlugin ", e);
            throw e;
        }
        repoManager.start();
    }

    public void checkHgInstallation() {
        try {
            this.hgUsable = true;
            MercurialUtilities.getHGExecutable(true);
            String result = HgDebugInstallClient.debugInstall();
            if (result.endsWith("No problems detected")) {
                this.hgUsable = true;
                return;
            }
        } catch (Exception e) {
            this.hgUsable = false;
        }
    }

    // public static void refreshProjectsFlags(final Collection<IProject>
    // projects) {
    // new SafeWorkspaceJob("Refresh project state") {
    // @Override
    // protected IStatus runSafe(IProgressMonitor monitor) {
    // try {
    // for (IProject project : projects) {
    // //getDefault().getFlagManager().refresh(project);
    // MercurialStatusCache.getInstance().refreshStatus(project, monitor);
    // }
    // } catch (HgException e) {
    // logError(e);
    // }
    // return Status.OK_STATUS;
    //            }
    //        }.schedule();
    //    }

  static public HgRepositoryLocationManager getRepoManager()
  {
    return repoManager;
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
public void stop(BundleContext context) throws Exception
  {
//    flagManager.dispose();
    repoManager.stop();
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the shared instance.
   */
  public static MercurialEclipsePlugin getDefault()
  {
    return plugin;
  }

  /**
   * Returns an image descriptor for the image file at the given plug-in
   * relative path.
   *
   * @param path
   *          the path
   * @return the image descriptor
   */
  public static ImageDescriptor getImageDescriptor(String path)
  {
    return AbstractUIPlugin.imageDescriptorFromPlugin("com.vectrace.MercurialEclipse", "icons/"+path);
  }

  public static final void logError(String message, Throwable error)
  {
    getDefault().getLog().log(createStatus(message, 0, IStatus.ERROR, error));
  }

  public static void showError(Throwable error) {
      ErrorDialog.openError(null, "Unexpected Error", error.getMessage(),
              createStatus(error.getMessage(), 0, IStatus.ERROR, error));
  }

  public static final void logWarning(String message, Throwable error)
  {
    getDefault().getLog().log(createStatus(message, 0, IStatus.WARNING, error));
  }

  public static final void logInfo(String message, Throwable error)
  {
    getDefault().getLog().log(createStatus(message, 0, IStatus.INFO, error));
  }

  private static IStatus createStatus(String msg, int code, int severity,
      Throwable ex)
  {
    return new Status(severity, ID, code, msg, ex);
  }

  /**
   * @param ex
   */
  public final static void logError(Throwable ex)
  {
    logError(ex.getMessage(), ex);
  }

  /**
     * Creates a busy cursor and runs the specified runnable. May be called from
     * a non-UI thread.
     *
     * @param parent
     *            the parent Shell for the dialog
     * @param cancelable
     *            if true, the dialog will support cancelation
     * @param runnable
     *            the runnable
     *
     * @exception InvocationTargetException
     *                when an exception is thrown from the runnable
     * @exception InterruptedException
     *                when the progress monitor is cancelled
     */

    public static void runWithProgress(Shell parent, boolean cancelable,
            final IRunnableWithProgress runnable)
            throws InvocationTargetException, InterruptedException {

        boolean createdShell = false;
        Shell myParent = parent;
        try {
            if (myParent == null || myParent.isDisposed()) {
                Display display = Display.getCurrent();
                if (display == null) {
                    // cannot provide progress (not in UI thread)
                    runnable.run(new NullProgressMonitor());
                    return;
                }
                // get the active shell or a suitable top-level shell
                myParent = display.getActiveShell();
                if (myParent == null) {
                    myParent = new Shell(display);
                    createdShell = true;
                }
            }
            // pop up progress dialog after a short delay
            final Exception[] holder = new Exception[1];
            BusyIndicator.showWhile(myParent.getDisplay(), new Runnable() {
                public void run() {
                    try {
                        runnable.run(new NullProgressMonitor());
                    } catch (InvocationTargetException e) {
                        holder[0] = e;
                    } catch (InterruptedException e) {
                        holder[0] = e;
                    }
                }
            });
            if (holder[0] != null) {
                if (holder[0] instanceof InvocationTargetException) {
                    throw (InvocationTargetException) holder[0];
                }
                throw (InterruptedException) holder[0];

            }
            // new TimeoutProgressMonitorDialog(parent, TIMEOUT).run(true
            // /*fork*/, cancelable, runnable);
        } finally {
            if (createdShell) {
                parent.dispose();
            }
        }
    }

    /**
     * Convenience method to get the currently active workbench page. Note that
     * the active page may not be the one that the usr perceives as active in
     * some situations so this method of obtaining the activae page should only
     * be used if no other method is available.
     *
     * @return the active workbench page
     */
    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }
        return window.getActivePage();
    }

    /**
     * @return the hgUsable
     */
    public boolean isHgUsable() {
        return hgUsable;
    }

    /**
     * @return
     */
    public static Display getStandardDisplay() {
        return PlatformUI.getWorkbench().getDisplay();
    }
}