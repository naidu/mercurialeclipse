package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgIgnoreClient {

	public static void addExtension(IFile file) throws HgException {
		addPattern(file.getProject(), "regexp", escape("."+file.getFileExtension())+"$");
	}

	public static void addFile(IFile file) throws HgException {
		String regexp = "^"+escape(file.getProjectRelativePath().toString())+"$";
		addPattern(file.getProject(), "regexp", regexp);
	}
	
	public static void addFolder(IFolder folder) throws HgException {
		String regexp = "^"+escape(folder.getProjectRelativePath().toString())+"$";
		addPattern(folder.getProject(), "regexp", regexp);
	}
	
	public static void addRegexp(IProject project, String regexp) throws HgException {
		addPattern(project, "regexp", regexp);
	}
	
	public static void addGlob(IProject project, String glob) throws HgException {
		addPattern(project, "glob", glob);
	}
	
	private static String escape(String string) {
		StringBuilder result = new StringBuilder();
		int len = string.length();
		for(int i=0; i<len; i++) {
			char c = string.charAt(i);
			switch(c) {
				//TODO a lot are missing
				case '\\':
				case '.':
				case '^':
				case '$':
					result.append('\\');
				default:
					result.append(c);
			}
		}
		return result.toString();
	}
	
	private static void addPattern(IProject project, String syntax, String pattern) throws HgException {
		//TODO use existing sections
		try {
			IFile hgignore = project.getFile(".hgignore");
			hgignore.refreshLocal(IResource.DEPTH_ZERO, null);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			boolean exists = hgignore.exists();
			if(exists) {
				//read current patterns
				BufferedInputStream content = new BufferedInputStream(hgignore
						.getContents());
				try {
					byte[] b = new byte[1024];
					int len;
					while ((len = content.read(b)) != -1) {
						buffer.write(b, 0, len);
					}
				} finally {
					content.close();
				}
			}
			//add the new one
			buffer.write(new byte[]{'\n','s','y','n','t','a','x',':',' '});
			buffer.write(syntax.getBytes());
			buffer.write('\n');
			buffer.write(pattern.getBytes());
			//write to .hgignore
			ByteArrayInputStream input = new ByteArrayInputStream(buffer.toByteArray());
			if(exists) {
				hgignore.setContents(input, false, true, null);
			} else {
				hgignore.create(input, false, null);
			}
		} catch (CoreException e) {
			throw new HgException("Failed to add an entry to .hgignore", e);
		} catch (IOException e) {
			throw new HgException("Failed to add an entry to .hgignore", e);
		}
		
	}
	
}
