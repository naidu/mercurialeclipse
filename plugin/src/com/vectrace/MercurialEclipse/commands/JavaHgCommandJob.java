/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * john	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.aragost.javahg.internals.AbstractCommand;
import com.vectrace.MercurialEclipse.exception.HgException;

public abstract class JavaHgCommandJob<T> extends CommandJob {

	private final AbstractCommand command;
	private T value;

	public JavaHgCommandJob(com.aragost.javahg.internals.AbstractCommand command, String sUIName) {
		this(command, sUIName, false);
	}

	public JavaHgCommandJob(com.aragost.javahg.internals.AbstractCommand command, String sUIName,
			boolean isInitialCommand) {
		super(sUIName, isInitialCommand);

		this.command = command;
	}

	// operations

	/**
	 * @see com.vectrace.MercurialEclipse.commands.CommandJob#doRun(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected final IStatus doRun(IProgressMonitor monitor) throws Exception {
		value = run();
		return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
	}

	protected abstract T run() throws Exception;

	/**
	 * @see com.vectrace.MercurialEclipse.commands.CommandJob#getDebugName()
	 */
	@Override
	protected String getDebugName() {
		// Future: include arguments, etc
		return command.getRepository().toString() + ":" + command.getCommandName();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.commands.CommandJob#getMessage()
	 */
	@Override
	protected String getMessage() {
		return command.getErrorString();
	}

	/**
	 * @see com.vectrace.MercurialEclipse.commands.CommandJob#checkError()
	 */
	@Override
	protected void checkError() throws HgException {
	}

	/**
	 * @see com.vectrace.MercurialEclipse.commands.CommandJob#execute(int)
	 */
	@Override
	public JavaHgCommandJob<T> execute(int timeout) throws HgException {
		super.execute(timeout);
		return this;
	}

	/**
	 * @return The value returned by {@link #run()}
	 */
	public T getValue() {
		return value;
	}
}
