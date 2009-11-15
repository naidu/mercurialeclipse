/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipant;
import org.eclipse.team.core.mapping.ISynchronizationScopeParticipantFactory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.synchronize.RepositorySynchronizationScope;

/**
 * @author Andrei
 *
 */
public class HgChangeSetModelProvider extends ModelProvider {

	public final static String ID = "com.vectrace.MercurialEclipse.changeSetModel";
	private static HgChangeSetModelProvider provider;

	public HgChangeSetModelProvider() {
		super();
	}

	public static HgChangeSetModelProvider getProvider() {
		if (provider == null) {
			try {
				provider = (HgChangeSetModelProvider) ModelProvider.getModelProviderDescriptor(ID)
						.getModelProvider();
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return provider;
	}

	public static class HgModelScopeParticipantFactory implements
			ISynchronizationScopeParticipantFactory, IAdapterFactory {

		public HgModelScopeParticipantFactory() {

		}

		public ISynchronizationScopeParticipant createParticipant(ModelProvider provider1,
				ISynchronizationScope scope) {
			RepositorySynchronizationScope rscope = (RepositorySynchronizationScope) scope;
			return rscope.getSubscriber().getParticipant();
		}

		@SuppressWarnings("unchecked")
		public Object getAdapter(Object adaptableObject, Class adapterType) {
			if (adaptableObject instanceof ModelProvider) {
				ModelProvider provider1 = (ModelProvider) adaptableObject;
				if (provider1.getDescriptor().getId().equals(ID)) {
//					if (adapterType == IResourceMappingMerger.class) {
//						return new DefaultResourceMappingMerger((ModelProvider)adaptableObject);
//					}
					if (adapterType == ISynchronizationScopeParticipantFactory.class) {
						return this;
					}
				}
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		public Class[] getAdapterList() {
			return new Class[] {
//					IResourceMappingMerger.class,
//					ISynchronizationCompareAdapter.class,
					ISynchronizationScopeParticipantFactory.class
				};
		}

	}

}
