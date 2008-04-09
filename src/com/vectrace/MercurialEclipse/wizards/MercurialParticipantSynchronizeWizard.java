/**
 * 
 */
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.SubscriberParticipantWizard;

/**
 * @author bastian
 *
 */
public class MercurialParticipantSynchronizeWizard extends SubscriberParticipantWizard implements IWizard {
	private IWizard importWizard = new CloneRepoWizard();

	@Override
	protected SubscriberParticipant createParticipant(ISynchronizeScope scope) {
		
		return null;
	}

	@Override
	protected IWizard getImportWizard() {
		return importWizard;
	}

	@Override
	protected String getPageTitle() {
		return "Mercurial Synchronization Wizard";
	}

	@Override
	protected IResource[] getRootResources() {
		return ResourcesPlugin.getWorkspace().getRoot().getProjects();
	}
	
	

}
