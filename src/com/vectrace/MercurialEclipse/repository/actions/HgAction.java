/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation for repository view.
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * HgAction is the common superclass for all Hg actions. It provides
 * facilities for enablement handling, standard error handling, selection
 * retrieval and prompting.
 */
abstract public class HgAction extends ReplaceableIconAction {
	
	private List<IStatus> accumulatedStatus = new ArrayList<IStatus>();
	
	/**
	 * Common run method for all Hg actions.
	 */
	@Override
    final public void run(IAction action) {
		try {
			if (!beginExecution(action)) return;
			execute(action);
			endExecution();
		} catch (InvocationTargetException e) {
			// Handle the exception and any accumulated errors
			handle(e);
		} catch (InterruptedException e) {
			// Show any problems that have occured so far
			handle(null);
		}
	}

	/**
	 * This method gets invoked before the <code>HgAction#execute(IAction)</code>
	 * method. It can preform any prechecking and initialization required before 
	 * the action is executed. Sunclasses may override but must invoke this
	 * inherited method to ensure proper initialization of this superclass is performed.
	 * These included prepartion to accumulate IStatus and checking for dirty editors.
	 */
	protected boolean beginExecution(IAction action) {
		accumulatedStatus.clear();
		return true;
	}

	/**
	 * Actions must override to do their work.
	 */
	abstract protected void execute(IAction action) throws InvocationTargetException, InterruptedException;

	/**
	 * This method gets invoked after <code>HgAction#execute(IAction)</code>
	 * if no exception occured. Sunclasses may override but should invoke this
	 * inherited method to ensure proper handling oy any accumulated IStatus.
	 */
	protected void endExecution() {
		if ( ! accumulatedStatus.isEmpty()) {
			handle(null);
		}
	}
	
	/**
	 * Add a status to the list of accumulated status. 
	 * These will be provided to method handle(Exception, IStatus[])
	 * when the action completes.
	 */
	protected void addStatus(IStatus status) {
		accumulatedStatus.add(status);
	}
	
	/**
	 * Return the list of status accumulated so far by the action. This
	 * will include any OK status that were added using addStatus(IStatus)
	 */
	protected IStatus[] getAccumulatedStatus() {
		return accumulatedStatus.toArray(new IStatus[accumulatedStatus.size()]);
	}
	
	/**
	 * Return the title to be displayed on error dialogs.
	 * Sunclasses should override to present a custon message.
	 */
	protected String getErrorTitle() {
		return "Error";
	}
	
	/**
	 * Return the title to be displayed on error dialogs when warnigns occur.
	 * Sunclasses should override to present a custon message.
	 */
	protected String getWarningTitle() {
		return "Warning";
	}

	/**
	 * Return the message to be used for the parent MultiStatus when 
	 * mulitple errors occur during an action.
	 * Sunclasses should override to present a custon message.
	 */
	protected String getMultiStatusMessage() {
		return "Multiple Problems occurred"; 
	}
	
	/**
	 * Return the status to be displayed in an error dialog for the given list
	 * of non-OK status.
	 * 
	 * This method can be overridden bu subclasses. Returning an OK status will 
	 * prevent the error dialog from being shown.
	 */
	protected IStatus getStatusToDisplay(IStatus[] problems) {
		if (problems.length == 1) {
			return problems[0];
		}
		MultiStatus combinedStatus = new MultiStatus(MercurialEclipsePlugin.ID, 0, getMultiStatusMessage(), null); //$NON-NLS-1$
		for (int i = 0; i < problems.length; i++) {
			combinedStatus.merge(problems[i]);
		}
		return combinedStatus;
	}
	
	/**
	 * Method that implements generic handling of an exception. 
	 * 
	 * Thsi method will also use any accumulated status when determining what
	 * information (if any) to show the user.
	 * 
	 * @param exception the exception that occured (or null if none occured)
	 * @param status any status accumulated by the action before the end of 
	 * the action or the exception occured.
	 */
	protected void handle(Exception exception) {
		// Get the non-OK statii
		List<IStatus>problems = new ArrayList<IStatus>();
		IStatus[] status = getAccumulatedStatus();
		if (status != null) {
			for (int i = 0; i < status.length; i++) {
				IStatus iStatus = status[i];
				if ( ! iStatus.isOK() ) {
					problems.add(iStatus);
				}
			}
		}
		// Handle the case where there are no problem statii
		if (problems.size() == 0) {
			if (exception == null) return;
			handle(exception, getErrorTitle(), null);
			return;
		}

		// For now, display both the exception and the problem status
		// Later, we can determine how to display both together
		if (exception != null) {
			handle(exception, getErrorTitle(), null);
		}
		
		String message = null;
		IStatus statusToDisplay = getStatusToDisplay(problems.toArray(new IStatus[problems.size()]));
		if (statusToDisplay.isOK()) return;
		if (statusToDisplay.isMultiStatus() && statusToDisplay.getChildren().length == 1) {
			message = statusToDisplay.getMessage();
			statusToDisplay = statusToDisplay.getChildren()[0];
		}
		String title;
		if (statusToDisplay.getSeverity() == IStatus.ERROR) {
			title = getErrorTitle();
		} else {
			title = getWarningTitle();
		}
		MercurialEclipsePlugin.logError(message+","+title,exception);
	}

	/**
	 * Convenience method for running an operation with the appropriate progress.
	 * Any exceptions are propogated so they can be handled by the
	 * <code>HgAction#run(IAction)</code> error handling code.
	 * 
	 * @param runnable  the runnable which executes the operation
	 * @param cancelable  indicate if a progress monitor should be cancelable
	 * @param progressKind  one of PROGRESS_BUSYCURSOR or PROGRESS_DIALOG
	 */
	final protected void run(final IRunnableWithProgress runnable, boolean cancelable, int progressKind) throws InvocationTargetException, InterruptedException {
		final Exception[] exceptions = new Exception[] {null};
		
		// Ensure that no repository view refresh happens until after the action
		final IRunnableWithProgress innerRunnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                runnable.run(monitor);
			}
		};
		
		switch (progressKind) {
			case PROGRESS_BUSYCURSOR :
				BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
					public void run() {
						try {
							innerRunnable.run(new NullProgressMonitor());
						} catch (InvocationTargetException e) {
							exceptions[0] = e;
						} catch (InterruptedException e) {
							exceptions[0] = e;
						}
					}
				});
				break;
			case PROGRESS_DIALOG :
			default :
				new ProgressMonitorDialog(getShell()).run(true, cancelable,/*cancelable, true, */innerRunnable);	
				break;
		}
		if (exceptions[0] != null) {
			if (exceptions[0] instanceof InvocationTargetException)
				throw (InvocationTargetException)exceptions[0];
            throw (InterruptedException)exceptions[0];
		}
	}
	
	/**
	 * Answers if the action would like dirty editors to saved
	 * based on the Hg preference before running the action. By
	 * default, HgActions do not save dirty editors.
	 */
	protected boolean needsToSaveDirtyEditors() {
		return false;
	}

	/**
	 * Find the object associated with the selected object that is adapted to
	 * the provided class.
	 * 
	 * @param selection
	 * @param c
	 * @return Object
	 */
	@SuppressWarnings("unchecked")
    public static Object getAdapter(Object selection, Class c) {
		if (c.isInstance(selection)) {
			return selection;
		}
		if (selection instanceof IAdaptable) {
			IAdaptable a = (IAdaptable) selection;
			Object adapter = a.getAdapter(c);
			if (c.isInstance(adapter)) {
				return adapter;
			}
		}
		return null;
	}
	
			
	/**
	 * @see org.tigris.subversion.subclipse.ui.actions.TeamAction#handle(java.lang.Exception, java.lang.String, java.lang.String)
	 */
	@Override
    protected void handle(Exception exception, String title, String message) {
		MercurialEclipsePlugin.logError(title+","+message,exception);
	}	
}
