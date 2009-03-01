/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import static com.vectrace.MercurialEclipse.ui.SWTWidgetHelper.getFillGD;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author bastian
 * 
 */
public class DiffTray extends org.eclipse.jface.dialogs.DialogTray {

    private String patch;

    /**
     * 
     */
    public DiffTray(String patch) {
        this.patch = patch;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.dialogs.DialogTray#createContents(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite comp = SWTWidgetHelper.createComposite(parent, 1);

        ISourceViewer diffContent = new SourceViewer(comp, null, SWT.V_SCROLL
                | SWT.MULTI | SWT.BORDER);
        diffContent.setEditable(false);
        diffContent.getTextWidget().setLayoutData(getFillGD(150));
        Document doc = new Document();
        doc.set(patch);
        diffContent.setDocument(doc);
        diffContent.activatePlugins();
        return comp;
    }

}
