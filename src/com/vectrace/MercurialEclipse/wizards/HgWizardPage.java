/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation to hg
 ******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import com.vectrace.MercurialEclipse.team.ResourceDecorator;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;

/**
 * Common superclass for Hg wizard pages. Provides convenience methods for
 * widget creation.
 */
public abstract class HgWizardPage extends WizardPage {
    protected Properties properties = null;
    protected IDialogSettings settings;

    /**
     * HgWizardPage constructor comment.
     * 
     * @param pageName
     *            the name of the page
     */
    public HgWizardPage(String pageName) {
        super(pageName);
    }

    /**
     * HgWizardPage constructor comment.
     * 
     * @param pageName
     *            the name of the page
     * @param title
     *            the title of the page
     * @param titleImage
     *            the image for the page
     */
    public HgWizardPage(String pageName, String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    /**
     * HgWizardPage constructor comment.
     * 
     * @param pageName
     *            the name of the page
     * @param title
     *            the title of the page
     * @param titleImage
     *            the image for the page
     * @param description
     *            the description of the page
     */
    public HgWizardPage(String pageName, String title,
            ImageDescriptor titleImage, String description) {
        super(pageName, title, titleImage);
        setDescription(description);
    }

    protected TreeViewer createResourceSelectionTree(Composite composite,
            int types, int span) {
        TreeViewer tree = new TreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.BORDER);
        tree.setUseHashlookup(true);
        tree.setContentProvider(getResourceProvider(types));
        tree.setLabelProvider(new DecoratingLabelProvider(
                new WorkbenchLabelProvider(), PlatformUI.getWorkbench()
                        .getDecoratorManager().getLabelDecorator(
                                ResourceDecorator.class.getName())));
        tree.setComparator(new ResourceComparator(ResourceComparator.NAME));

        GridData data = new GridData(GridData.FILL_BOTH
                | GridData.GRAB_VERTICAL);
        data.heightHint = SWTWidgetHelper.LIST_HEIGHT_HINT;
        data.horizontalSpan = span;
        tree.getControl().setLayoutData(data);
        return tree;
    }

    /**
     * Returns a content provider for <code>IResource</code>s that returns
     * only children of the given resource type.
     */
    protected ITreeContentProvider getResourceProvider(final int resourceType) {
        return new WorkbenchContentProvider() {
            @Override
            public Object[] getChildren(Object o) {
                if (o instanceof IContainer) {
                    IResource[] members = null;
                    try {
                        members = ((IContainer) o).members();
                    } catch (CoreException e) {
                        // just return an empty set of children
                        return new Object[0];
                    }

                    // filter out the desired resource types
                    ArrayList<IResource> results = new ArrayList<IResource>();
                    for (int i = 0; i < members.length; i++) {
                        // And the test bits with the resource types to see if
                        // they are what we want
                        if ((members[i].getType() & resourceType) > 0) {
                            results.add(members[i]);
                        }
                    }
                    return results.toArray();
                }
                return super.getChildren(o);
            }
        };
    }

    /**
     * @param monitor
     * @return
     */
    public boolean finish(IProgressMonitor monitor) {
        return true;
    }

    /**
     * Returns the properties for the repository connection
     * 
     * @return the properties or null
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the properties for the repository connection
     * 
     * @param properties
     *            the properties or null
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public IDialogSettings getDialogSettings() {
        return settings;
    }

    public void setDialogSettings(IDialogSettings settings) {
        this.settings = settings;
    }
}
