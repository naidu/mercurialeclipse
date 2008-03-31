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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

/**
 * The main plugin class to be used in the desktop.
 */
public class MercurialEclipsePlugin extends AbstractUIPlugin
{

  public static final String ID = "com.vectrace.MercurialEclipse";

  public static final String ID_ChangeLogView = "com.vectrace.MercurialEclipse.views.ChangeLogView";

  // The shared instance.
  private static MercurialEclipsePlugin plugin;

  // TODO: not quite sure this should be static
  private static HgRepositoryLocationManager repoManager = new HgRepositoryLocationManager();

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
public void start(BundleContext context) throws Exception
  {
    try
    {
      // System.out.println("MercurialEclipsePlugin.start()");
      super.start(context);
    } catch (Exception e)
    {
    	logError("unable to startMercurialEclipsePlugin ", e);
      // TODO: handle exception
      // System.out.println("MercurialEclipsePlugin.start() got
      // execption");
      throw e;
    }

    // TODO: Presumably this should be wrapped around some sort of timer to
    // ensure we don't tank eclipse if something goes wrong.
    repoManager.start();
  }

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
    return AbstractUIPlugin.imageDescriptorFromPlugin("com.vectrace.MercurialEclipse", path);
  }

  public static final void logError(String message, Throwable error)
  {
    getDefault().getLog().log(createStatus(message, 0, IStatus.ERROR, error));
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
}