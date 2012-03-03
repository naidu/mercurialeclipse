/*******************************************************************************
 * Copyright (c) Subclipse and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     StefanC           - remove empty lines, code cleenup
 *     Jérôme Nègre      - make it work
 *     Bastian Doetsch   - refactorings
 *     Andrei Loskutov   - bug fixes
 *******************************************************************************/

package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.aragost.javahg.commands.AnnotateLine;
import com.aragost.javahg.commands.flags.AnnotateCommandFlags;
import com.vectrace.MercurialEclipse.annotations.AnnotateBlock;
import com.vectrace.MercurialEclipse.annotations.AnnotateBlocks;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgAnnotateClient {

	public static AnnotateBlocks execute(IResource file) throws HgException {

		if (!MercurialTeamProvider.isHgTeamProviderFor(file)) {
			return null;
		}

		HgRoot root = AbstractClient.getHgRoot(file);

		try {
			AnnotateBlocks blocks = new AnnotateBlocks();
			List<AnnotateLine> lines = AnnotateCommandFlags.on(root.getRepository()).execute(
					ResourceUtils.getPath(file).toOSString());

			int i = 0;

			for (AnnotateLine line : lines) {
				blocks.add(new AnnotateBlock(LocalChangesetCache.getInstance().get(root,
						line.getChangeset()), i, i));
				i += 1;
			}

			return blocks;
		} catch (IOException e) {
			throw new HgException("Couldn't get annotation lines", e);
		}
	}
}
