/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *******************************************************************************/

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
  public static final ImageDescriptor addedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/added.png");
  
  /**
   * Deleted but still tracked Image Descriptor
   */ 
  public static final ImageDescriptor deletedStillTrackedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/deleted_still_tracked.png");
  
  /**
   * Ignored Image Descriptor
   */ 
  public static final ImageDescriptor ignoredDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/ignored.png");
    
  /**
   * Modified Image Descriptor
   */ 
  public static final ImageDescriptor modifiedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/modified.png");

  /**
   * Not tracked Image Descriptor
   */ 
  public static final ImageDescriptor notTrackedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/not_tracked.png");
  /**
   * Removed Image Descriptor
   */ 
  public static final ImageDescriptor removedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/removed.png");

  /**
   * Managed Image Descriptor
   */ 
  public static final ImageDescriptor managedDescriptor = ImageDescriptor.createFromFile (DecoratorStatus.class, "images/managed.png");
  
  
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
    return null;
  }
   
  /**
   * Get the image descriptor depending on the key
   * 
   * @return image descriptor 
   * 
   */ 
  public static final ImageDescriptor getImageDescriptor(String imageKey)
  {
    //Input is the output from the "hg status <file>" comamnd
    if(imageKey==null)
    {
      //hg status <file> has no output in an a managed file
      return managedDescriptor;
    }

    // Look at the first letter
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
