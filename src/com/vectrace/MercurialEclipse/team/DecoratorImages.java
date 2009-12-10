/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Andrei Loskutov (Intland) - bug fixes
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
public static final ImageDescriptor addedDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/added_ov.gif"); //$NON-NLS-1$

/**
   * Deleted but still tracked Image Descriptor
   */
public static final ImageDescriptor deletedStillTrackedDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/deleted_still_tracked_ov.gif"); //$NON-NLS-1$

/**
   * Ignored Image Descriptor
   */
public static final ImageDescriptor ignoredDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/ignored_ov.gif"); //$NON-NLS-1$

/**
   * Modified Image Descriptor
   */
public static final ImageDescriptor modifiedDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/modified_ov.gif"); //$NON-NLS-1$

/**
   * Not tracked Image Descriptor
   */
public static final ImageDescriptor notTrackedDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/not_tracked_ov.gif"); //$NON-NLS-1$
/**
   * Removed Image Descriptor
   */
public static final ImageDescriptor removedDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/removed_ov.gif"); //$NON-NLS-1$

/**
   * Managed Image Descriptor
   */
public static final ImageDescriptor managedDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/managed_ov.gif"); //$NON-NLS-1$

/**
   * Conflict Image Descriptor
   */
//FIXME temporary picture
public static final ImageDescriptor conflictDescriptor = ImageDescriptor.createFromFile (DecoratorImages.class, "images/confchg_ov.gif"); //$NON-NLS-1$

}
