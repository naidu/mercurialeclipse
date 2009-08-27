/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.List;
import org.eclipse.core.resources.IContainer;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class HgCommand extends AbstractShellCommand {

    public HgCommand(List<String> commands, File workingDir, boolean escapeFiles) {
        super(commands, workingDir, escapeFiles);
    }

    public HgCommand(String command, File workingDir, boolean escapeFiles) {
        this.command = command;
        this.workingDir = workingDir;
        this.escapeFiles = escapeFiles;
    }

    public HgCommand(String command, IContainer container,
            boolean escapeFiles) {
        this(command, container.getLocation().toFile(), escapeFiles);
    }

    public HgCommand(String command, boolean escapeFiles) {
        this(command, (File) null, escapeFiles);
    }

    protected String getHgExecutable() {
        return HgClients.getExecutable();

    }

    protected String getDefaultUserName() {
        return HgClients.getDefaultUserName();
    }

    protected void addUserName(String user) {
        this.options.add("-u"); //$NON-NLS-1$
        this.options.add(user != null ? user : getDefaultUserName());
    }

    @Override
    protected String getExecutable() {
        return getHgExecutable();
    }
}
