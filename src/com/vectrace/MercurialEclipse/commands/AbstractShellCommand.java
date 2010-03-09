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

import static com.vectrace.MercurialEclipse.MercurialEclipsePlugin.*;
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
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.exception.HgCoreException;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 */
public abstract class AbstractShellCommand extends AbstractClient {

	public static final int DEFAULT_TIMEOUT = 360000;

	private static final int BUFFER_SIZE = 32768;

	public static final int MAX_PARAMS = 120;

	static {
		// see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=298795
		// we must run this stupid code in the UI thread
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().getProgressService().registerIconForFamily(
						getImageDescriptor("mercurialeclipse.png"),
						AbstractShellCommand.class);
			}
		});
	}

	/**
	 * This rule disallows hg commands run in parallel if the hg root is specified.
	 * If the hg root is not set, then this rule allows parallel job execution.
	 */
	public static class DefaultExecutionRule implements ISchedulingRule {
		protected volatile HgRoot hgRoot;

		public DefaultExecutionRule() {
			super();
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return contains(rule);
		}

		public boolean contains(ISchedulingRule rule) {
			if(this == rule){
				return true;
			}
			if(!(rule instanceof DefaultExecutionRule)){
				return false;
			}
			DefaultExecutionRule rule2 = (DefaultExecutionRule) rule;
			return hgRoot != null && hgRoot.equals(rule2.hgRoot);
		}
	}

	/**
	 * This rule disallows hg commands run in parallel on the same hg root
	 */
	public static class ExclusiveExecutionRule extends DefaultExecutionRule {
		/**
		 * @param hgRoot must be not null
		 */
		public ExclusiveExecutionRule(HgRoot hgRoot) {
			super();
			Assert.isNotNull(hgRoot);
			this.hgRoot = hgRoot;
		}
	}

	// should not extend threads directly, should use thread pools or jobs.
	// In case many threads created at same time, VM can crash or at least get OOM
	class ProzessWrapper extends Job {

		private final OutputStream output;
		private final ProcessBuilder builder;
		private final DefaultExecutionRule execRule;
		volatile IProgressMonitor monitor2;
		volatile boolean started;
		private Process process;
		long startTime;
		int exitCode = -1;
		Throwable error;

		public ProzessWrapper(String name, ProcessBuilder builder, OutputStream output) {
			super(name);
			execRule = getExecutionRule();
			setRule(execRule);
			this.builder = builder;
			this.output = output;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			if(debugExecTime) {
				startTime = System.currentTimeMillis();
			} else {
				startTime = 0;
			}
			started = true;
			monitor2 = monitor;
			InputStream stream = null;
			try {
				process = builder.start();
				stream = process.getInputStream();
				int length;
				byte[] buffer = new byte[BUFFER_SIZE];
				while ((length = stream.read(buffer)) != -1) {
					output.write(buffer, 0, length);
					if(monitor.isCanceled()){
						break;
					}
				}
				exitCode = process.waitFor();
			} catch (IOException e) {
				if (!monitor.isCanceled()) {
					error = e;
				}
				return Status.CANCEL_STATUS;
			} catch (InterruptedException e) {
				if (!monitor.isCanceled()) {
					error = e;
				}
				return Status.CANCEL_STATUS;
			} finally {
				if(stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						HgClients.logError(e);
					}
				}
				try {
					output.close();
				} catch (IOException e) {
					HgClients.logError(e);
				}
				monitor.done();
				monitor2 = null;
				if(process != null) {
					process.destroy();
				}
			}
			return monitor.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
		}

		private boolean isAlive() {
			// job is either not started yet (is scheduled and waiting), or it is not finished or cancelled yet
			return (!started && getResult() == null) || (monitor2 != null);
		}

		@Override
		public boolean belongsTo(Object family) {
			return AbstractShellCommand.class == family;
		}

		@Override
		protected void canceling() {
			super.canceling();
			if(process != null) {
				process.destroy();
			}
			// remove exclusive lock on the hg root
			execRule.hgRoot = null;
		}
	}

	protected String command;
	protected List<String> commands;
	protected boolean escapeFiles;
	protected List<String> options;
	protected File workingDir;
	protected final List<String> files;
	private String timeoutConstant;
	private ProzessWrapper processWrapper;
	private boolean showOnConsole;
	private final boolean debugMode;
	private final boolean debugExecTime;

	private HgRoot hgRoot;

	private DefaultExecutionRule executionRule;

	protected AbstractShellCommand() {
		super();
		options = new ArrayList<String>();
		files = new ArrayList<String>();
		showOnConsole = true;
		debugMode = Boolean.valueOf(HgClients.getPreference(PREF_CONSOLE_DEBUG, "false")).booleanValue(); //$NON-NLS-1$
		debugExecTime = Boolean.valueOf(HgClients.getPreference(PREF_CONSOLE_DEBUG_TIME, "false")).booleanValue(); //$NON-NLS-1$
	}

	protected AbstractShellCommand(List<String> commands, File workingDir, boolean escapeFiles) {
		this();
		this.escapeFiles = escapeFiles;
		this.workingDir = workingDir;
		this.commands = commands;
	}

	/**
	 * Per default, a non-exclusive rule is created
	 * @return rule for hg job execution, never null
	 */
	protected DefaultExecutionRule getExecutionRule() {
		if(executionRule == null) {
			executionRule = new DefaultExecutionRule();
		}
		return executionRule;
	}

	public void setExecutionRule(DefaultExecutionRule rule){
		executionRule = rule;
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

	protected boolean executeToStream(OutputStream output, int timeout, boolean expectPositiveReturnValue)
			throws HgException {

		hgRoot = setupHgRoot();

		List<String> cmd = getCommands();

		String jobName = obfuscateLoginData(getCommandInvoked(cmd));

		ProcessBuilder builder = setupProcess(cmd);

		// I see sometimes that hg has errors if it runs in parallel
		// using a job with exclusive rule here serializes all hg access from plugin.
		processWrapper = createProcessWrapper(output, jobName, builder);

		logConsoleCommandInvoked(jobName);

		// will start hg command as soon as job manager allows us to do it
		processWrapper.schedule();

		try {
			waitForConsumer(timeout);
		} catch (InterruptedException e) {
			processWrapper.cancel();
			throw new HgException("Process cancelled: " + jobName, e);
		}

		if (processWrapper.isAlive()) {
			// command timeout
			processWrapper.cancel();
			throw new HgException("Process timeout: " + jobName);
		}

		IStatus result = processWrapper.getResult();
		if (processWrapper.process == null) {
			// process is either not started or we failed to create it
			if(result == null){
				// was not started at all => timeout?
				throw new HgException("Process timeout: " + jobName);
			}
			if(processWrapper.error == null && result == Status.CANCEL_STATUS) {
				throw new HgException(HgException.OPERATION_CANCELLED,
						"Process cancelled: " + jobName, null);
			}
			throw new HgException("Process start failed: " + jobName, processWrapper.error);
		}

		final String msg = getMessage(output);
		final int exitCode = processWrapper.exitCode;
		long timeInMillis = debugExecTime? System.currentTimeMillis() - processWrapper.startTime : 0;
		// everything fine
		if (exitCode != 0 && expectPositiveReturnValue) {
			Throwable rootCause = result != null ? result.getException() : null;
			final HgException hgex = new HgException(exitCode,
					msg + ". Command line: " + jobName, rootCause);
			logConsoleCompleted(timeInMillis, msg, exitCode, hgex);
			throw hgex;
		}
		if (debugExecTime || debugMode) {
			logConsoleCompleted(timeInMillis, msg, exitCode, null);
		}
		return true;
	}

	protected ProzessWrapper createProcessWrapper(OutputStream output, String jobName, ProcessBuilder builder) {
		return new ProzessWrapper(jobName, builder, output);
	}

	private ProcessBuilder setupProcess(List<String> cmd) {
		ProcessBuilder builder = new ProcessBuilder(cmd);

		// set locale to english have deterministic output
		Map<String, String> env = builder.environment();
		env.put("LANG", "C"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("LANGUAGE", "C"); //$NON-NLS-1$ //$NON-NLS-2$

		// HGPLAIN normalizes output in Mercurial 1.5+
		env.put("HGPLAIN","set by MercurialEclipse"); //$NON-NLS-1$ //$NON-NLS-2$
		Charset charset = setupEncoding(cmd);
		if (charset != null) {
			env.put("HGENCODING", charset.name()); //$NON-NLS-1$
		}

		builder.redirectErrorStream(true); // makes my life easier
		if (workingDir != null) {
			builder.directory(workingDir);
		}
		return builder;
	}

	private Charset setupEncoding(List<String> cmd) {
		if(hgRoot == null){
			return null;
		}
		Charset charset = hgRoot.getEncoding();
		// Enforce strict command line encoding
		cmd.add(1, charset.name());
		cmd.add(1, "--encoding");
		// Enforce fallback encoding for UI (command output)
		// Note: base encoding is UTF-8 for mercurial, fallback is only take into account
		// if actual platfrom don't support it.
		cmd.add(1, "ui.fallbackencoding=" + hgRoot.getFallbackencoding().name()); //$NON-NLS-1$
		cmd.add(1, "--config"); //$NON-NLS-1$
		return charset;
	}

	private HgRoot setupHgRoot() {
		if (workingDir == null) {
			return null;
		}
		try {
			return HgClients.getHgRoot(workingDir);
		} catch (HgCoreException e) {
			// no hg root found
			return null;
		}
	}

	private void waitForConsumer(int timeout) throws InterruptedException {
		if (timeout <= 0) {
			timeout = 1;
		}
		long start = System.currentTimeMillis();
		long now = 0;
		while (processWrapper.isAlive()) {
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

	private void logConsoleCompleted(final long timeInMillis, final String msg, final int exitCode, final HgException hgex) {
		if (showOnConsole) {
			getConsole().commandCompleted(exitCode, timeInMillis, msg, hgex);
		}
	}

	private String getMessage(OutputStream output) {
		String msg = null;
		if (output instanceof FileOutputStream) {
			return null;
		} else if (output instanceof ByteArrayOutputStream) {
			ByteArrayOutputStream baos = (ByteArrayOutputStream) output;
			try {
				String encoding = getEncoding();
				msg = baos.toString(encoding);
			} catch (UnsupportedEncodingException e) {
				logError(e);
				msg = baos.toString();
			}
			if(msg != null){
				msg = msg.trim();
			}
		}
		return msg;
	}

	/**
	 * @return
	 */
	private String getEncoding() {
		String encoding = null;
		if (hgRoot != null) {
			encoding = hgRoot.getEncoding().name();
		} else {
			encoding = getDefaultEncoding();
		}
		return encoding;
	}

	public String executeToString() throws HgException {
		byte[] bytes = executeToBytes();
		if (bytes != null && bytes.length > 0) {
			try {
				return new String(bytes, getEncoding());
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
		if (processWrapper != null) {
			processWrapper.cancel();
		}
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
		if (processWrapper != null) {
			builder.append("processWrapper=");
			builder.append(processWrapper);
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
