/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch  -  implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgOutgoingClient extends AbstractParseChangesetClient {
    
    private static final Pattern DIFF_START_PATTERN = Pattern.compile(
            "^diff -r ", Pattern.MULTILINE);

    public static Map<IPath, SortedSet<ChangeSet>> getOutgoing(IResource res,
            HgRepositoryLocation repository) throws HgException {
        try {
            HgCommand command = getCommand(res);
            command.addOptions("--style", AbstractParseChangesetClient //$NON-NLS-1$
                    .getStyleFile(true).getCanonicalPath());
            setRepository(repository, command);
            
            String result = getResult(command);
            if (result == null) {
                return null;
            }
            
            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    res, result, true, Direction.OUTGOING, repository, null,
                    getOutgoingPatches(res, repository));
            return revisions;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }
    
    private static IFilePatch[] getOutgoingPatches(IResource res,
            HgRepositoryLocation repository) throws HgException {
        String outgoingPatch = getOutgoingPatch(res, repository);
        if (outgoingPatch == null) {
            return new IFilePatch[0];
        }
        Matcher matcher = DIFF_START_PATTERN.matcher(outgoingPatch);
        if (matcher.find()) {
            final String strippedPatch = outgoingPatch.substring(matcher.start(),
                    outgoingPatch.length());
            try {
                return createPatches(strippedPatch);
            } catch (CoreException e) {
                throw new HgException(e);
            }
        }
        return new IFilePatch[0];
    }
    
    private static IFilePatch[] createPatches(final String patch)
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

    private static String getResult(HgCommand command) throws HgException {
        try {
            String result = command.executeToString();
            if (result.trim().equals("no changes found")) { //$NON-NLS-1$
                return null;
            }
            return result;
        } catch (HgException hg) {
            if (hg.getMessage().contains("return code: 1")) { //$NON-NLS-1$
                return null;
            }
            throw new HgException(hg.getMessage(), hg);
        }
    }

    private static HgCommand getCommand(IResource res) throws HgException {
        HgCommand command = new HgCommand("outgoing", res.getProject(), //$NON-NLS-1$
                false);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
        return command;
    }

    private static void setRepository(HgRepositoryLocation repository,
            HgCommand command) {
        URI uri = repository.getUri();
        if (uri != null) {
            command.addOptions(uri.toASCIIString());
        } else {
            command.addOptions(repository.getLocation());
        }
    }
    
    private static String getOutgoingPatch(IResource res, HgRepositoryLocation repository) throws HgException {
        HgCommand command = getCommand(res);
        command.addOptions("-p");
        setRepository(repository, command);
        return getResult(command);
    }

}
