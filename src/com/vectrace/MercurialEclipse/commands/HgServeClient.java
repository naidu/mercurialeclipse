/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      bastian	implementation
 *      Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeWorkspaceJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author bastian
 *
 */
public class HgServeClient {

    static class HgServeJob extends SafeWorkspaceJob {
        private final HgRoot hgRoot;
        private final boolean ipv6;
        private final String name;
        private final String prefix;
        private final int port;
        private final String webdirConf;
        private final boolean stdio;
        private final IProgressMonitor progress;

        public HgServeJob(HgRoot hgRoot, int port, String prefix,
                String name, String webdirConf, boolean ipv6, boolean stdio) {
            super(Messages.getString("HgServeClient.serveJob.name") //$NON-NLS-1$
                    + hgRoot.getName() + "..."); //$NON-NLS-1$
            this.hgRoot = hgRoot;
            this.port = port;
            this.prefix = prefix;
            this.name = name;
            this.webdirConf = webdirConf;
            this.ipv6 = ipv6;
            this.stdio = stdio;
            progress = getJobManager().createProgressGroup();
            progress.beginTask("Local Mercurial Server", 1);
            setProgressGroup(progress, IProgressMonitor.UNKNOWN);
        }

        @Override
        protected IStatus runSafe(IProgressMonitor monitor) {
            final AbstractShellCommand command;
            try {
                command = HgServeClient.getCommand(
                        hgRoot, port, prefix, name, webdirConf, stdio, ipv6);
            } catch (IOException e) {
                MercurialEclipsePlugin.logError(e);
                return Status.CANCEL_STATUS;
            }

            SafeWorkspaceJob job = new SafeWorkspaceJob(
                    Messages.getString("HgServeClient.serverThread.name")
                    + "" + command.getCommands().toString().replace(",", " ")) { //$NON-NLS-1$ //$NON-NLS-2$
                @Override
                protected IStatus runSafe(IProgressMonitor m) {
                    try {
                        command.executeToBytes(Integer.MAX_VALUE, false);
                    } catch (HgException e) {
                        MercurialEclipsePlugin.logError(e);
                    }
                    return super.runSafe(m);
                }
            };
            job.setProgressGroup(progress, IProgressMonitor.UNKNOWN);
            try {
                job.schedule();
                while(!progress.isCanceled() && !monitor.isCanceled()){
                    synchronized (this) {
                        try {
                            wait(2000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            } finally {
                command.terminate();
            }
            return super.runSafe(monitor);
        }
    }

    public void serve(HgRoot hgRoot, int port, String prefixPath,
            String name, String webdirConf, boolean stdio, boolean ipv6) {
        new HgServeJob(hgRoot, port, prefixPath, name, webdirConf, ipv6, stdio)
                .schedule();
    }

    private static AbstractShellCommand getCommand(HgRoot hgRoot, int port,
            String prefixPath, String name, String webdirConf, boolean stdio,
            boolean ipv6) throws IOException {
        final AbstractShellCommand command = new HgCommand("serve", hgRoot, true);
        File pidFile = File.createTempFile("hgserve_" + hgRoot.getName(), //$NON-NLS-1$
                ".pidfile"); //$NON-NLS-1$
        pidFile.deleteOnExit();
        if(port!=8000){
            command.addOptions("--port", String.valueOf(port)); //$NON-NLS-1$
        }
        if (prefixPath != null && prefixPath.length() > 0) {
            command.addOptions("--prefix", prefixPath); //$NON-NLS-1$
        }
        if (name != null && name.length() > 0) {
            command.addOptions("--name", name); //$NON-NLS-1$
        }
        if (webdirConf != null && webdirConf.length() > 0) {
            command.addOptions("--webdir-conf", webdirConf); //$NON-NLS-1$
        }
        if (stdio) {
            command.addOptions("--stdio"); //$NON-NLS-1$
        }
        if (ipv6) {
            command.addOptions("--ipv6"); //$NON-NLS-1$
        }
        // start daemon
        return command;
    }

}
