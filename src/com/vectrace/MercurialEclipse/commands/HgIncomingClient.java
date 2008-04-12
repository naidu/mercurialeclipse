package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgIncomingClient {

	public static String template = "{tags};;{rev};;{node|short};;{node};;{date|isodate};;{date|age};;{author|person};;{description};;{files};;!!";

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
		File bundleFile = getBundleFile(proj);
		File temp = new File(proj.getLocation() + "bundle.temp");
		try {
			command.addOptions("--template", template, "--bundle", temp
					.getCanonicalPath(), repository.getUrl());
			String result = command.executeToString();
			result = result.substring(result.lastIndexOf("\n") + 1);
			if (result.contains("no changes found")) {
				return null;
			}
			Map<IResource, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
					result, proj, "!!", template, ";;");
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

	public static File getBundleFile(IProject proj) {
		return MercurialEclipsePlugin.getDefault().getStateLocation().append(
				MercurialEclipsePlugin.BUNDLE_FILE_PREFIX + "."
						+ proj.getName() + ".hg").toFile();
	}

	public static Map<IResource, SortedSet<ChangeSet>> createMercurialRevisions(
			String input, IProject proj, String changeSetSeparator,
			String templ, String templateElementSeparator) {
		String[] changeSetStrings = input.split(changeSetSeparator);

		Map<IResource, SortedSet<ChangeSet>> fileRevisions = new HashMap<IResource, SortedSet<ChangeSet>>();

		for (String changeSet : changeSetStrings) {
			ChangeSet cs = getChangeSet(changeSet, templ,
					templateElementSeparator);
			if (cs.getChangedFiles() != null) {
				for (String file : cs.getChangedFiles()) {
					IResource res = proj.getFile(file);
					SortedSet<ChangeSet> incomingFileRevs = fileRevisions
							.get(res);
					if (incomingFileRevs == null) {
						incomingFileRevs = new TreeSet<ChangeSet>();
					}
					incomingFileRevs.add(cs);
					fileRevisions.put(res, incomingFileRevs);
				}
			}
		}
		return fileRevisions;
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
		String author = changeSetComponents[templatePositions.get(
				"{author|person}").intValue()];
		String description = "";
		if (changeSetComponents.length - 1 > templatePositions
				.get("{description}")) {
			description = changeSetComponents[templatePositions.get(
					"{description}").intValue()];
		}

		String[] files = null;
		if (changeSetComponents.length - 1 >= templatePositions.get("{files}")) {
			String filesString = changeSetComponents[templatePositions.get(
					"{files}").intValue()];

			files = filesString.split(" ");
		}
		ChangeSet cs = new ChangeSet(Integer.parseInt(revision), nodeShort,
				node, tag, author, date, ageDate, files, description);
		return cs;
	}
}
