/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;

/**
 * @author bastian
 *
 */
public class ResourceUtils {

    /**
     * Checks which editor is active an determines the IResource that is edited.
     */
    public static IResource getActiveResourceFromEditor() {
        IEditorPart editorPart = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getActiveEditor();

        if (editorPart != null) {
            IFileEditorInput input = (IFileEditorInput) editorPart
                    .getEditorInput();
            IFile file = ResourceUtil.getFile(input);
            return file;
        }
        return null;
    }
}
