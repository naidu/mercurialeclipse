package com.vectrace.MercurialEclipse;

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
//    System.out.println("MercurialEclipsePlugin.MercurialEclipsePlugin()");
  }

  /**
   * This method is called upon plug-in activation
   */
  public void start(BundleContext context) throws Exception
  {
    try
    {
      // System.out.println("MercurialEclipsePlugin.start()");
      super.start(context);
    } catch (Exception e)
    {
      // TODO: handle exception
      // System.out.println("MercurialEclipsePlugin.start() got execption");
      throw e;
    }

    // TODO: Presumably this should be wrapped around some sort of timer to
    //       ensure we don't tank eclipse if something goes wrong.
    repoManager.start();
  }
  
  static public HgRepositoryLocationManager getRepoManager()
  {
    return repoManager;
  }

  /**
   * This method is called when the plug-in is stopped
   */
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

}
