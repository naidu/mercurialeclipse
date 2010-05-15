/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Calls hg root
 *
 * @author bastian
 *
 */
public class HgRootClient extends AbstractClient {

	private static final Map<File, HgRoot> ROOTS = new HashMap<File, HgRoot>();

	/**
	 * @param file
	 * @return hg root as <b>canonical path</b> (see {@link File#getCanonicalPath()})
	 * @throws HgException
	 */
	public static HgRoot getHgRoot(File file) throws HgException {
		File dir = ResourceUtils.getFirstExistingDirectory(file);

		HgRoot hgRoot = getHgRootFromCache(dir);
		if(hgRoot != null){
			return hgRoot;
		}

		// search up the parents recursive (with canonical file) if we see .hg directory there
		File root = findHgDir(getCanonicalPath(dir));
		if (root == null) {
			throw new HgException(file.getName() + Messages.getString("HgRootClient.error.noRoot")); //$NON-NLS-1$
		}
		// .hg parent dire found
		try {
			hgRoot = new HgRoot(root);
		} catch (IOException e) {
			throw new HgException(Messages.getString("HgRootClient.error.cannotGetCanonicalPath")+file.getName()); //$NON-NLS-1$
		}
		ROOTS.put(root, hgRoot);
		return hgRoot;
	}

	private static HgRoot getHgRootFromCache(File dir) throws HgException {
		// test if we have the path "as is" already
		HgRoot hgRoot = ROOTS.get(dir);
		if(hgRoot != null){
			return hgRoot;
		}

		HgRoot testRoot = getCanonicalPath(dir);

		// test with canonical version of the same file
		hgRoot = ROOTS.get(testRoot);
		if(hgRoot != null){
			// remember NON canonical version too
			ROOTS.put(dir, hgRoot);
		}
		return hgRoot;
	}

	private static HgRoot getCanonicalPath(File dir) throws HgException {
		// build canonical path of dir
		try {
			return new HgRoot(dir);
		} catch(IOException e) {
			throw new HgException(Messages.getString("HgRootClient.error.cannotGetCanonicalPath")+dir.getName()); //$NON-NLS-1$
		}
	}

	/**
	 * @param file
	 * @return hg root as <b>canonical path</b> (see {@link File#getCanonicalPath()}), or null
	 * if no hg root can't be found, or exception happens
	 */
	public static HgRoot hasHgRoot(File file) {
		try{
			return getHgRoot(file);
		}catch(HgException hge){
			return null;
		}
	}

	/**
	 * Returns the HgRoot in the specified path, or null if there is not hg root in that path. This method does not recurse in parents to find an HgRoot.
	 */
	public static HgRoot hasHgRootDirect(File file){
		if(!file.isDirectory()){
			return null;
		}

		try{
			HgRoot cached = getHgRootFromCache(file);
			if(cached != null){
				return cached;
			}

			HgRoot canonical = getCanonicalPath(file);
			File[] contents = canonical.listFiles(new HgRootFileFilter());
			if(contents.length == 0){
				return null;
			}
			return canonical;

		}catch(HgException hge){
			return null;
		}
	}

	/**
	 * Searches the .hg directory up in all parents of the given file
	 * @param startDir a directory
	 * @return directory named ".hg" (in any case), or null if the given file does not
	 * have parents wich contains .hg directory
	 */
	private static File findHgDir(File startDir) {
		FileFilter hg = new HgRootFileFilter();
		File root = startDir;
		File[] rootContent = root.listFiles(hg);
		while (rootContent != null && rootContent.length == 0) {
			root = root.getParentFile();
			if (root == null) {
				return null;
			}
			rootContent = root.listFiles(hg);
		}
		if(rootContent != null) {
			return root;
		}
		return null;
	}

	private static class HgRootFileFilter implements FileFilter {
		public boolean accept(File path) {
			return path.getName().equalsIgnoreCase(".hg") && path.isDirectory();
		}
	}
}
