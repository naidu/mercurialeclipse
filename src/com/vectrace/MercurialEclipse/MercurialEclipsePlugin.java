/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Jérôme Nègre              - some fixes
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland)     - default encoding
 *     Philip Graf               - proxy support
 *******************************************************************************/

package com.vectrace.MercurialEclipse;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;

import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgClients;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.commands.HgDebugInstallClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.storage.HgCommitMessageManager;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.views.console.HgConsoleHolder;

/**
 * The main plugin class to be used in the desktop.
 */
public class MercurialEclipsePlugin extends AbstractUIPlugin {

	public static final String ID = "com.vectrace.MercurialEclipse"; //$NON-NLS-1$

	public static final String ID_ChangeLogView = "com.vectrace.MercurialEclipse.views.ChangeLogView"; //$NON-NLS-1$

	public static final String BUNDLE_FILE_PREFIX = "bundlefile"; //$NON-NLS-1$

	// The shared instance.
	private static MercurialEclipsePlugin plugin;

	// the repository manager
	private static HgRepositoryLocationManager repoManager = new HgRepositoryLocationManager();

	// the commit message manager
	private static HgCommitMessageManager commitMessageManager = new HgCommitMessageManager();

	private static final String defaultEncoding = Charset.isSupported(MercurialPreferenceConstants.PREF_DEFAULT_ENCODING) ?
			MercurialPreferenceConstants.PREF_DEFAULT_ENCODING : Charset.defaultCharset().name();

	private boolean hgUsable = true;

	private ServiceTracker proxyServiceTracker;

	private static final Version LOWEST_WORKING_VERSION = new Version(1, 3, 1);

	private static final Pattern VERSION_PATTERN = Pattern.compile(".*version\\s+(\\d(\\.\\d)+)+.*", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	public MercurialEclipsePlugin() {
		// should NOT do anything until started by OSGI
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		DefaultConfiguration cfg = new DefaultConfiguration();
		HgClients.initialize(cfg, cfg, cfg);

		proxyServiceTracker = new ServiceTracker(context, IProxyService.class.getName(), null);
		proxyServiceTracker.open();

		Job job = new Job(Messages.getString("MercurialEclipsePlugin.startingMercurialEclipse")) { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(Messages.getString("MercurialEclipsePlugin.startingMercurialEclipse"), 3); //$NON-NLS-1$
					monitor.subTask(Messages.getString("MercurialEclipsePlugin.checkingMercurialInstallation")); //$NON-NLS-1$
					checkHgInstallation();
					monitor.worked(1);
					// read known repositories
					monitor.subTask(Messages.getString("MercurialEclipsePlugin.loadingKnownMercurialRepositories")); //$NON-NLS-1$
					repoManager.start();
					monitor.worked(1);
					// read in commit messages from disk
					monitor.subTask(Messages.getString("MercurialEclipsePlugin.startingCommitMessageManager")); //$NON-NLS-1$
					commitMessageManager.start();
					monitor.worked(1);
					monitor.done();
					return new Status(IStatus.OK, ID, Messages.getString("MercurialEclipsePlugin.startedSuccessfully")); //$NON-NLS-1$
				} catch (Exception e) {
					hgUsable = false;
					logError(Messages.getString("MercurialEclipsePlugin.unableToStart"), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, ID, e.getLocalizedMessage(), e);
				}
			}
		};

		// always show console
		job.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				super.done(event);
				// open in SWT GUI Thread
				new SafeUiJob(Messages.getString("MercurialEclipsePlugin.openingMercurialConsole")) { //$NON-NLS-1$
					@Override
					protected IStatus runSafe(IProgressMonitor monitor) {
						HgConsoleHolder.getInstance().showConsole(true);
						return super.runSafe(monitor);
					}
				}.schedule();
			}
		});
		job.schedule();

	}

	/**
	 * Checks if Mercurial is configured properly by issuing the hg debuginstall
	 * command.
	 */
	public void checkHgInstallation() {
		try {
			hgUsable = true;
			MercurialUtilities.getHGExecutable(true);
			String result = HgDebugInstallClient.debugInstall();
			if (result.endsWith("No problems detected")) { //$NON-NLS-1$
				checkHgVersion();
				hgUsable = true;
				return;
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
			hgUsable = false;
		}
	}

	private void checkHgVersion() throws HgException {
		AbstractShellCommand command = new HgCommand("version", (File) null, true); //$NON-NLS-1$
		command.setShowOnConsole(false);
		String version = new String(command.executeToBytes(Integer.MAX_VALUE)).trim();
		String[] split = version.split("\\n"); //$NON-NLS-1$
		version = split.length > 0? split[0] : ""; //$NON-NLS-1$
		Matcher matcher = VERSION_PATTERN.matcher(version);
		if(matcher.matches()){
			version = matcher.group(1);
			if(version != null && LOWEST_WORKING_VERSION.compareTo(new Version(version)) <= 0){
				return;
			}
			throw new HgException(Messages.getString("MercurialEclipsePlugin.unsupportedHgVersion") + version + Messages.getString("MercurialEclipsePlugin.expectedAtLeast") //$NON-NLS-1$ //$NON-NLS-2$
					+ LOWEST_WORKING_VERSION + "."); //$NON-NLS-1$
		}
	}

	/**
	 * Gets the repository manager
	 */
	static public HgRepositoryLocationManager getRepoManager() {
		return repoManager;
	}

	public static HgCommitMessageManager getCommitMessageManager() {
		return commitMessageManager;
	}

	public static void setCommitMessageManager(
			HgCommitMessageManager commitMessageManager) {
		MercurialEclipsePlugin.commitMessageManager = commitMessageManager;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			repoManager.stop();
			// save commit messages to disk
			commitMessageManager.stop();
			proxyServiceTracker.close();
		} finally {
			super.stop(context);
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static MercurialEclipsePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path.
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		ImageDescriptor descriptor = getDefault().getImageRegistry().getDescriptor(path);
		if(descriptor == null) {
			descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(ID, "icons/" + path); //$NON-NLS-1$
			getDefault().getImageRegistry().put(path, descriptor);
		}
		return descriptor;
	}

	/**
	 * Returns an image at the given plug-in relative path.
	 *
	 * @param path
	 *            the path
	 * @return the image
	 */
	public static Image getImage(String path) {
		ImageDescriptor descriptor = getDefault().getImageRegistry().getDescriptor(path);
		if(descriptor == null) {
			descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(ID, "icons/" + path); //$NON-NLS-1$
			getDefault().getImageRegistry().put(path, descriptor);
		}
		return getDefault().getImageRegistry().get(path);
	}

	public static final void logError(String message, Throwable error) {
		getDefault().getLog().log(createStatus(message, 0, IStatus.ERROR, error));
	}

	public static void showError(final Throwable error) {
		new ErrorJob(error).schedule(100);
	}


	public static final void logWarning(String message, Throwable error) {
		getDefault().getLog().log(createStatus(message, 0, IStatus.WARNING, error));
	}

	public static final void logInfo(String message, Throwable error) {
		getDefault().getLog().log(createStatus(message, 0, IStatus.INFO, error));
	}

	public static IStatus createStatus(String msg, int code, int severity,
			Throwable ex) {
		return new Status(severity, ID, code, msg, ex);
	}

	public final static void logError(Throwable ex) {
		logError(ex.getMessage(), ex);
	}

	/**
	 * Creates a busy cursor and runs the specified runnable. May be called from
	 * a non-UI thread.
	 *
	 * @param parent
	 *            the parent Shell for the dialog
	 * @param cancelable
	 *            if true, the dialog will support cancelation
	 * @param runnable
	 *            the runnable
	 *
	 * @exception InvocationTargetException
	 *                when an exception is thrown from the runnable
	 * @exception InterruptedException
	 *                when the progress monitor is cancelled
	 */
	public static void runWithProgress(Shell parent, boolean cancelable,
			final IRunnableWithProgress runnable)
	throws InvocationTargetException, InterruptedException {

		boolean createdShell = false;
		Shell myParent = parent;
		try {
			if (myParent == null || myParent.isDisposed()) {
				Display display = Display.getCurrent();
				if (display == null) {
					// cannot provide progress (not in UI thread)
					runnable.run(new NullProgressMonitor());
					return;
				}
				// get the active shell or a suitable top-level shell
				myParent = display.getActiveShell();
				if (myParent == null) {
					myParent = new Shell(display);
					createdShell = true;
				}
			}
			// pop up progress dialog after a short delay
			final Exception[] holder = new Exception[1];
			BusyIndicator.showWhile(myParent.getDisplay(), new Runnable() {
				public void run() {
					try {
						runnable.run(new NullProgressMonitor());
					} catch (InvocationTargetException e) {
						holder[0] = e;
					} catch (InterruptedException e) {
						holder[0] = e;
					}
				}
			});
			if (holder[0] != null) {
				if (holder[0] instanceof InvocationTargetException) {
					throw (InvocationTargetException) holder[0];
				}
				throw (InterruptedException) holder[0];

			}
			// new TimeoutProgressMonitorDialog(parent, TIMEOUT).run(true
			// /*fork*/, cancelable, runnable);
		} finally {
			if (createdShell) {
				parent.dispose();
			}
		}
	}

	/**
	 * Convenience method to get the currently active workbench page. Note that
	 * the active page may not be the one that the usr perceives as active in
	 * some situations so this method of obtaining the activae page should only
	 * be used if no other method is available.
	 *
	 * @return the active workbench page
	 */
	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		return window.getActivePage();
	}

	public boolean isHgUsable() {
		return hgUsable;
	}

	public static Display getStandardDisplay() {
		return PlatformUI.getWorkbench().getDisplay();
	}

	public IProxyService getProxyService() {
		return (IProxyService) proxyServiceTracker.getService();
	}

	/**
	 * @return the defaultencoding
	 */
	public static String getDefaultEncoding() {
		return defaultEncoding;
	}

	public static String getDefaultEncoding(IProject project) {
		if (project != null && getDefaultEncoding().equals(Charset.defaultCharset().name())) {
			try {
				return project.getDefaultCharset();
			} catch (CoreException ex) {
				MercurialEclipsePlugin.logError(ex);
			}

		}
		return getDefaultEncoding();
	}

	/**
	 * Job to show error dialogs. Avoids to show hunderts of dialogs by ussing an exclusive rule.
	 *
	 * @author Andrei
	 */
	private static final class ErrorJob extends SafeUiJob {

		static class ExclusiveRule implements ISchedulingRule {
			public boolean isConflicting(ISchedulingRule rule) {
				return contains(rule);
			}
			public boolean contains(ISchedulingRule rule) {
				return rule instanceof ExclusiveRule;
			}
		}

		final IStatus status;

		private ErrorJob(Throwable error) {
			super(Messages.getString("MercurialEclipsePlugin.showError")); //$NON-NLS-1$
			if(error instanceof CoreException){
				status = ((CoreException) error).getStatus();
			} else {
				status = createStatus(error.getMessage(), 0, IStatus.ERROR, error);
			}
			setRule(new ExclusiveRule());
		}

		@Override
		protected IStatus runSafe(IProgressMonitor monitor) {

			IJobManager jobManager = Job.getJobManager();
			String title;
			IStatus errStatus;
			if (jobManager.find(plugin).length == 1) {
				// it's me alone there
				errStatus = status;
			} else {
				// o-ho, we have multiple errors waiting to be displayed...
				title = Messages.getString("MercurialEclipsePlugin.unexpectedErrors"); //$NON-NLS-1$
				String message = Messages.getString("MercurialEclipsePlugin.unexpectedErrorsOccured"); //$NON-NLS-1$
				// get the latest state
				Job[] jobs = jobManager.find(plugin);
				// discard all waiting now (we are not affected)
				jobManager.cancel(plugin);
				List<IStatus> stati = new ArrayList<IStatus>();
				for (Job job : jobs) {
					if(job instanceof ErrorJob){
						ErrorJob errorJob = (ErrorJob) job;
						stati.add(errorJob.status);
					}
				}
				IStatus[] array = stati.toArray(new IStatus[stati.size()]);
				errStatus = new MultiStatus(title, 0, array, message, null);
			}
			StatusManager.getManager().handle(errStatus, StatusManager.SHOW);
			return super.runSafe(monitor);
		}

		@Override
		public boolean belongsTo(Object family) {
			return plugin == family;
		}
	}

}