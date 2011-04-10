/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
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

	private static final String KEY_FILES_PER_PROJECT_PREFIX = "projectFiles/";
	private static final String KEY_CS_COMMENT_PREFIX = "changesetComment/";
	private static final String KEY_CS_DEFAULT = "changesetIsDefault/";
	private static final String KEY_CS_LIST = "changesets";

	private WorkingChangeSet defaultChangeset;
	private IProject[] projects;

	private final HgChangeSetContentProvider provider;

	private final UncommittedChangesetGroup group;

	public UncommittedChangesetManager(HgChangeSetContentProvider provider) {
		this.provider = provider;
		group = new UncommittedChangesetGroup(this);
		loadSavedChangesets();
	}

	protected WorkingChangeSet createDefaultChangeset() {
		WorkingChangeSet changeset = new WorkingChangeSet("Default Changeset", group);
		changeset.setDefault(true);
		changeset.setComment("(no commit message)");
		return changeset;
	}

	public UncommittedChangesetGroup getUncommittedGroup(){
		return group;
	}

	private void loadSavedChangesets(){
		Set<WorkingChangeSet> sets = new HashSet<WorkingChangeSet>();
		loadfromPreferences(sets);
		WorkingChangeSet defCs = getDefault(sets);
		if(defCs == null) {
			defCs = createDefaultChangeset();
			sets.add(defCs);
		}
		defaultChangeset = defCs;
		group.setChangesets(sets);
		assignRemainingFiles();
	}

	private void assignRemainingFiles() {
		if(projects == null) {
			return;
		}
		group.update(null, null);
	}

	public void storeChangesets(){
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		Set<ChangeSet> changesets = group.getChangesets();
		Map<IProject, Map<IFile, String>> projectMap = new HashMap<IProject, Map<IFile,String>>();
		StringBuilder changesetNames = new StringBuilder();
		for (ChangeSet changeSet : changesets) {
			String name = changeSet.getName();
			String comment = changeSet.getComment();
			changesetNames.append(name).append(MAPPINGS_SEPARATOR);
			store.putValue(KEY_CS_COMMENT_PREFIX + name, comment);
			if(((WorkingChangeSet)changeSet).isDefault()) {
				store.putValue(KEY_CS_DEFAULT + name, Boolean.TRUE.toString());
			}
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
		store.putValue(KEY_CS_LIST, changesetNames.toString());

		Set<Entry<IProject,Map<IFile,String>>> entrySet = projectMap.entrySet();
		for (Entry<IProject, Map<IFile, String>> entry : entrySet) {
			store.putValue(KEY_FILES_PER_PROJECT_PREFIX + entry.getKey().getName(), encode(entry.getValue()));
		}
	}

	public WorkingChangeSet getDefaultChangeset(){
		return defaultChangeset;
	}

	private void loadfromPreferences(Set<WorkingChangeSet> sets) {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		String changesets = store.getString(KEY_CS_LIST);
		if(!StringUtils.isEmpty(changesets)){
			String[] names = changesets.split(MAPPINGS_SEPARATOR);
			for (String name : names) {
				if(!StringUtils.isEmpty(name)) {
					WorkingChangeSet changeset = new WorkingChangeSet(name, group);
					sets.add(changeset);
				}
			}
		}
		for (WorkingChangeSet changeSet : sets) {
			String comment = store.getString(KEY_CS_COMMENT_PREFIX + changeSet.getName());
			changeSet.setComment(comment);
			boolean isDefault = store.getBoolean(KEY_CS_DEFAULT + changeSet.getName());
			if(isDefault) {
				changeSet.setDefault(true);
			}
		}
		if(projects == null){
			return;
		}
		for (IProject project : projects) {
			String filesStr = store.getString(KEY_FILES_PER_PROJECT_PREFIX + project.getName());
			if(StringUtils.isEmpty(filesStr)){
				continue;
			}
			Map<IFile, String> fileToChangeset = decode(filesStr, project);
			Set<Entry<IFile,String>> entrySet = fileToChangeset.entrySet();
			for (Entry<IFile, String> entry : entrySet) {
				String name = entry.getValue();
				WorkingChangeSet changeset = getChangeset(name, sets);
				if(changeset == null){
					changeset = new WorkingChangeSet(name, group);
					sets.add(changeset);
				}
				changeset.add(entry.getKey());
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

	private static WorkingChangeSet getDefault(Set<WorkingChangeSet> sets){
		for (WorkingChangeSet set : sets) {
			if(set.isDefault()){
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
		if(projects != null){
			assignRemainingFiles();
		}
	}

	public IProject[] getProjects() {
		return projects;
	}

	public void makeDefault(WorkingChangeSet set) {
		if(set == null || !group.getChangesets().contains(set)){
			return;
		}
		defaultChangeset.setDefault(false);
		defaultChangeset = set;
		defaultChangeset.setDefault(true);
	}
}
