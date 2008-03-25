/*******************************************************************************
 * Copyright (c) 2008 Vectrace (Zingo Andersen) 
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
 *******************************************************************************/

package com.vectrace.MercurialEclipse.team;



import org.eclipse.jface.resource.ImageDescriptor;

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

}
