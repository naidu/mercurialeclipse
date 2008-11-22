/*******************************************************************************
 * Copyright (c) 2005-2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author bastian
 * 
 */
public class Ancestor {
    private HgRoot root;
    private ChangeSet cs1;
    private ChangeSet cs2;
    private ChangeSet ancestor;
    private final static Map<String, ChangeSet> CACHE = new HashMap<String, ChangeSet>();

    public Ancestor(HgRoot root, ChangeSet cs1, ChangeSet cs2) {
        assert (cs1 != null || cs2 != null);
        this.root = root;
        if (cs1 != null && cs2 != null
                && cs1.getChangesetIndex() < cs2.getChangesetIndex()) {
            this.cs1 = cs1;
            this.cs2 = cs2;
        } else {
            this.cs1 = cs2;
            this.cs2 = cs1;
        }
    }

    public ChangeSet get() throws HgException {
        if (ancestor == null) {
            // determine ancestor if both changesets are set.
            if (cs1 != null && cs2 != null) {
                ancestor = CACHE.get(getKey());
                if (ancestor == null) {
                    int ancestorIndex = HgParentClient.findCommonAncestor(root,
                            cs1, cs2);
                    ancestor = LocalChangesetCache.getInstance()
                            .getLocalChangeSet(
                                    MercurialUtilities.convert(root),
                                    String.valueOf(ancestorIndex));
                    CACHE.put(getKey(), ancestor);
                }
            } else {
                // determine changeset that isn't null
                if (cs1 != null) {
                    ancestor = cs1;
                } else {
                    ancestor = cs2;
                }
            }
        }
        return ancestor;
    }

    /**
     * @return
     */
    private String getKey() {
        return cs1.getChangeset() + "|" + cs2.getChangeset(); //$NON-NLS-1$
    }

}
