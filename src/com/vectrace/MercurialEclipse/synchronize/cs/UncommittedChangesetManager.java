/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov (Intland).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov (Intland)	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.IUncommittedChangesetManager;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * @author Andrei
 */
public class UncommittedChangesetManager implements IUncommittedChangesetManager {

	private static final String PATH_NAME_SEPARATOR = "=";

	private static final String MAPPINGS_SEPARATOR = ";";

	private static final String KEY_PREFIX = "projectChangesets/";

	private WorkingChangeSet defaultChangeset;
	private IProject[] projects;

	private final HgChangeSetContentProvider provider;

	private final UncommittedChangesetGroup group;

	public UncommittedChangesetManager(HgChangeSetContentProvider provider) {
		this.provider = provider;
		group = new UncommittedChangesetGroup();
		defaultChangeset = new WorkingChangeSet("Default Changeset");
	}

	public UncommittedChangesetGroup loadChangesets(){
		Set<WorkingChangeSet> sets = new HashSet<WorkingChangeSet>();
		loadfromPreferences(sets);
		assignRemainingFiles();
		sets.add(defaultChangeset);
		group.setChangesets(sets);
		return group;
	}

	private void assignRemainingFiles() {
		// TODO Auto-generated method stub
	}

	public void storeChangesets(){
		Set<ChangeSet> changesets = group.getChangesets();
		Map<IProject, Map<IFile, String>> projectMap = new HashMap<IProject, Map<IFile,String>>();
		for (ChangeSet changeSet : changesets) {
			String name = changeSet.getName();
			Set<IFile> files = changeSet.getFiles();
			for (IFile file : files) {
				IProject project = file.getProject();
				Map<IFile, String> fileToChangeset = projectMap.get(project);
				if(fileToChangeset == null){
					fileToChangeset = new HashMap<IFile, String>();
					projectMap.put(project, fileToChangeset);
				}
				fileToChangeset.put(file, name);
			}
		}
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		Set<Entry<IProject,Map<IFile,String>>> entrySet = projectMap.entrySet();
		for (Entry<IProject, Map<IFile, String>> entry : entrySet) {
			store.putValue(KEY_PREFIX + entry.getKey().getName(), encode(entry.getValue()));
		}
	}

	public WorkingChangeSet getDefaultChangeset(){
		return defaultChangeset;
	}

	private void loadfromPreferences(Set<WorkingChangeSet> sets) {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		for (IProject project : getProjects()) {
			String filesStr = store.getString(KEY_PREFIX + project.getName());
			if(StringUtils.isEmpty(filesStr)){
				continue;
			}
			Map<IFile, String> fileToChangeset = decode(filesStr, project);
			Set<Entry<IFile,String>> entrySet = fileToChangeset.entrySet();
			for (Entry<IFile, String> entry : entrySet) {
				String name = entry.getValue();
				WorkingChangeSet changeset = getChangeset(name, sets);
				if(changeset == null){
					changeset = new WorkingChangeSet(name);
					sets.add(changeset);
				}
				// TODO
//				changeset.addFile(entry.getKey());
			}
		}
	}

	private static WorkingChangeSet getChangeset(String name, Set<WorkingChangeSet> sets){
		for (WorkingChangeSet set : sets) {
			if(name.equals(set.getName())){
				return set;
			}
		}
		return null;
	}

	/**
	 * @param filesStr non null string encoded like "(project_rel_path=changeset_name;)*"
	 * @param project
	 * @return
	 */
	private static Map<IFile, String> decode(String filesStr, IProject project) {
		Map<IFile, String> fileToChangeset = new HashMap<IFile, String>();
		String[] mappings = filesStr.split(MAPPINGS_SEPARATOR);
		for (String mapping : mappings) {
			if(StringUtils.isEmpty(mapping)){
				continue;
			}
			String[] pathAndName = mapping.split(PATH_NAME_SEPARATOR);
			if(pathAndName.length != 2){
				continue;
			}
			IFile file = project.getFile(new Path(pathAndName[0]));
			if(file != null) {
				fileToChangeset.put(file, pathAndName[1]);
			}
		}
		return fileToChangeset;
	}

	private static String encode(Map<IFile, String> fileToChangeset){
		Set<Entry<IFile,String>> entrySet = fileToChangeset.entrySet();
		StringBuilder sb = new StringBuilder();
		for (Entry<IFile, String> entry : entrySet) {
			sb.append(entry.getKey().getLocation()).append(PATH_NAME_SEPARATOR).append(entry.getValue());
			sb.append(MAPPINGS_SEPARATOR);
		}
		return sb.toString();
	}

	public void setProjects(IProject[] projects) {
		this.projects = projects;
	}

	public IProject[] getProjects() {
		return projects;
	}

	public void makeDefault(WorkingChangeSet set) {
		if(set == null || !group.getChangesets().contains(set)){
			return;
		}
		defaultChangeset = set;
	}
}
