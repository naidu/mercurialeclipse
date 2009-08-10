package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgIgnoreClient {

	public static void addExtension(IFile file) throws HgException {
		addPattern(file.getProject(), "regexp", escape("."+file.getFileExtension())+"$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static void addFile(IFile file) throws HgException {
		String regexp = "^"+escape(file.getProjectRelativePath().toString())+"$"; //$NON-NLS-1$ //$NON-NLS-2$
		addPattern(file.getProject(), "regexp", regexp); //$NON-NLS-1$
	}

	public static void addFolder(IFolder folder) throws HgException {
		String regexp = "^"+escape(folder.getProjectRelativePath().toString())+"$"; //$NON-NLS-1$ //$NON-NLS-2$
		addPattern(folder.getProject(), "regexp", regexp); //$NON-NLS-1$
	}

	public static void addRegexp(IProject project, String regexp) throws HgException {
		addPattern(project, "regexp", regexp); //$NON-NLS-1$
	}

	public static void addGlob(IProject project, String glob) throws HgException {
		addPattern(project, "glob", glob); //$NON-NLS-1$
	}

	private static String escape(String string) {
		StringBuilder result = new StringBuilder();
		int len = string.length();
		for(int i=0; i<len; i++) {
			char c = string.charAt(i);
			switch(c) {
				case '\\':
				case '.':
				case '*':
				case '?':
				case '+':
				case '|':
				case '^':
				case '$':
				case '(':
				case ')':
				case '[':
				case ']':
				case '{':
				case '}':
					result.append('\\');
			}
			result.append(c);
		}
		return result.toString();
	}

	private static void addPattern(IProject project, String syntax, String pattern) throws HgException {
		//TODO use existing sections
	    BufferedOutputStream buffer = null;
		try {
		    File hgignore = new File(HgRootClient.getHgRoot(project), ".hgignore");
		    // append to file if it exists, else create a new one
			buffer = new BufferedOutputStream(new FileOutputStream(hgignore,true));
			// write contents
			buffer.write(new byte[]{'\n','s','y','n','t','a','x',':',' '});
			buffer.write(syntax.getBytes());
			buffer.write('\n');
			buffer.write(pattern.getBytes());
			buffer.flush();
		} catch (CoreException e) {
			throw new HgException(Messages.getString("HgIgnoreClient.failedToAddHgIgnore"), e); //$NON-NLS-1$
		} catch (IOException e) {
			throw new HgException(Messages.getString("HgIgnoreClient.failedToAddEntry"), e); //$NON-NLS-1$
		} finally {
		    // we don't want to leak file descriptors...
		    if (buffer != null) {
		        try {
                    buffer.close();
                } catch (IOException e) {
                   throw new HgException(Messages.getString("HgIgnoreClient.failedToCloseHgIgnore"),e); //$NON-NLS-1$
                }
		    }
		}

	}

}
