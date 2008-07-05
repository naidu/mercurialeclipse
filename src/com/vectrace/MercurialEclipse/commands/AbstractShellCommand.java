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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.views.console.HgConsole;

/**
 * @author bastian
 * 
 */
public abstract class AbstractShellCommand {
    /**
     * 
     */
    public static final int DEFAULT_TIMEOUT = 120000;

    private class InputStreamConsumer extends Thread {
        private byte[] output;
        private final InputStream stream;

        public InputStreamConsumer(InputStream stream) {
            this.stream = new BufferedInputStream(stream);
        }

        public byte[] getBytes() {
            return output;
        }

        @Override
        public void run() {
            ByteArrayOutputStream myOutput = new ByteArrayOutputStream();
            try {
                int length;
                byte[] buffer = new byte[1024];

                while ((length = stream.read(buffer)) != -1) {
                    myOutput.write(buffer, 0, length);
                }
                this.output = myOutput.toByteArray();
            } catch (IOException e) {
                if (!interrupted()) {
                    MercurialEclipsePlugin.logError(e);
                }
            } finally {
                try {
                    this.stream.close();
                } catch (IOException e) {
                    MercurialEclipsePlugin.logError(e);
                }
                try {
                    myOutput.close();
                } catch (IOException e) {
                    MercurialEclipsePlugin.logError(e);
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
    private HgConsole console;

    protected AbstractShellCommand() {
        this.console = MercurialUtilities.getMercurialConsole();
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
            String pref = MercurialUtilities.getPreference(
                    this.timeoutConstant, String.valueOf(DEFAULT_TIMEOUT));
            try {
                timeout = Integer.parseInt(pref);
                if (timeout < 0) {
                    throw new NumberFormatException("Timeout < 0");
                }
            } catch (NumberFormatException e) {
                MercurialEclipsePlugin.logWarning(
                        "Timeout for command " + command
                                + " not correctly configured in preferences.",
                        e);
            }
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
        try {
            List<String> cmd = getCommands();
            String cmdString = cmd.toString().replace(",", "").substring(1);
            cmdString = cmdString.substring(0, cmdString.length() - 1);
            if (console == null) {
                console = MercurialUtilities.getMercurialConsole();
            }
            console.commandInvoked(cmdString);
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true); // makes my life easier
            if (workingDir != null) {
                builder.directory(workingDir);
            }
            process = builder.start();

            consumer = new InputStreamConsumer(process.getInputStream());
            consumer.start();
            consumer.join(timeout); // 30 seconds timeout
            if (!consumer.isAlive()) {

                if (process.waitFor() == 0) {
                    console.commandCompleted(new Status(IStatus.OK,
                            MercurialEclipsePlugin.ID, new String(consumer
                                    .getBytes())), null);
                    return consumer.getBytes();
                }
                String msg = "";
                if (!expectPositiveReturnValue) {
                    return msg.getBytes();
                }

                HgException hgex = new HgException(
                        "Process error, return code: " + process.exitValue()
                                + ", message: " + msg);

                console.commandCompleted(new Status(IStatus.ERROR,
                        MercurialEclipsePlugin.ID, hgex.getLocalizedMessage()),
                        null);

                throw hgex;
            }
            HgException hgEx = new HgException("Process timeout");
            console.errorLineReceived(hgEx.getMessage(), new Status(
                    IStatus.ERROR, MercurialEclipsePlugin.ID, new String(
                            consumer.getBytes()), hgEx));
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

    public String executeToString() throws HgException {
        return new String(executeToBytes());
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
            result.add("--");
        }
        result.addAll(files);
        // TODO check that length <= MAX_PARAMS
        return result;
    }

    protected abstract String getExecutable();

    protected void addFiles(String... myFiles) {
        for (String file : myFiles) {
            this.files.add(file);
        }
    }

    protected void addFiles(IResource... resources) {
        for (IResource resource : resources) {
            this.files.add(resource.getLocation().toOSString());
        }
    }

    protected void addFiles(List<? extends IResource> resources) {
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
}
