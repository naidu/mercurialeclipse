/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Juerg Billeter, juergbi@ethz.ch - 47136 Search view should show match objects
 *     Ulrich Etter, etteru@ethz.ch - 47136 Search view should show match objects
 *     Roman Fuchs, fuchsro@ethz.ch - 47136 Search view should show match objects
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.search.internal.ui.Messages;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.internal.ui.SearchPluginImages;
import org.eclipse.search.internal.ui.text.BasicElementLabels;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

public class MercurialTextSearchLabelProvider extends LabelProvider implements IStyledLabelProvider {

	public static final int SHOW_LABEL = 1;
	public static final int SHOW_LABEL_PATH = 2;
	public static final int SHOW_PATH_LABEL = 3;

	private static final String fgSeparatorFormat = "{0} - {1}"; //$NON-NLS-1$

	private static final String fgEllipses = " ... "; //$NON-NLS-1$

	private final WorkbenchLabelProvider fLabelProvider;
	private final AbstractTextSearchViewPage fPage;
	private final Comparator<MercurialMatch> fMatchComparator;

	private final Image fLineMatchImage;

	private int fOrder;

	public MercurialTextSearchLabelProvider(AbstractTextSearchViewPage page, int orderFlag) {
		fLabelProvider = new WorkbenchLabelProvider();
		fOrder = orderFlag;
		fPage = page;
		fLineMatchImage = SearchPluginImages.get(SearchPluginImages.IMG_OBJ_TEXT_SEARCH_LINE);
		fMatchComparator = new Comparator<MercurialMatch>() {
			public int compare(MercurialMatch o1, MercurialMatch o2) {
				return o1.getLineNumber() - o2.getLineNumber();
			}
		};
	}

	public void setOrder(int orderFlag) {
		fOrder = orderFlag;
	}

	public int getOrder() {
		return fOrder;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object object) {
		return getStyledText(object).getString();
	}

	private StyledString getColoredLabelWithCounts(Object element, StyledString coloredName) {
		AbstractTextSearchResult result = fPage.getInput();
		if (result == null) {
			return coloredName;
		}

		int matchCount = result.getMatchCount(element);
		if (matchCount <= 1) {
			return coloredName;
		}

		String countInfo = Messages.format(SearchMessages.FileLabelProvider_count_format,
				new Integer(matchCount));
		coloredName.append(' ').append(countInfo, StyledString.COUNTER_STYLER);
		return coloredName;
	}

	public StyledString getStyledText(Object element) {
		if (element instanceof MercurialRevisionStorage) {
			MercurialRevisionStorage mrs = (MercurialRevisionStorage) element;
			return new StyledString(mrs.getRevision() + ":" + mrs.getChangeSet().getNodeShort());
		}

		if (element instanceof MercurialMatch) {
			MercurialMatch match = (MercurialMatch) element;
			return getMercurialMatchLabel(match);
		}

		if (!(element instanceof IResource)) {
			return new StyledString(element.toString());
		}

		IResource resource = (IResource) element;
		if (!resource.exists()) {
			new StyledString(SearchMessages.FileLabelProvider_removed_resource_label);
		}

		String name = BasicElementLabels.getResourceName(resource);
		if (fOrder == SHOW_LABEL) {
			return getColoredLabelWithCounts(resource, new StyledString(name));
		}

		String pathString = BasicElementLabels.getPathLabel(resource.getParent().getFullPath(),
				false);
		if (fOrder == SHOW_LABEL_PATH) {
			StyledString str = new StyledString(name);
			String decorated = Messages.format(fgSeparatorFormat, new String[] { str.getString(),
					pathString });

			StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER,
					str);
			return getColoredLabelWithCounts(resource, str);
		}

		StyledString str = new StyledString(Messages.format(fgSeparatorFormat, new String[] {
				pathString, name }));
		return getColoredLabelWithCounts(resource, str);
	}

	private StyledString getMercurialMatchLabel(MercurialMatch match) {
		int lineNumber = match.getLineNumber();

		StyledString str = new StyledString(lineNumber + ", " + match.getOffset()+": ",
				StyledString.QUALIFIER_STYLER);

		String content = match.getExtract();

		return str.append(content);
	}

	private static final int MIN_MATCH_CONTEXT = 10; // minimal number of

	// characters shown
	// after and before a
	// match

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof MercurialRevisionStorage) {
			return MercurialEclipsePlugin.getImage("elcl16/changeset_obj.gif");
		}

		if (element instanceof MercurialMatch) {
			return fLineMatchImage;
		}

		if (!(element instanceof IResource)) {
			return null;
		}

		IResource resource = (IResource) element;
		Image image = fLabelProvider.getImage(resource);
		return image;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		fLabelProvider.dispose();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#isLabelProperty(java.lang .Object,
	 * java.lang.String)
	 */
	@Override
	public boolean isLabelProperty(Object element, String property) {
		return fLabelProvider.isLabelProperty(element, property);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#removeListener(org.eclipse
	 * .jface.viewers.ILabelProviderListener)
	 */
	@Override
	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		fLabelProvider.removeListener(listener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#addListener(org.eclipse.jface
	 * .viewers.ILabelProviderListener)
	 */
	@Override
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		fLabelProvider.addListener(listener);
	}

}
