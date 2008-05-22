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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

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
                // TODO report the error to the caller thread
                MercurialEclipsePlugin.logError(e);
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

    protected static PrintStream console = new PrintStream(MercurialUtilities
            .getMercurialConsole().newOutputStream());    

    public static final int MAX_PARAMS = 120;
    protected String command;
    protected List<String> commands;
    protected boolean escapeFiles;
    protected List<String> options = new ArrayList<String>();
    protected File workingDir;
    final List<String> files = new ArrayList<String>();

    private String timeoutConstant;

    protected AbstractShellCommand() {
    }

    protected AbstractShellCommand(List<String> commands, File workingDir,
            boolean escapeFiles) {
        this.command = null;
        this.escapeFiles = escapeFiles;
        this.workingDir = workingDir;
        this.commands = commands;
    }

    protected void addOptions(String... optionsToAdd) {
        for (String option : optionsToAdd) {
            this.options.add(option);
        }
    }

    protected byte[] executeToBytes() throws HgException {
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

    /**
     * Execute a command.
     * 
     * @param timeout
     *            -1 if no timeout, else the timeout in ms.
     * @return
     * @throws HgException
     */
    protected byte[] executeToBytes(int timeout) throws HgException {
        try {
            long start = System.currentTimeMillis();
            List<String> cmd = getCommands();
            ProcessBuilder builder = new ProcessBuilder(cmd);
            builder.redirectErrorStream(true); // makes my life easier
            if (workingDir != null) {
                builder.directory(workingDir);
            }
            Process process = builder.start();
            InputStreamConsumer consumer = new InputStreamConsumer(process
                    .getInputStream());            
            consumer.start();
            consumer.join(timeout); // 30 seconds timeout
            if (!consumer.isAlive()) {
                if (process.waitFor() == 0) {
                    console.println("Done in "
                            + (System.currentTimeMillis() - start) + " ms");
                    return consumer.getBytes();
                }
                String msg = new String(consumer.getBytes());
                System.out.println(msg);
                throw new HgException("Process error, return code: "
                        + process.exitValue() + ", message: " + msg);
            }
            process.destroy();
            throw new HgException("Process timeout");
        } catch (IOException e) {
            throw new HgException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new HgException(e.getMessage(), e);
        }
    }

    protected String executeToString() throws HgException {
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
        console.println("Command: (" + result.size() + ") " + result);
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
}
