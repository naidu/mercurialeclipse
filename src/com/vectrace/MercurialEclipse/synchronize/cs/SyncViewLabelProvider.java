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

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.internal.ui.mapping.ResourceModelLabelProvider;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.utils.StringUtils;

@SuppressWarnings("restriction")
public class SyncViewLabelProvider extends ResourceModelLabelProvider {

	@Override
	public Image getImage(Object element) {
		// we MUST call super, to get the nice in/outgoing decorations...
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
			} else if(group.getDirection() == Direction.INCOMING){
				image = MercurialEclipsePlugin.getImage("actions/update.gif");
			} else {
				image = MercurialEclipsePlugin.getImage("cview16/repository_rep.gif");
			}
		} else if(element instanceof FileFromChangeSet){
			FileFromChangeSet file = (FileFromChangeSet) element;
			if(file.getFile() != null){
				image = getDelegateLabelProvider().getImage(file.getFile());
			} else {
				image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
			}
		} else {
			try {
				image = super.getDelegateImage(element);
			} catch (NullPointerException npex) {
				// if element is invalid or not yet fully handled
				// NPE is possible
				MercurialEclipsePlugin.logError(npex);
			}
		}
		return image;
	}

	@Override
	protected Image decorateImage(Image base, Object element) {
		Image decoratedImage;
		if (element instanceof FileFromChangeSet) {
			FileFromChangeSet ffc = (FileFromChangeSet) element;
			int kind = ffc.getDiffKind();
			decoratedImage = getImageManager().getImage(base, kind);
		} else if (element instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) element;
			if(group.getDirection() == Direction.LOCAL){
				decoratedImage = getImageManager().getImage(base, Differencer.CHANGE);
			} else {
				decoratedImage = getImageManager().getImage(base, Differencer.NO_CHANGE);
			}
		} else {
			decoratedImage = getImageManager().getImage(base, Differencer.NO_CHANGE);
		}
		return decoratedImage;
	}


	@Override
	protected String getDelegateText(Object elementOrPath) {
		if(elementOrPath instanceof ChangeSet){
			ChangeSet cset = (ChangeSet) elementOrPath;
			StringBuilder sb = new StringBuilder();
			if(!(cset instanceof WorkingChangeSet)){
				sb.append(cset.getChangesetIndex());
				sb.append(" [").append(cset.getAuthor()).append("]");
				sb.append(" (").append(cset.getAgeDate()).append(")");
				sb.append(" ").append(getShortComment(cset));
			} else {
				sb.append(cset.toString());
			}
			return StringUtils.removeLineBreaks(sb.toString());
		}
		if(elementOrPath instanceof ChangesetGroup){
			ChangesetGroup group = (ChangesetGroup) elementOrPath;
			String name = group.getName();
			if(group.getChangesets().isEmpty()){
				return name + " (empty)";
			}
			return name + " (" + group.getChangesets().size() + ")";
		}
		if(elementOrPath instanceof FileFromChangeSet){
			FileFromChangeSet file = (FileFromChangeSet) elementOrPath;

			String delegateText;
			if(file.getFile() != null) {
				delegateText = super.getDelegateText(file.getFile());
			} else {
				delegateText = file.toString();
			}
			if(delegateText != null && delegateText.length() > 0){
				delegateText = " " + delegateText;
			}
			return delegateText;
		}
		String delegateText = super.getDelegateText(elementOrPath);
		if(delegateText != null && delegateText.length() > 0){
			delegateText = " " + delegateText;
		}
		return delegateText;
	}

	private String getShortComment(ChangeSet cset) {
		String comment = cset.getComment();
		if(comment.length() > 50){
			comment = comment.substring(0, 50) + "...";
		}
		return comment;
	}

}
