package com.vectrace.MercurialEclipse.team;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Scanner;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgIdentClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;

public class MercurialStatusCache extends Observable {
	//relative order for folders
	public final static int BIT_IGNORE     = 0;
	public final static int BIT_CLEAN      = 1;
	public final static int BIT_DELETED    = 2;
	public final static int BIT_REMOVED    = 3;
	public final static int BIT_UNKNOWN    = 4;
	public final static int BIT_ADDED      = 5;
	public final static int BIT_MODIFIED   = 6;
	public final static int BIT_IMPOSSIBLE = 7;
	
	private static MercurialStatusCache instance;
	
	private MercurialStatusCache() {
		
	}
	
	public static MercurialStatusCache getInstance() {
		if (instance == null){
			instance = new MercurialStatusCache();
		}
		return instance;
	}
	
	
	
	/** Used to store the last known status of a resource */
	private static Map<IResource, BitSet> statusMap = new HashMap<IResource, BitSet>();

	/** Used to store which projects have already been parsed */
	private static Set<IProject> knownStatus = new HashSet<IProject>();

	private static Map<IProject, String> versions = new HashMap<IProject, String>();

	/** 
	 * Clears the known status of all resources and projects.
	 * and calls for a update of decoration
	 *  
	 */
	public void clear() {
		/* While this clearing of status is a "naive" implementation, it is simple. */
		statusMap.clear();
		knownStatus.clear();
		setChanged();
		notifyObservers(knownStatus.toArray(new IProject[knownStatus.size()]));
	}
	
	public boolean isStatusKnown(IProject project){
		return knownStatus.contains(project);
	}

	public BitSet getStatus(IResource objectResource) {
		return statusMap.get(objectResource);
	}

	public boolean isVersionKnown(IResource objectResource) {
		return versions.containsKey(objectResource);
	}

	public String getVersion(IResource objectResource) {
		return versions.get(objectResource);
	}

	public void refresh(IProject project) throws TeamException {
		/* hg status on project (all files) instead of per file basis*/
		try {
			// set version
			versions.put(project, HgIdentClient.getCurrentRevision(project));
			
			// set status
			parseStatusCommand(project, HgStatusClient.getStatus(project));
			setChanged();
			notifyObservers(project);
		} catch (HgException e) {
			throw new TeamException(e.getMessage(),e);
		}
	}
	
	/**
	 * @param output
	 */
	private void parseStatusCommand(IProject ctr, String output) {
		IContainer ctrParent = ctr.getParent();
		knownStatus.add(ctr);
		Scanner scanner = new Scanner(output);
		while (scanner.hasNext()) {
			String status = scanner.next();
			String localName = scanner.nextLine();
			IResource member = ctr.getFile(localName.trim());

			BitSet bitSet = new BitSet();
			bitSet.set(getBitIndex(status.charAt(0)));
			statusMap.put(member, bitSet);
			
			//ancestors
			for (IResource parent = member.getParent(); parent != ctrParent; parent = parent.getParent()) {
				BitSet parentBitSet = statusMap.get(parent);
				if(parentBitSet!=null) {
					bitSet = (BitSet)bitSet.clone();
					bitSet.or(parentBitSet);
				}
				statusMap.put(parent, bitSet);
			}
		}
	}
	
	private final int getBitIndex(char status) {
		switch(status) {
			case '!':
				return BIT_DELETED;
			case 'R':
				return BIT_REMOVED;
			case 'I':
				return BIT_IGNORE;
			case 'C':
				return BIT_CLEAN;
			case '?':
				return BIT_UNKNOWN;
			case 'A':
				return BIT_ADDED;
			case 'M':
				return BIT_MODIFIED;
			default:
				MercurialEclipsePlugin.logWarning("Unknown status: '"+status+"'", null);
				return BIT_IMPOSSIBLE;
		}
	}

	public void refresh() throws TeamException {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			refresh(project);
		}
	}
	
}
