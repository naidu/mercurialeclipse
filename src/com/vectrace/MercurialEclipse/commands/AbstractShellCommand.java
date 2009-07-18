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
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgCoreException;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
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
    private boolean showOnConsole = true;

    // private HgConsole console;

    protected AbstractShellCommand() {
        // this.console = MercurialUtilities.getMercurialConsole();
    }

    public AbstractShellCommand(List<String> commands, File workingDir, boolean escapeFiles) {
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
    public byte[] executeToBytes(int timeout, boolean expectPositiveReturnValue) throws HgException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (executeToStream(bos, timeout, expectPositiveReturnValue)) {
            return bos.toByteArray();
        }
        return null;
    }

    public boolean executeToStream(OutputStream output, int timeout, boolean expectPositiveReturnValue)
            throws HgException {
        try {
            List<String> cmd = getCommands();
            // don't output fallbackencoding
            String cmdString = cmd.toString().replace(",", "").substring(1); //$NON-NLS-1$ //$NON-NLS-2$
            final String commandInvoked = cmdString.substring(0, cmdString.length() - 1);
            
            Charset charset = null;
            if (workingDir != null) {
                HgRoot hgRoot;
                try {
                    hgRoot = HgClients.getHgRoot(workingDir);
                    charset = hgRoot.getEncoding();
                    cmd.add(2, "--config"); //$NON-NLS-1$
                    cmd.add(3, "ui.fallbackencoding=" + hgRoot.getFallbackencoding().name()); //$NON-NLS-1$
                } catch (HgCoreException e) {
                    // no hg root found
                }
            }

            ProcessBuilder builder = new ProcessBuilder(cmd);

            // set locale to english have deterministic output
            Map<String, String> env = builder.environment();
            env.put("LANG", "en"); //$NON-NLS-1$ //$NON-NLS-2$
            env.put("LANGUAGE", "en"); //$NON-NLS-1$ //$NON-NLS-2$
            if (charset != null) {
                env.put("HGENCODING", charset.name()); //$NON-NLS-1$
            }
            
            builder.redirectErrorStream(true); // makes my life easier
            if (workingDir != null) {
                builder.directory(workingDir);
            }
            process = builder.start();
            consumer = new InputStreamConsumer(process.getInputStream(), output);
            consumer.start();

            logConsoleCommandInvoked(commandInvoked);
            consumer.join(timeout); // 30 seconds timeout
            final String msg = getMessage(output);
            if (!consumer.isAlive()) {
                final int exitCode = process.waitFor();
                // everything fine
                if (exitCode == 0 || !expectPositiveReturnValue) {
                    logConsoleCompleted(msg, exitCode, null);
                    if (isDebugMode()) {
                        logConsoleMessage(msg, null);
                    }
                    return true;
                }

                // exit code > 0
                final HgException hgex = new HgException("Process error, return code: " + exitCode //$NON-NLS-1$
                        + ", message: " + getMessage(output)); //$NON-NLS-1$

                // exit code == 1 usually isn't fatal.
                logConsoleCompleted(msg, exitCode, hgex);
                throw hgex;
            }
            // command timeout
            final HgException hgEx = new HgException("Process timeout"); //$NON-NLS-1$
            logConsoleError(msg, hgEx);
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

    /**
     * @param commandInvoked
     */
    protected void logConsoleCommandInvoked(final String commandInvoked) {
        if (showOnConsole) {
            new SafeWorkspaceJob("Writing to console") {
                @Override
                public IStatus runSafe(IProgressMonitor monitor) {
                    monitor.beginTask("Writinng to console", 2);
                    monitor.worked(1);
                    getConsole().commandInvoked(commandInvoked);
                    monitor.worked(1);
                    monitor.done();
                    return super.runSafe(monitor);
                }
            }.schedule();
        }
    }

    /**
     * @param msg
     */
    protected void logConsoleMessage(final String msg, final Throwable t) {
        if (showOnConsole) {
            new SafeWorkspaceJob("Writing to console") {
                @Override
                public IStatus runSafe(IProgressMonitor monitor) {
                    monitor.beginTask("Writinng to console", 2);
                    monitor.worked(1);
                    getConsole().printMessage(msg, t);
                    monitor.worked(1);
                    monitor.done();
                    return super.runSafe(monitor);
                }
            }.schedule();
        }
    }

    /**
     * @param msg
     * @param hgEx
     */
    protected void logConsoleError(final String msg, final HgException hgEx) {
        if (showOnConsole) {
            new SafeWorkspaceJob("Writing to console...") {
                @Override
                public IStatus runSafe(IProgressMonitor monitor) {
                    monitor.beginTask("Writinng to console", 2);
                    monitor.worked(1);
                    if (msg != null) {
                        getConsole().printError(msg, hgEx);
                    } else {
                        getConsole().printError(hgEx.getMessage(), hgEx);
                    }
                    monitor.worked(1);
                    monitor.done();
                    return super.runSafe(monitor);
                }
            }.schedule();
        }
    }

    /**
     * @param msg
     * @param exitCode
     * @param hgex
     */
    private void logConsoleCompleted(final String msg, final int exitCode, final HgException hgex) {
        if (showOnConsole) {
            new SafeWorkspaceJob("Writing to console...") {
                @Override
                public IStatus runSafe(IProgressMonitor monitor) {
                    monitor.beginTask("Writinng to console", 2);
                    monitor.worked(1);
                    getConsole().commandCompleted(exitCode, msg, hgex);
                    monitor.worked(1);
                    monitor.done();
                    return super.runSafe(monitor);
                }
            }.schedule();
        }
    }

    private static String getMessage(OutputStream output) {
        String msg = null;
        if (output instanceof FileOutputStream) {
            return null;
        } else if (output instanceof ByteArrayOutputStream) {
            ByteArrayOutputStream baos = (ByteArrayOutputStream) output;
            try {
                msg = baos.toString("UTF-8");
            } catch (UnsupportedEncodingException e) {
                MercurialEclipsePlugin.logError(e);
                msg = baos.toString();
            }
        }

        return msg;
    }

    /**
     * @return
     */
    private boolean isDebugMode() {
        return Boolean
                .valueOf(HgClients.getPreference(MercurialPreferenceConstants.PREF_CONSOLE_DEBUG, "false")).booleanValue(); //$NON-NLS-1$
    }

    public String executeToString() throws HgException {
        byte[] bytes = executeToBytes();
        if (bytes != null) {
            try {
                return new String(bytes, "UTF-8");
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

    /**
     * @param b
     */
    public void setShowOnConsole(boolean b) {
        this.showOnConsole = b;
    }
}
