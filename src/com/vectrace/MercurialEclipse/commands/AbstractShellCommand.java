/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation (with lots of stuff pulled up from HgCommand)
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland) - bug fixes/restructure
 *     Zsolt Koppany (Intland) - enhancements
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgCoreException;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
public abstract class AbstractShellCommand extends AbstractClient {

	public static final int DEFAULT_TIMEOUT = 360000;

	private static final int BUFFER_SIZE = 32768;

	// private static final Object executionLock = new Object();

	// should not extend threads directly, should use thread pools or jobs.
	// In case many threads created at same time, VM can crash or at least get OOM
	private static class InputStreamConsumer extends Job {

		static {
			// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=298795
			// we must run this stupid code in the UI thread
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					PlatformUI.getWorkbench().getProgressService().registerIconForFamily(
							MercurialEclipsePlugin.getImageDescriptor("mercurialeclipse.png"),
							InputStreamConsumer.class);
				}
			});
		}

		private final InputStream stream;
		private final OutputStream output;
		volatile IProgressMonitor monitor2;
		volatile boolean started;
		private final Process process;

		public InputStreamConsumer(String name, Process process, OutputStream output) {
			super(name);
			this.process = process;
			this.output = output;
			this.stream = process.getInputStream();
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			started = true;
			monitor2 = monitor;
			try {
				int length;
				byte[] buffer = new byte[BUFFER_SIZE];
				while ((length = stream.read(buffer)) != -1 && !monitor.isCanceled()) {
					output.write(buffer, 0, length);
				}
			} catch (IOException e) {
				if (!monitor.isCanceled()) {
					HgClients.logError(e);
				}
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					HgClients.logError(e);
				}
				try {
					output.close();
				} catch (IOException e) {
					HgClients.logError(e);
				}
				if(monitor.isCanceled()) {
					process.destroy();
				}
				monitor2 = null;
			}
			return monitor.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

		private boolean isAlive() {
			// job is either not started yet (is scheduled and waiting), or it is not finished or cancelled yet
			return (!started && getResult() == null) || (monitor2 != null && !monitor2.isCanceled());
		}

		@Override
		public boolean belongsTo(Object family) {
			return InputStreamConsumer.class == family;
		}

	}

	public static final int MAX_PARAMS = 120;

	protected String command;
	protected List<String> commands;
	protected boolean escapeFiles;
	protected List<String> options;
	protected File workingDir;
	protected final List<String> files;
	private String timeoutConstant;
	private InputStreamConsumer consumer;
	private Process process;
	private boolean showOnConsole;
	private final boolean debugMode;
	private final boolean debugExecTime;

	protected AbstractShellCommand() {
		super();
		options = new ArrayList<String>();
		files = new ArrayList<String>();
		showOnConsole = true;
		debugMode = Boolean.valueOf(HgClients.getPreference(PREF_CONSOLE_DEBUG, "false")).booleanValue(); //$NON-NLS-1$
		debugExecTime = Boolean.valueOf(HgClients.getPreference(PREF_CONSOLE_DEBUG_TIME, "false")).booleanValue(); //$NON-NLS-1$
	}

	public AbstractShellCommand(List<String> commands, File workingDir, boolean escapeFiles) {
		this();
		this.escapeFiles = escapeFiles;
		this.workingDir = workingDir;
		this.commands = commands;
	}

	public void addOptions(String... optionsToAdd) {
		for (String option : optionsToAdd) {
			options.add(option);
		}
	}

	public byte[] executeToBytes() throws HgException {
		int timeout = DEFAULT_TIMEOUT;
		if (timeoutConstant != null) {
			timeout = HgClients.getTimeOut(timeoutConstant);
		}
		return executeToBytes(timeout);
	}

	public byte[] executeToBytes(int timeout) throws HgException {
		return executeToBytes(timeout, true);
	}

	/**
	 * Execute a command.
	 *
	 * @param timeout
	 *            -1 if no timeout, else the timeout in ms.
	 */
	public byte[] executeToBytes(int timeout, boolean expectPositiveReturnValue) throws HgException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(BUFFER_SIZE);
		if (executeToStream(bos, timeout, expectPositiveReturnValue)) {
			return bos.toByteArray();
		}
		return null;
	}

	public boolean executeToStream(OutputStream output, int timeout, boolean expectPositiveReturnValue)
			throws HgException {
		try {
			List<String> cmd = getCommands();

			final String commandInvoked = getCommandInvoked(cmd);

			// This is totally
			Charset charset = null;
			if (workingDir != null) {
				HgRoot hgRoot;
				try {
					hgRoot = HgClients.getHgRoot(workingDir);
					charset = hgRoot.getEncoding();
					// Enforce strict command line encoding
					cmd.add(1, charset.name());
					cmd.add(1, "--encoding");
					// Enforce fallback encoding for UI (command output)
					// Note: base encoding is UTF-8 for mercurial, fallback is only take into account
					// if actual platfrom don't support it.
					cmd.add(1, "ui.fallbackencoding=" + hgRoot.getFallbackencoding().name()); //$NON-NLS-1$
					cmd.add(1, "--config"); //$NON-NLS-1$
				} catch (HgCoreException e) {
					// no hg root found
				}
			}

			ProcessBuilder builder = new ProcessBuilder(cmd);

			// set locale to english have deterministic output
			Map<String, String> env = builder.environment();
			env.put("LANG", "C"); //$NON-NLS-1$ //$NON-NLS-2$
			env.put("LANGUAGE", "C"); //$NON-NLS-1$ //$NON-NLS-2$
			if (charset != null) {
				env.put("HGENCODING", charset.name()); //$NON-NLS-1$
			}

			builder.redirectErrorStream(true); // makes my life easier
			if (workingDir != null) {
				builder.directory(workingDir);
			}
			final String msg;
			// TODO Andrei: I see sometimes that hg has errors if it runs in parallel
			// locking here would serialize all hg access from plugin.
			// not sure if it is worth...
			//synchronized (executionLock) {
				process = builder.start();
				consumer = new InputStreamConsumer(commandInvoked, process, output);
				long startTime;
				if(debugExecTime) {
					startTime = System.currentTimeMillis();
				} else {
					startTime = 0;
				}
				consumer.schedule();
				logConsoleCommandInvoked(commandInvoked);
				waitForConsumer(timeout); // 30 seconds timeout
				msg = getMessage(output);
				if (!consumer.isAlive()) {
					final int exitCode = process.waitFor();
					// everything fine
					if (exitCode == 0 || !expectPositiveReturnValue) {
						if (debugExecTime || debugMode) {
							long timeInMillis = debugExecTime? System.currentTimeMillis() - startTime : 0;
							logConsoleCompleted(timeInMillis, msg, exitCode, null);
						}
						return true;
					}

					// exit code > 0
					final HgException hgex = new HgException(exitCode, getMessage(output));
					long timeInMillis = debugExecTime? System.currentTimeMillis() - startTime : 0;
					// exit code == 1 usually isn't fatal.
					logConsoleCompleted(timeInMillis, msg, exitCode, hgex);
					throw hgex;
				}
			//}
			// command timeout
			final HgException hgEx = new HgException("Process timeout"); //$NON-NLS-1$
			logConsoleError(msg, hgEx);
			throw hgEx;
		} catch (IOException e) {
			throw new HgException(e.getMessage(), e);
		} catch (InterruptedException e) {
			String message = e.getMessage();
			if(message == null || message.length() == 0){
				message = "Operation cancelled";
			}
			throw new HgException(message, e);
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	private void waitForConsumer(int timeout) throws InterruptedException {
		if (timeout <= 0) {
			throw new IllegalArgumentException("Illegal timeout: " + timeout);
		}
		long start = System.currentTimeMillis();
		long now = 0;
		while (consumer.isAlive()) {
			long delay = timeout - now;
			if (delay <= 0) {
				break;
			}
			synchronized (this){
				wait(10);
			}
			now = System.currentTimeMillis() - start;
		}
	}

	protected void logConsoleCommandInvoked(final String commandInvoked) {
		if (showOnConsole) {
			getConsole().commandInvoked(commandInvoked);
		}
	}

	protected void logConsoleMessage(final String msg, final Throwable t) {
		if (showOnConsole) {
			getConsole().printMessage(msg, t);
		}
	}

	protected void logConsoleError(final String msg, final HgException hgEx) {
		if (showOnConsole) {
			if (msg != null) {
				getConsole().printError(msg, hgEx);
			} else {
				getConsole().printError(hgEx.getMessage(), hgEx);
			}
		}
	}

	private void logConsoleCompleted(final long timeInMillis, final String msg, final int exitCode, final HgException hgex) {
		if (showOnConsole) {
			getConsole().commandCompleted(exitCode, timeInMillis, msg, hgex);
		}
	}

	private static String getMessage(OutputStream output) {
		String msg = null;
		if (output instanceof FileOutputStream) {
			return null;
		} else if (output instanceof ByteArrayOutputStream) {
			ByteArrayOutputStream baos = (ByteArrayOutputStream) output;
			try {
				msg = baos.toString(MercurialEclipsePlugin.getDefaultEncoding());
			} catch (UnsupportedEncodingException e) {
				MercurialEclipsePlugin.logError(e);
				msg = baos.toString();
			}
		}
		return msg;
	}

	public String executeToString() throws HgException {
		byte[] bytes = executeToBytes();
		if (bytes != null && bytes.length > 0) {
			try {
				return new String(bytes, MercurialEclipsePlugin.getDefaultEncoding());
			} catch (UnsupportedEncodingException e) {
				throw new HgException(e.getLocalizedMessage(), e);
			}
		}
		return ""; //$NON-NLS-1$
	}

	public boolean executeToFile(File file, int timeout, boolean expectPositiveReturnValue) throws HgException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, false);
			return executeToStream(fos, timeout, expectPositiveReturnValue);
		} catch (FileNotFoundException e) {
			throw new HgException(e.getMessage(), e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					throw new HgException(e.getMessage(), e);
				}
			}
		}
	}

	protected List<String> getCommands() {
		if (commands != null) {
			return commands;
		}
		ArrayList<String> result = new ArrayList<String>();
		result.add(getExecutable());
		result.add(command);
		result.addAll(options);
		if (escapeFiles && !files.isEmpty()) {
			result.add("--"); //$NON-NLS-1$
		}
		result.addAll(files);
		// TODO check that length <= MAX_PARAMS
		return result;
	}

	protected abstract String getExecutable();

	public void addFiles(String... myFiles) {
		for (String file : myFiles) {
			files.add(file);
		}
	}

	public void addFiles(Collection<String> myFiles) {
		files.addAll(myFiles);
	}

	public void addFiles(IResource... resources) {
		for (IResource resource : resources) {
			files.add(resource.getLocation().toOSString());
		}
	}

	public void addFiles(List<? extends IResource> resources) {
		for (IResource resource : resources) {
			files.add(resource.getLocation().toOSString());
		}
	}

	public void addFiles(Set<IPath> paths) {
		for (IPath path : paths) {
			files.add(path.toOSString());
		}
	}

	public void setUsePreferenceTimeout(String cloneTimeout) {
		this.timeoutConstant = cloneTimeout;
	}

	public void terminate() {
		if (consumer != null) {
			consumer.cancel();
		}
		process.destroy();
	}

	private IConsole getConsole() {
		return HgClients.getConsole();
	}

	public void setShowOnConsole(boolean b) {
		showOnConsole = b;
	}

	private String getCommandInvoked(List<String> cmd) {
		if(cmd.isEmpty()){
			// paranoia
			return "<empty command>";
		}
		StringBuilder sb = new StringBuilder();
		if(workingDir != null){
			sb.append(workingDir);
			sb.append(File.separatorChar);
		}
		String exec = cmd.get(0);
		exec = exec.replace('\\', '/');
		int lastSep = exec.lastIndexOf('/');
		if(lastSep <= 0){
			sb.append(exec);
		} else {
			// just the exec. name, not the full path
			if(exec.endsWith(".exe")){
				sb.append(exec.substring(lastSep + 1, exec.length() - 4));
			} else {
				sb.append(exec.substring(lastSep + 1));
			}
		}
		for (int i = 1; i < cmd.size(); i++) {
			sb.append(" ").append(cmd.get(i));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName());
		builder.append(" [");
		if (command != null) {
			builder.append("command=");
			builder.append(command);
			builder.append(", ");
		}
		if (commands != null) {
			builder.append("commands=");
			builder.append(commands);
			builder.append(", ");
		}
		if (options != null) {
			builder.append("options=");
			builder.append(options);
			builder.append(", ");
		}
		if (workingDir != null) {
			builder.append("workingDir=");
			builder.append(workingDir);
			builder.append(", ");
		}
		if (files != null) {
			builder.append("files=");
			builder.append(files);
			builder.append(", ");
		}
		builder.append("escapeFiles=");
		builder.append(escapeFiles);
		builder.append(", ");
		if (consumer != null) {
			builder.append("consumer=");
			builder.append(consumer);
			builder.append(", ");
		}
		if (process != null) {
			builder.append("process=");
			builder.append(process);
			builder.append(", ");
		}
		builder.append("showOnConsole=");
		builder.append(showOnConsole);
		builder.append(", ");
		if (timeoutConstant != null) {
			builder.append("timeoutConstant=");
			builder.append(timeoutConstant);
		}
		builder.append("]");
		return builder.toString();
	}
}
