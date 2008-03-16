package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class HgCommand {

	protected static class InputStreamConsumer extends Thread {
		private final InputStream stream;
		private byte[] output;
		
		public InputStreamConsumer(InputStream stream) {
			this.stream = new BufferedInputStream(stream);
		}
		
		@Override
		public void run() {
			try {
				int length;
				byte[] buffer = new byte[1024];
				ByteArrayOutputStream output  = new ByteArrayOutputStream();
				while((length = stream.read(buffer)) != -1) {
					output.write(buffer, 0, length);
				}
				stream.close();
				this.output = output.toByteArray();
			} catch (IOException e) {
				// TODO report the error to the caller thread
				MercurialEclipsePlugin.logError(e);
			}
		}
		
		public byte[] getBytes() {
			return output;
		}
		
	}
	
	private final String command;
	private final File workingDir;
	private final boolean escapeFiles;
	private final boolean forceEncoding;
	private final List<String> options = new ArrayList<String>();
	private final List<String> files = new ArrayList<String>();
	
	protected HgCommand(String command, File workingDir, boolean escapeFiles, boolean forceEncoding) {
		this.command = command;
		this.workingDir = workingDir;
		this.escapeFiles = escapeFiles;
		this.forceEncoding = forceEncoding;
	}
	
	protected HgCommand(String command, IContainer container, boolean escapeFiles, boolean forceEncoding) {
		this(
				command,
				container.getLocation().toFile(),
				escapeFiles,
				forceEncoding);
	}
	
	protected String getHgExecutable() {
		return MercurialEclipsePlugin.getDefault()
			.getPreferenceStore()
			.getString(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE);
	}
	
	protected List<String> getCommands() {
		ArrayList<String> result = new ArrayList<String>();
		result.add(getHgExecutable());
		result.add(command);
		result.addAll(options);
		if(forceEncoding) {
			result.add("--encoding");
			result.add("UTF-8");
		}
		if(escapeFiles && !files.isEmpty()) {
			result.add("--");
		}
		result.addAll(files);
		return result;
	}
	
	protected void addOptions(String... options) {
		for(String option: options) {
			this.options.add(option);
		}
	}
	
	protected void addFiles(String... files) {
		for(String file: files) {
			this.files.add(file);
		}
	}
	
	/* TODO the timeout should be configurable, for instance a remote
	 * pull will likely exceed the 5 seconds limit
	 */
	protected byte[] executeToBytes() throws HgException {
		try {
			ProcessBuilder builder = new ProcessBuilder(getCommands());
			builder.redirectErrorStream(true); // makes my life easier
			builder.directory(workingDir);
			Process process = builder.start();
			InputStreamConsumer consumer = new InputStreamConsumer(process.getInputStream());
			consumer.start();
			consumer.join(5000); // 5 seconds timeout
			if(!consumer.isAlive()) {
				if(process.exitValue() == 0) {
					return consumer.getBytes();
				} else {
					throw new HgException("Process error, return code: "+process.exitValue());
				}
			} else {
				process.destroy();
				throw new HgException("Process timeout");
			}
		} catch (IOException e) {
			throw new HgException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new HgException(e.getMessage(), e);
		}
	}
	
	protected String executeToString() throws HgException {
		if(forceEncoding) {
			try {
				return new String(executeToBytes(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// will not happen, UTF-8 is mandatory
				throw new HgException("The JVM does not handle UTF-8", e);
			}
		} else {
			return new String(executeToBytes());
		}
	}
}
