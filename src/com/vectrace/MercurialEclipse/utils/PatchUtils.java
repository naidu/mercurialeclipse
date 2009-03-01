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
package com.vectrace.MercurialEclipse.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class PatchUtils {

    /**
     * @param outgoingPatch
     * @return
     * @throws HgException
     */
    public static IFilePatch[] getFilePatches(String outgoingPatch)
            throws HgException {
        if (outgoingPatch == null) {
            return new IFilePatch[0];
        }
        Matcher matcher = PatchUtils.DIFF_START_PATTERN.matcher(outgoingPatch);
        if (matcher.find()) {
            final String strippedPatch = outgoingPatch.substring(matcher.start(),
                    outgoingPatch.length());
            try {
                return PatchUtils.createPatches(strippedPatch);
            } catch (CoreException e) {
                throw new HgException(e);
            }
        }
        return new IFilePatch[0];
    }

    public static IFilePatch[] createPatches(final String patch)
            throws CoreException {
        return ApplyPatchOperation.parsePatch(new IStorage() {
            public InputStream getContents() throws CoreException {
                return new ByteArrayInputStream(patch.getBytes());
            }
    
            public IPath getFullPath() {
                return null;
            }
    
            public String getName() {
                return null;
            }
    
            public boolean isReadOnly() {
                return true;
            }
    
            public Object getAdapter(
                    @SuppressWarnings("unchecked") Class adapter) {
                return null;
            }
        });
    }

    public static final Pattern DIFF_START_PATTERN = Pattern.compile(
    "^diff -r ", Pattern.MULTILINE);

}
