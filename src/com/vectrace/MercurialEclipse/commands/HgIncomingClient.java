package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others. All rights
 * reserved. This program and the accompanying materials are made available
 * under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Bastian Doetsch - implementation
 ******************************************************************************/
public class HgIncomingClient extends AbstractParseChangesetClient {

    /**
     * Gets all File Revisions that are incoming and saves them in a bundle
     * file. There can be more than one revision per file as this method obtains
     * all new changesets.
     * 
     * @param proj
     * @param repositories
     * @return Map containing all revisions of the IResources contained in the
     *         Changesets. The sorting is ascending by date.
     * @throws HgException
     */
    public static Map<IPath, SortedSet<ChangeSet>> getHgIncoming(IResource res,
            HgRepositoryLocation repository) throws HgException {
        HgCommand command = new HgCommand("incoming", getWorkingDirectory(res),
                false);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
        try {
            final File bundleFile = File.createTempFile("bundleFile-".concat(
                    res.getProject().getName()).concat("-"), ".tmp", null);
            bundleFile.deleteOnExit();
            
            
            command.addOptions("--debug", "--style",
                    AbstractParseChangesetClient.getStyleFile(true)
                            .getCanonicalPath(), "--bundle", bundleFile
                            .getCanonicalPath());

            URI uri = repository.getUri();
            if (uri != null) {
                command.addOptions(uri.toASCIIString());
            } else {
                command.addOptions(repository.getLocation());
            }            

            String result = command.executeToString();
            if (result.contains("no changes found")) {
                return null;
            }
            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    res, result, true,
                    Direction.INCOMING, repository, bundleFile);
            return revisions;
        } catch (HgException hg) {
            if (hg.getMessage().contains("return code: 1")) {
                return null;
            }
            MercurialEclipsePlugin.logError(hg);
            throw new HgException(hg.getMessage(), hg);
        } catch (IOException e) {
            MercurialEclipsePlugin.logError(e);
            throw new HgException(e.getMessage(), e);
        }
    }
}
