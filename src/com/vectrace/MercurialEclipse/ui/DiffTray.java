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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;


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
        Label l = SWTWidgetHelper.createLabel(comp, patch);
        GridData gd = new GridData(SWT.BEGINNING, SWT.TOP, false, true);
        l.setLayoutData(gd);
        return comp;
    }

}
