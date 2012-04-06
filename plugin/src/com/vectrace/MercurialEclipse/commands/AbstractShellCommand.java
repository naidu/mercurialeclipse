/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation (with lots of stuff pulled up from HgCommand)
 *     Andrei Loskutov           - bug fixes
 *     Adam Berkes (Intland)     - bug fixes/restructure
 *     Zsolt Koppany (Intland)   - enhancements
 *     Philip Graf               - use default timeout from preferences
 *     John Peberdy              - refactoring
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import static com.vectrace.MercurialEclipse.MercurialEclipsePlugin.*;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public abstract class AbstractShellCommand extends AbstractClient {

	private static final int BUFFER_SIZE = 32768;

	/**
	 * File encoding to use. If not specified falls back to {@link HgRoot}'s encoding.
	 */
	private String encoding;

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
	class ProzessWrapper extends CommandJob {

		private final OutputStream output;
		private final ProcessBuilder builder;
		private final DefaultExecutionRule execRule;
		private Process process;

		private final String debugName;
		private final boolean expectZeroReturnValue;

		public ProzessWrapper(String name, String debugName, ProcessBuilder builder, OutputStream output, boolean expectZeroReturnValue) {
			super(name, isInitialCommand);

			this.debugName = debugName;
			execRule = getExecutionRule();
			setRule(execRule);
			this.builder = builder;
			this.output = output;
			this.expectZeroReturnValue = expectZeroReturnValue;
		}

		/**
		 * @throws InterruptedException
		 * @see com.vectrace.MercurialEclipse.commands.CommandJob#doRun(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus doRun(IProgressMonitor monitor) throws Exception {
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

				if(process != null) {
					process.destroy();
				}
			}
			return monitor.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
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

		/**
		 * @see com.vectrace.MercurialEclipse.commands.CommandJob#getDebugName()
		 */
		@Override
		protected String getDebugName() {
			return debugName;
		}

		@Override
		protected String getMessage() {
			String msg = null;
			if (output instanceof FileOutputStream) {
				return null;
			} else if (output instanceof ByteArrayOutputStream) {
				ByteArrayOutputStream baos = (ByteArrayOutputStream) output;
				try {
					msg = baos.toString(getEncoding());
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
		 * @see com.vectrace.MercurialEclipse.commands.CommandJob#checkError()
		 */
		@Override
		protected void checkError() throws HgException {
			if (exitCode != 0 && expectZeroReturnValue) {
				IStatus result = getResult();
				Throwable rootCause = result != null ? result.getException() : null;

				throw new HgException(exitCode, getMessage(), getDebugName(), rootCause);
			}
		}
	}

	private boolean isInitialCommand;

	protected String command;

	/**
	 * Calculated commands. See {@link #getCommands()}
	 */
	private List<String> commands;

	/**
	 * Whether files should be preceded by "--" on the command line.
	 * @see #files
	 */
	private final boolean escapeFiles;

	protected List<String> options;

	/**
	 * The working directory. May be null for default working directory.
	 */
	protected final File workingDir;

	protected final List<String> files;
	private String timeoutConstant;
	private ProzessWrapper processWrapper;

	/**
	 * Though this command might not invoke hg, it might get encoding information from it. May be
	 * null.
	 */
	protected final HgRoot hgRoot;

	private DefaultExecutionRule executionRule;

	/**
	 * Human readable name for this operation
	 */
	private final String uiName;

	/**
	 * @param uiName
	 *            Human readable name for this command
	 * @param hgRoot
	 *            Though this command might not invoke hg, it might get encoding information from
	 *            it. May be null.
	 */
	protected AbstractShellCommand(String uiName, HgRoot hgRoot, File workingDir, boolean escapeFiles) {
		super();
		this.hgRoot = hgRoot;
		this.workingDir = workingDir;
		this.escapeFiles = escapeFiles;
		this.uiName = uiName;
		options = new ArrayList<String>();
		files = new ArrayList<String>();

		timeoutConstant = MercurialPreferenceConstants.DEFAULT_TIMEOUT;

		Assert.isNotNull(uiName);
	}

	protected AbstractShellCommand(String uiName, HgRoot hgRoot, List<String> commands, File workingDir, boolean escapeFiles) {
		this(uiName, hgRoot, workingDir, escapeFiles);

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
		return executeToBytes(getTimeOut());
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
	public byte[] executeToBytes(int timeout, boolean expectZeroReturnValue) throws HgException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(BUFFER_SIZE);
		if (executeToStream(bos, timeout, expectZeroReturnValue)) {
			return bos.toByteArray();
		}
		return null;
	}

	protected final boolean executeToStream(OutputStream output, int timeout, boolean expectZeroReturnValue)
			throws HgException {

		List<String> cmd = getCommands();
		String jobName = obfuscateLoginData(getCommandInvoked(cmd));
		File tmpFile = setupTmpFile(cmd);
		ProcessBuilder builder = setupProcess(cmd);

		try {

			// I see sometimes that hg has errors if it runs in parallel
			// using a job with exclusive rule here serializes all hg access from plugin.
			processWrapper = createProcessWrapper(output, uiName, jobName, builder, expectZeroReturnValue);

			CommandJob.execute(processWrapper, timeout);

			return true;
		} finally {
			if (tmpFile != null && tmpFile.isFile()) {
				tmpFile.delete();
			}
		}
	}

	protected ProzessWrapper createProcessWrapper(OutputStream output, String jobName,
			String debugName, ProcessBuilder builder, boolean expectZeroReturnValue) {
		return new ProzessWrapper(jobName, debugName, builder, output, expectZeroReturnValue);
	}

	/**
	 * @param cmd hg commands to run
	 * @return temporary file path used to write {@link HgFeatures#LISTFILE} arguments
	 * @deprecated
	 */
	@Deprecated
	private static File setupTmpFile(List<String> cmd) {
		for (String line : cmd) {
			if (line.startsWith(HgFeatures.LISTFILE.getHgCmd())) {
				return new File(line.substring(HgFeatures.LISTFILE.getHgCmd().length()));
			}
		}
		return null;
	}

	private ProcessBuilder setupProcess(List<String> cmd) {
		ProcessBuilder builder = new ProcessBuilder(cmd);

		// set locale to english have deterministic output
		Map<String, String> env = builder.environment();
		// From wiki: http://mercurial.selenic.com/wiki/UpgradeNotes
		// "If your tools look for particular English messages in Mercurial output,
		// they should disable translations with LC_ALL=C"
		env.put("LC_ALL", "C"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("LANG", "C"); //$NON-NLS-1$ //$NON-NLS-2$
		env.put("LANGUAGE", "C"); //$NON-NLS-1$ //$NON-NLS-2$

		// HGPLAIN normalizes output in Mercurial 1.5+
		env.put("HGPLAIN", "set by MercurialEclipse"); //$NON-NLS-1$ //$NON-NLS-2$
		String charset = setupEncoding(cmd);
		if (charset != null) {
			env.put("HGENCODING", charset); //$NON-NLS-1$
		}

		env.put("HGE_RUNDIR", getRunDir());

		// removing to allow using eclipse merge editor
		builder.environment().remove("HGMERGE");

		builder.redirectErrorStream(true); // makes my life easier
		if (workingDir != null) {
			builder.directory(workingDir);
		}
		return builder;
	}

	/**
	 * Template method to add encoding options
	 * @param cmd The list of commands to add to
	 * @return The encoding for HGENCODING env var, or null
	 */
	@SuppressWarnings("static-method")
	protected String setupEncoding(List<String> cmd) {
		return null;
	}

	/**
	 * Sets the command output charset if the charset is available in the VM.
	 */
	public void setEncoding(String charset) {
		encoding = charset;
	}
	/**
	 * @return never returns null
	 */
	private String getEncoding() {
		if(encoding == null){
			if (hgRoot != null) {
				encoding = hgRoot.getEncoding();
			} else {
				encoding = getDefaultEncoding();
			}
		}
		return encoding;
	}

	public String executeToString() throws HgException {
		return executeToString(true);
	}

	public String executeToString(boolean expectZeroReturnValue) throws HgException {
		byte[] bytes = executeToBytes(getTimeOut(), expectZeroReturnValue);
		if (bytes != null && bytes.length > 0) {
			try {
				return new String(bytes, getEncoding());
			} catch (UnsupportedEncodingException e) {
				throw new HgException(e.getLocalizedMessage(), e);
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Executes a command and writes its output to a file.
	 *
	 * @param file
	 *            The file to which the output is written.
	 * @param expectZeroReturnValue
	 *            If set to {@code true}, an {@code HgException} will be thrown if the command's
	 *            exit code is not zero.
	 * @return Returns {@code true} iff the command was executed successfully.
	 * @throws HgException
	 *             Thrown when the command could not be executed successfully.
	 */
	public boolean executeToFile(File file, boolean expectZeroReturnValue) throws HgException {
		int timeout = HgClients.getTimeOut(MercurialPreferenceConstants.DEFAULT_TIMEOUT);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, false);
			return executeToStream(fos, timeout, expectZeroReturnValue);
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

	private List<String> getCommands() {
		if (commands != null) {
			return commands;
		}
		List<String> result = new ArrayList<String>();
		result.add(getExecutable());
		result.add(command);
		result.addAll(options);

		String listFilesFile = null;
		if (HgFeatures.LISTFILE.isEnabled() && getCommandLineLength(files) > 8000) {
			listFilesFile = createListFilesFile(files);
		}
		if(listFilesFile != null){
			result.add(listFilesFile);
		} else {
			if (escapeFiles && !files.isEmpty()) {
				result.add("--"); //$NON-NLS-1$
			}
			result.addAll(files);
		}
		customizeCommands(result);

		return commands = result;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	private static String createListFilesFile(List<String> paths) {
		BufferedWriter bw = null;
		try {
			File listFile = File.createTempFile("listfile_", "txt");
			bw = new BufferedWriter(new FileWriter(listFile));
			for (String file : paths) {
				bw.write(file);
				bw.newLine();
			}
			bw.flush();
			return HgFeatures.LISTFILE.getHgCmd() + listFile.getAbsolutePath();
		} catch (IOException ioe) {
			MercurialEclipsePlugin.logError(ioe);
			return null;
		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}
	}

	private static int getCommandLineLength(List<String> cmds) {
		int length = 0;
		for (String f : cmds) {
			length += f.getBytes().length;
		}
		return length;
	}


	/**
	 * Can be used after execution to get a list of paths needed to be updated
	 * @return a copy of file paths affected by this command, if any. Never returns null,
	 * but may return empty list. The elements of the set are absolute file paths.
	 */
	public Set<String> getAffectedFiles(){
		Set<String> fileSet = new HashSet<String>();
		fileSet.addAll(files);
		return fileSet;
	}

	/**
	 * Template method to customize the commands to execute
	 * @param cmd The list of commands to execute.
	 */
	protected void customizeCommands(List<String> cmd) {
	}

	protected abstract String getExecutable();

	private String getRunDir()
	{
		String sDir = getExecutable();
		int i;

		if (sDir != null)
		{
			i = Math.max(sDir.lastIndexOf('\\'), sDir.lastIndexOf('/'));

			if (i >= 0)
			{
				return sDir.substring(0, i);
			}
		}

		return "";
	}

	/**
	 * Add a file. Need not be canonical, but will try transform to canonical.
	 *
	 * @param myfiles The files to add
	 */
	public void addFiles(Collection<File> myfiles) {
		for (File file : myfiles) {
			addFile(file);
		}
	}

	/**
	 * Add a file. Looks like we must keep the file path (do not resolve),
	 * see issue #20854.
	 * Mercurial can deal with links by itself, but only if the link target is
	 * under the hg root. resolving the link can move the path outside the hg root
	 * and so hg will deny to collaborate :-)
	 *
	 * @param file The file to add
	 */
	public void addFile(File file) {
		files.add(file.getAbsolutePath());
	}

	public void addFiles(IResource... resources) {
		for (IResource resource : resources) {
			addResource(resource);
		}
	}

	public void addFiles(List<? extends IResource> resources) {
		for (IResource resource : resources) {
			addResource(resource);
		}
	}

	/**
	 * Add all resources that are of type IResource.FILE
	 */
	public void addFilesWithoutFolders(List<? extends IResource> resources) {
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE) {
				addResource(resource);
			}
		}
	}

	private void addResource(IResource resource) {
		// TODO This can be done faster without any file system calls by saving uncanonicalized hg
		// root locations (?).
		// files.add(resource.getLocation().toOSString());
		addFile(ResourceUtils.getFileHandle(resource));
	}

	public void setUsePreferenceTimeout(String cloneTimeout) {
		this.timeoutConstant = cloneTimeout;
	}

	public void terminate() {
		if (processWrapper != null) {
			processWrapper.cancel();
		}
	}

	private String getCommandInvoked(List<String> cmd) {
		if(cmd.isEmpty()){
			// paranoia
			return "<empty command>";
		}
		StringBuilder sb = new StringBuilder();
		if(workingDir != null){
			sb.append(workingDir);
			sb.append(':');
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
		if (timeoutConstant != null) {
			builder.append("timeoutConstant=");
			builder.append(timeoutConstant);
		}
		builder.append("]");
		return builder.toString();
	}

	private int getTimeOut() {
		int timeout;
		if (timeoutConstant == null) {
			timeoutConstant = MercurialPreferenceConstants.DEFAULT_TIMEOUT;
		}
		timeout = HgClients.getTimeOut(timeoutConstant);
		return timeout;
	}

	public void setInitialCommand(boolean initialCommand) {
		this.isInitialCommand = initialCommand;
	}
}
