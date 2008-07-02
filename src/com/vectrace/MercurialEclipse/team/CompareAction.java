/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.compare.RevisionNode;
import com.vectrace.MercurialEclipse.utils.CompareUtils;

/**
 * @author zingo, Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class CompareAction extends SingleFileAction {

    @Override
    protected void run(IFile file) throws TeamException {
        // workspace version
        ResourceNode leftNode = new ResourceNode(file);

        // mercurial version
        RevisionNode rightNode = new RevisionNode(
                new IStorageMercurialRevision(file));
        CompareUtils.openEditor(leftNode, rightNode, false);
    }

}
