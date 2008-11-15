/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian  implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.dialogs.BookmarkDialog;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.ResourceProperties;

public class BookmarkHandler extends SingleResourceHandler {
    @Override
    protected void run(IResource resource) throws Exception {
        IProject project = resource.getProject();
        
        try {
            if (!MercurialUtilities.isCommandAvailable("bookmarks",
                    ResourceProperties.EXT_BOOKMARKS_AVAILABLE,
                    "hgext.bookmarks=")) {
                Shell shell = getShell();
                MessageDialog
                        .openInformation(
                                shell,
                                "Bookmarks extension not available",
                                "The bookmarks extension is not available in your Mercurial installation.\n"
                                        + "Please consider updating your installation to a Mercurial development version\n"
                                        + "as stable Mercurial releases < 1.1 don't provide this extension.");
            }
        } catch (HgException e) {
            MercurialEclipsePlugin.logError(e);
        }
        
        BookmarkDialog dialog = new BookmarkDialog(getShell(), project);

        if (dialog.open() == IDialogConstants.OK_ID) {
            // do nothing
        }
    }

}
