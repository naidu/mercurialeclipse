/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov	 -   implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import com.vectrace.MercurialEclipse.dialogs.RevisionContentProposalProvider.ChangeSetContentProposal;
import com.vectrace.MercurialEclipse.dialogs.RevisionContentProposalProvider.ContentType;
import com.vectrace.MercurialEclipse.model.ChangeSet;


/**
 * @author andrei
 */
public class HistoryContentProposalProvider implements IContentProposalProvider  {

	private final MercurialHistoryPage page;

	/**
	 * @param page non null
	 */
	public HistoryContentProposalProvider(MercurialHistoryPage page) {
		this.page = page;
	}

	public IContentProposal[] getProposals(String contents, int position) {
		List<IContentProposal> result = new LinkedList<IContentProposal>();
		List<MercurialRevision> revisions = page.getMercurialHistory().getRevisions();
		String filter = contents.substring(0, position).toLowerCase();
		for (MercurialRevision revision : revisions) {
			ChangeSet changeSet = revision.getChangeSet();
			if (changeSet.getName().toLowerCase().startsWith(filter)
					|| changeSet.getChangeset().startsWith(filter)) {
				result.add(0, new RevisionContentProposal(revision, ContentType.REVISION));
			}
		}
		// TODO expand proposals to tags/branches/comments etc
		return result.toArray(new IContentProposal[result.size()]);
	}


	public static class RevisionContentProposal extends ChangeSetContentProposal {

		private final MercurialRevision revision;

		/**
		 * @param revision non null
		 * @param type non null
		 */
		public RevisionContentProposal(MercurialRevision revision, ContentType type) {
			super(revision.getChangeSet(), type);
			this.revision = revision;
		}

		/**
		 * @return the revision, never null
		 */
		public MercurialRevision getRevision() {
			return revision;
		}

	}
}
