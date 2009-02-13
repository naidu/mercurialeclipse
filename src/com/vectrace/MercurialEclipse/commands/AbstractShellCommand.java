/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation (with lots of stuff pulled up from HgCommand)
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author bastian
 * 
 */
public abstract class AbstractShellCommand {
    /**
     * 
     */
    public static final int DEFAULT_TIMEOUT = 360000;

    private class InputStreamConsumer extends Thread {
        private final InputStream stream;
        private final OutputStream output;

        public InputStreamConsumer(InputStream stream, OutputStream output) {
            this.output = output;
            this.stream = new BufferedInputStream(stream);
        }

        @Override
        public void run() {
            try {
                int length;
                byte[] buffer = new byte[1024];
                while ((length = stream.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
            } catch (IOException e) {
                if (!interrupted()) {
                    HgClients.logError(e);
                }
            } finally {
                try {
                    this.stream.close();
                } catch (IOException e) {
                    HgClients.logError(e);
                }
                try {
                    output.close();
                } catch (IOException e) {
                    HgClients.logError(e);
                }
            }
        }
    }

    public static final int MAX_PARAMS = 120;
    protected String command;
    protected List<String> commands;
    protected boolean escapeFiles;
    protected List<String> options = new ArrayList<String>();
    protected File workingDir;
    final List<String> files = new ArrayList<String>();

    private String timeoutConstant;
    private InputStreamConsumer consumer;
    private Process process;

    // private HgConsole console;

    protected AbstractShellCommand() {
        // this.console = MercurialUtilities.getMercurialConsole();
    }

    public AbstractShellCommand(List<String> commands, File workingDir,
            boolean escapeFiles) {
        this();
        this.command = null;
        this.escapeFiles = escapeFiles;
        this.workingDir = workingDir;
        this.commands = commands;
    }

    public void addOptions(String... optionsToAdd) {
        for (String option : optionsToAdd) {
            this.options.add(option);
        }
    }

    public byte[] executeToBytes() throws HgException {
        int timeout = DEFAULT_TIMEOUT;
        if (this.timeoutConstant != null) {
            timeout = HgClients.getTimeOut(this.timeoutConstant);

        }
        return executeToBytes(timeout);
    }

    public byte[] executeToBytes(int timeout) throws HgException {
        return this.executeToBytes(timeout, true);
    }

    /**
     * Execute a command.
     * 
     * @param timeout
     *            -1 if no timeout, else the timeout in ms.
     * @return
     * @throws HgException
     */
    public byte[] executeToBytes(int timeout, boolean expectPositiveReturnValue)
            throws HgException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (executeToStream(bos, timeout, expectPositiveReturnValue)) {
            return bos.toByteArray();
        }
        return null;
    }

    public boolean executeToStream(OutputStream output, int timeout,
            boolean expectPositiveReturnValue) throws HgException {
        try {
            List<String> cmd = getCommands();
            String cmdString = cmd.toString().replace(",", "").substring(1); //$NON-NLS-1$ //$NON-NLS-2$
            final String commandInvoked = cmdString.substring(0, cmdString
                    .length() - 1);

            ProcessBuilder builder = new ProcessBuilder(cmd);

            // set locale to english have deterministic output
            Map<String, String> env = builder.environment();
            env.put("LC_ALL", "en_US.utf8"); //$NON-NLS-1$ //$NON-NLS-2$
            env.put("LANG", "en_US.utf8"); //$NON-NLS-1$ //$NON-NLS-2$
            env.put("LANGUAGE", "en_US.utf8"); //$NON-NLS-1$ //$NON-NLS-2$
            env.put("LC_MESSAGES", "en_US.utf8"); //$NON-NLS-1$ //$NON-NLS-2$

            builder.redirectErrorStream(true); // makes my life easier
            if (workingDir != null) {
                builder.directory(workingDir);
            }
            process = builder.start();
            consumer = new InputStreamConsumer(process.getInputStream(), output);
            consumer.start();
            new SafeWorkspaceJob("Writing to console...") {
                /*
                 * (non-Javadoc)
                 * 
                 * @see
                 * com.vectrace.MercurialEclipse.SafeWorkspaceJob#runSafe(org
                 * .eclipse.core.runtime.IProgressMonitor)
                 */
                @Override
                protected IStatus runSafe(IProgressMonitor monitor) {
                    getConsole().commandInvoked(commandInvoked);
                    return super.runSafe(monitor);
                }
            }.schedule();

            consumer.join(timeout); // 30 seconds timeout
            final String msg = getMessage(output);
            if (!consumer.isAlive()) {
                final int exitCode = process.waitFor();
                // everything fine
                if (exitCode == 0 || !expectPositiveReturnValue) {
                    new SafeWorkspaceJob("Writing to console...") {
                        @Override
                        public IStatus runSafe(IProgressMonitor monitor) {
                            getConsole().commandCompleted(0, msg, null);
                            if (isDebugMode()) {
                                getConsole().printMessage(msg, null);
                            }
                            return super.runSafe(monitor);
                        }
                    }.schedule();
                    return true;
                }

                // exit code > 0
                final HgException hgex = new HgException(
                        "Process error, return code: " + exitCode //$NON-NLS-1$
                                + ", message: " + getMessage(output)); //$NON-NLS-1$

                // exit code == 1 usually isn't fatal.
                new SafeWorkspaceJob("Writing to console...") {
                    @Override
                    public IStatus runSafe(IProgressMonitor monitor) {
                        getConsole().commandCompleted(exitCode, msg, hgex);
                        return super.runSafe(monitor);
                    }
                }.schedule();

                throw hgex;
            }
            // command timeout
            final HgException hgEx = new HgException("Process timeout"); //$NON-NLS-1$
            new SafeWorkspaceJob("Writing to console...") {
                @Override
                public IStatus runSafe(IProgressMonitor monitor) {
                    if (msg != null) {
                        getConsole().printError(msg, hgEx);
                    } else {
                        getConsole().printError(hgEx.getMessage(), hgEx);
                    }
                    return super.runSafe(monitor);
                }
            }.schedule();
            throw hgEx;
        } catch (IOException e) {
            throw new HgException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new HgException(e.getMessage(), e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String getMessage(OutputStream output) {
        return output instanceof FileOutputStream ? null : output.toString();
    }

    /**
     * @return
     */
    private boolean isDebugMode() {
        return Boolean.valueOf(
                HgClients.getPreference(
                        MercurialPreferenceConstants.PREF_CONSOLE_DEBUG,
                        "false")).booleanValue(); //$NON-NLS-1$
    }

    public String executeToString() throws HgException {
        byte[] bytes = executeToBytes();
        if (bytes != null) {
            return new String(bytes);
        }
        return ""; //$NON-NLS-1$
    }

    public boolean executeToFile(File file, int timeout,
            boolean expectPositiveReturnValue) throws HgException {
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
        addFiles(Arrays.asList(myFiles));
    }

    public void addFiles(Collection<String> myFiles) {
        for (String file : myFiles) {
            this.files.add(file);
        }
    }

    public void addFiles(IResource... resources) {
        for (IResource resource : resources) {
            this.files.add(resource.getLocation().toOSString());
        }
    }

    public void addFiles(List<? extends IResource> resources) {
        for (IResource resource : resources) {
            this.files.add(resource.getLocation().toOSString());
        }
    }

    /**
     * @param cloneTimeout
     */
    public void setUsePreferenceTimeout(String cloneTimeout) {
        this.timeoutConstant = cloneTimeout;
    }

    /**
     * 
     */
    public void terminate() {
        if (consumer != null) {
            consumer.interrupt();
        }
        process.destroy();
    }

    /**
     * @return the console
     */
    private IConsole getConsole() {
        return HgClients.getConsole();
    }
}
