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

import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * A job that runs a command, mercurial or otherwise.
 */
public abstract class CommandJob extends Job {

	private enum State {
		PENDING, RUN, END
	}

	/**
	 * should not be used by any code except initialization of hg
	 *
	 * @see MercurialEclipsePlugin#checkHgInstallation()
	 */
	private static final CountDownLatch startSignal = new CountDownLatch(1);

	// attributes

	/**
	 * Should not be used by any command except commands needed for the initialization of hg
	 * (debuginstall and version)
	 *
	 * @see MercurialEclipsePlugin#checkHgInstallation()
	 */
	private final boolean isInitialCommand;

	protected long startTime;

	protected volatile State state = State.PENDING;

	protected final boolean debugExecTime;

	protected Throwable error;

	private final boolean isDebugging;

	private final boolean debugMode;

	protected int exitCode = -1;

	// constructors

	public CommandJob(String uiName, boolean isInitialCommand) {
		super(uiName);

		this.isInitialCommand = isInitialCommand && startSignal.getCount() > 0;
		this.debugExecTime = Boolean.valueOf(
				HgClients.getPreference(PREF_CONSOLE_DEBUG_TIME, "false")).booleanValue(); //$NON-NLS-1$

		isDebugging = Boolean.valueOf(
				Platform.getDebugOption(MercurialEclipsePlugin.ID + "/debug/commands"))
				.booleanValue();
		debugMode = Boolean
				.valueOf(HgClients.getPreference(PREF_CONSOLE_DEBUG, "false")).booleanValue(); //$NON-NLS-1$
	}

	// operations

	/**
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected final IStatus run(IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		if (debugExecTime) {
			startTime = System.currentTimeMillis();
		} else {
			startTime = 0;
		}
		state = State.RUN;
		waitForHgInitDone();

		try {
			return doRun(monitor);
		} catch (Throwable t) {
			if (!monitor.isCanceled()) {
				error = t;
			}
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
			state = State.END;
		}
	}

	protected abstract IStatus doRun(IProgressMonitor monitor) throws Exception;

	/**
	 * Waits until the gate is open (hg installation is checked etc)
	 */
	private void waitForHgInitDone() {
		if (!isInitialCommand) {
			try {
				startSignal.await();
			} catch (InterruptedException e1) {
				MercurialEclipsePlugin.logError(e1);
			}
		}
	}

	protected boolean isAlive() {
		// job is either not started yet (is scheduled and waiting), or it is not finished nor
		// cancelled yet
		return (state != State.PENDING && getResult() == null) || (state == State.RUN);
	}

	protected abstract String getDebugName();

	/**
	 * Should not be called by any code except for hg initialization job Opens the command execution
	 * gate after hg installation is checked etc
	 *
	 * @see MercurialEclipsePlugin#checkHgInstallation()
	 */
	public static void hgInitDone() {
		startSignal.countDown();
	}

	private void logConsoleCommandInvoked(final String commandInvoked) {
		if (isDebugging) {
			System.out.println(commandInvoked);
		}

		getConsole().commandInvoked(commandInvoked);
	}

	private void logConsoleCompleted(final long timeInMillis, final Throwable hgex) {
		if (debugExecTime || debugMode || hgex != null) {
			if (isDebugging) {
				System.out.println(getMessage());
				if (hgex != null) {
					hgex.printStackTrace();
				}
			}
			getConsole().commandCompleted(exitCode, timeInMillis, getMessage(), hgex);
		}
	}

	protected abstract String getMessage();

	protected abstract void checkError() throws HgException;

	private static IConsole getConsole() {
		return HgClients.getConsole();
	}

	public void execute(int timeout) throws HgException {

		logConsoleCommandInvoked(getDebugName());

		// will start hg command as soon as job manager allows us to do it
		schedule();

		if (timeout <= 0) {
			timeout = 1;
		}

		Throwable exception = null;
		long start = System.currentTimeMillis();
		long elapsed = 0;

		try {
			try {
				while (isAlive()) {
					if (elapsed >= timeout) {
						cancel();
						throw new HgException("Command timeout: " + getDebugName());
					}
					synchronized (this) { // TODO: sync should be outside the loop
						wait(100);
					}
					elapsed = System.currentTimeMillis() - start;
				}
			} catch (InterruptedException e) {
				cancel();
				throw new HgException("Command cancelled: " + getDebugName(), e);
			}

			IStatus result = getResult();

			if (state == State.PENDING) {
				// process is either not started or we failed to create it
				if (result == null) {
					// was not started at all => timeout?
					assert false;
					throw new HgException("Command timeout: " + getDebugName());
				}
				if (error == null && result == Status.CANCEL_STATUS) {
					throw new HgException(HgException.OPERATION_CANCELLED, "Command cancelled: "
							+ getDebugName(), null);
				}
				throw new HgException("Command start failed: " + getDebugName(), error);
			}

			checkError();

			if (error != null) {
				throw error;
			}

		} catch (Throwable e) {
			exception = e;

			if (!(e instanceof HgException)) {
				e = new HgException(e.getLocalizedMessage(), e);
			}

			throw ((HgException)e);
		} finally {
			long timeInMillis = debugExecTime ? System.currentTimeMillis() - startTime : 0;

			logConsoleCompleted(timeInMillis, exception);
		}

	}
}
