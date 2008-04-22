package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgIncomingClient {

	public static String template = "{tags};;{rev};;{node|short};;{node};;{date|isodate};;{date|age};;{author|person};;{desc};;{parents};;{files};;!!";

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
	public static Map<IResource, SortedSet<ChangeSet>> getHgIncoming(
			IProject proj, HgRepositoryLocation repository) throws HgException {
		HgCommand command = new HgCommand("incoming", proj, false);
		File bundleFile = getBundleFile(proj, repository);
		File temp = new File(proj.getLocation() + "bundle.temp");
		try {
			command.addOptions("--template", template, "--bundle", temp
					.getCanonicalPath(), repository.getUrl());
			String result = command.executeToString();			
			if (result.contains("no changes found")) {
				return null;
			}
			Map<IResource, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
					result, proj, "!!", template, ";;", bundleFile);
			temp.renameTo(bundleFile);
			return revisions;
		} catch (HgException hg) {
			if (hg.getMessage().contains("return code: 1")) {
				return null;
			}
			throw new HgException(hg.getMessage(), hg);
		} catch (IOException e) {
			throw new HgException(e.getMessage(), e);
		}
	}

	public static File getBundleFile(IProject proj, HgRepositoryLocation loc) {
		String strippedLocation = loc.getUrl().replace('/', '_').replace(':',
				'.');
		return MercurialEclipsePlugin.getDefault().getStateLocation().append(
				MercurialEclipsePlugin.BUNDLE_FILE_PREFIX + "."
						+ proj.getName() + "." + strippedLocation + ".hg")
				.toFile();
	}

	public static Map<IResource, SortedSet<ChangeSet>> createMercurialRevisions(
			String input, IProject proj, String changeSetSeparator,
			String templ, String templateElementSeparator, File bundleFile) {
		String[] changeSetStrings = input.split(changeSetSeparator);

		Map<IResource, SortedSet<ChangeSet>> fileRevisions = new HashMap<IResource, SortedSet<ChangeSet>>();

		for (String changeSet : changeSetStrings) {
			ChangeSet cs = getChangeSet(changeSet, templ,
					templateElementSeparator);

			// add bundle file for being able to look into the bundle.

			cs.setBundleFile(bundleFile);
			if (cs.getChangedFiles() != null) {
				for (String file : cs.getChangedFiles()) {
					IResource res = proj.getFile(file);
					SortedSet<ChangeSet> incomingFileRevs = addChangeSetRevisions(
							fileRevisions, cs, res);
					fileRevisions.put(res, incomingFileRevs);
				}
			}
			SortedSet<ChangeSet> projectRevs = addChangeSetRevisions(
					fileRevisions, cs, proj);
			fileRevisions.put(proj, projectRevs);
		}
		return fileRevisions;
	}

	/**
	 * @param fileRevisions
	 * @param cs
	 * @param res
	 * @return
	 */
	private static SortedSet<ChangeSet> addChangeSetRevisions(
			Map<IResource, SortedSet<ChangeSet>> fileRevisions, ChangeSet cs,
			IResource res) {
		SortedSet<ChangeSet> incomingFileRevs = fileRevisions.get(res);
		if (incomingFileRevs == null) {
			incomingFileRevs = new TreeSet<ChangeSet>();
		}
		incomingFileRevs.add(cs);
		return incomingFileRevs;
	}

	public static ChangeSet getChangeSet(String changeSet, String templ,
			String templateElementSeparator) {
		if (changeSet == null)
			return null;
		String[] templateElements = templ.split(templateElementSeparator);
		Map<String, Integer> templatePositions = new HashMap<String, Integer>(
				templateElements.length);

		int i = 0;
		for (String elem : templateElements) {
			templatePositions.put(elem, Integer.valueOf(i));
			i++;
		}

		String[] changeSetComponents = changeSet
				.split(templateElementSeparator);
		String tag = changeSetComponents[templatePositions.get("{tags}")
				.intValue()];
		String revision = changeSetComponents[templatePositions.get("{rev}")
				.intValue()];
		String nodeShort = changeSetComponents[templatePositions.get(
				"{node|short}").intValue()];
		String node = changeSetComponents[templatePositions.get("{node}")
				.intValue()];
		String date = changeSetComponents[templatePositions.get(
				"{date|isodate}").intValue()];
		String ageDate = changeSetComponents[templatePositions
				.get("{date|age}").intValue()];

		String author = null;
		if (changeSetComponents.length - 1 > templatePositions
				.get("{author|person}")) {

			author = changeSetComponents[templatePositions.get(
					"{author|person}").intValue()];
		}

		String description = "";
		if (changeSetComponents.length - 1 > templatePositions.get("{desc}")) {
			description = changeSetComponents[templatePositions.get("{desc}")
					.intValue()];
		}

		String[] parents = null;
		if (changeSetComponents.length - 1 >= templatePositions
				.get("{parents}")) {
			String string = changeSetComponents[templatePositions.get(
					"{parents}").intValue()];
			Vector<String> split = new Vector<String>(Arrays.asList(string
					.split(" ")));
			for (int j = 0; j < split.size(); j++) {
				String s = split.get(j);				
				if (s.equals(""))
					split.remove(s);
			}
			
			if (split.size()>0){
				parents = split.toArray(new String[split.size()]);
			}

		}

		String[] files = null;
		if (changeSetComponents.length - 1 >= templatePositions.get("{files}")) {
			String filesString = changeSetComponents[templatePositions.get(
					"{files}").intValue()];

			if (filesString != null)
				files = filesString.split(" ");
		}
		ChangeSet cs = new ChangeSet(Integer.parseInt(revision), nodeShort,
				node, tag, author, date, ageDate, files, description, null,
				parents);
		return cs;
	}
}
