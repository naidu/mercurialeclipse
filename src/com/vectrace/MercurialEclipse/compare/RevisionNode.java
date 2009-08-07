/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import java.io.InputStream;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.ResourceNode;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.team.IStorageMercurialRevision;

public class RevisionNode extends ResourceNode implements IStreamContentAccessor, ITypedElement {
    private final IStorageMercurialRevision rev;

    public RevisionNode(IStorageMercurialRevision rev) {
        super(rev.getResource());
        this.rev = rev;
    }

    @Override
    public String getName() {
        return rev.getName();
    }

    @Override
    public InputStream getContents() throws CoreException {
        // prefetch byte content
        getContent();
        return super.getContents();
    }

    public int getRevision() {
        return rev.getRevision();
    }

    @Override
    protected InputStream createStream() throws CoreException {
        // System.out.println("Creating stream...");
        return rev.getContents();
    }

    // to avoid FindBugs warnings
    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }
    
    // to avoid FindBugs warnings
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}