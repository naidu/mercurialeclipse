/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgBundleClient;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class BundleOperation extends HgOperation {

	private final HgRoot root;
	private final String bundleFile;
	private final ChangeSet rev;
	private final String remoteRepo;

	public BundleOperation(IRunnableContext ctx, HgRoot hgRoot, ChangeSet revision,
			String bundleFileName, String remoteRepo) {
		super(ctx);
		root = hgRoot;
		rev = revision;
		bundleFile = bundleFileName;
		this.remoteRepo = remoteRepo;
	}

	@Override
	protected String getActionDescription() {
		return "Create bundle of repository " + root;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			monitor.beginTask("Starting to create bundle...", 2); //$NON-NLS-1$
			monitor.subTask("Calling Mercurial bundle command...");
			monitor.worked(1);
			this.result = HgBundleClient.bundle(root, rev, bundleFile, remoteRepo);
			monitor.worked(1);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} finally {
			monitor.done();
		}
	}

}