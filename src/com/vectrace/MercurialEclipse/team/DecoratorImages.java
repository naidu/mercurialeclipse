package com.vectrace.MercurialEclipse.team;



import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;

/**
 * Set of images that are used for decorating resources are maintained
 * here. This acts as a image registry and hence there is a single copy
 * of the image files floating around the project. 
 * 
 */
public class DecoratorImages
{
  /**
   * Added Image Descriptor
   */ 
  public static final ImageDescriptor addedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/added.gif");
  
  /**
   * Deleted but still tracked Image Descriptor
   */ 
  public static final ImageDescriptor deletedStillTrackedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/deleted_still_tracked.gif");
  
  /**
   * Ignored Image Descriptor
   */ 
  public static final ImageDescriptor ignoredDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/ignored.gif");
    
  /**
   * Modified Image Descriptor
   */ 
  public static final ImageDescriptor modifiedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/modified.gif");

  /**
   * Not tracked Image Descriptor
   */ 
  public static final ImageDescriptor notTrackedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/not_tracked.gif");
  /**
   * Removed Image Descriptor
   */ 
  public static final ImageDescriptor removedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/removed.gif");

  
  
  /**
   * Constructor for DemoImages.
   */
  public DecoratorImages()
  {
    super();
  }


  
  /**
   * Get the image data depending on the key
   * 
   * @return image data 
   * 
   */ 
  public ImageData getImageData(String imageKey)
  {
    ImageDescriptor imagedescriptor=getImageDescriptor(imageKey);
    if(imagedescriptor!=null)
    {
      return imagedescriptor.getImageData();
    }
    else
    {
      return null;
    }
  }
   
  /**
   * Get the image descriptor depending on the key
   * 
   * @return image descriptor 
   * 
   */ 
  public static final ImageDescriptor getImageDescriptor(String imageKey)
  {
    if (imageKey.startsWith("M"))
    {
      return modifiedDescriptor;
    }
    if (imageKey.startsWith("A"))
    {
      return addedDescriptor;
    }
    if (imageKey.startsWith("R"))
    {
      return removedDescriptor;
    }
    if (imageKey.startsWith("!"))
    {
      return deletedStillTrackedDescriptor;
    }
    if (imageKey.startsWith("?"))
    {
      return notTrackedDescriptor;
    }
    if (imageKey.startsWith("I"))
    {
      return ignoredDescriptor;
    }
    return null;
  }

  // public ImageDescriptor 

}
