/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ui.mapping.ResourceModelLabelProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;

@SuppressWarnings("restriction")
public class SyncViewLabelProvider extends ResourceModelLabelProvider {

	@Override
	public Image getImage(Object element) {
		return super.getImage(element);
	}

	@Override
	protected Image getDelegateImage(Object element) {
		Image image = null;
		if (element instanceof ChangeSet) {
			image = MercurialEclipsePlugin.getImage("elcl16/changeset_obj.gif");
		} else if (element instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) element;
			if(group.getDirection() == Direction.OUTGOING){
				image = MercurialEclipsePlugin.getImage("actions/commit.gif");
			} else {
				image = MercurialEclipsePlugin.getImage("actions/update.gif");
			}
		} else {
			image = super.getDelegateImage(element);
		}
		return image;
	}

	@Override
	protected String getDelegateText(Object elementOrPath) {
		if(elementOrPath instanceof ChangeSet){
			ChangeSet cset = (ChangeSet) elementOrPath;
			StringBuilder sb = new StringBuilder(cset.toString());
			if(!(cset instanceof WorkingChangeSet)){
				sb.append(" (").append(cset.getAuthor()).append(")");
				sb.append(", ").append(cset.getAgeDate());
			}
			return sb.toString();
		}
		if(elementOrPath instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) elementOrPath;
			String name = group.getName();
			if(group.getChangesets().isEmpty()){
				return name + " (empty)";
			}
			return name + " (" + group.getChangesets().size() + ")";
		}
		String delegateText = super.getDelegateText(elementOrPath);
		if(delegateText != null && delegateText.length() > 0){
			delegateText = " " + delegateText;
		}
		return delegateText;
	}
}
