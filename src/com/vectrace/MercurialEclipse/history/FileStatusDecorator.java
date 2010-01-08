/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;

import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.team.DecoratorImages;

public class FileStatusDecorator extends LabelProvider implements ILightweightLabelDecorator {

	public static String ID = "com.vectrace.MercurialEclipse.history.FileStatusDecorator";

	@Override
	public void addListener(ILabelProviderListener listener) {
		// noop
	}

	public void decorate(Object element, IDecoration decoration) {
		if (!(element instanceof FileStatus)) {
			return;
		}
		final Action action = ((FileStatus) element).getAction();
		ImageDescriptor overlay = null;
		if(action == null){
			overlay = DecoratorImages.notTrackedDescriptor;
		} else {
			switch(action){
			case ADDED:
				overlay = DecoratorImages.addedDescriptor;
				break;
			case MODIFIED:
				overlay = DecoratorImages.modifiedDescriptor;
				break;
			case REMOVED:
				overlay = DecoratorImages.removedDescriptor;
				break;
			}
		}
		decoration.addOverlay(overlay);
	}

}
