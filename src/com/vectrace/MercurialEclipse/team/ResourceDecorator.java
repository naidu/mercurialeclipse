/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov (Intland) - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *     Zsolt Kopany (Intland)    - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import static com.vectrace.MercurialEclipse.preferences.HgDecoratorConstants.*;
import static com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.IncomingChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.Bits;

/**
 * @author zingo
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class ResourceDecorator extends LabelProvider implements ILightweightLabelDecorator, Observer {
	private static final MercurialStatusCache STATUS_CACHE = MercurialStatusCache.getInstance();
	private static final IncomingChangesetCache INCOMING_CACHE = IncomingChangesetCache.getInstance();
	private static final LocalChangesetCache LOCAL_CACHE = LocalChangesetCache.getInstance();

	private final static String[] fonts = new String[] {
		ADDED_FONT,
		CONFLICT_FONT,
		DELETED_FONT,
		REMOVED_FONT,
		UNKNOWN_FONT,
		IGNORED_FONT, CHANGE_FONT };

	private final static String[] colors = new String[] {
		ADDED_BACKGROUND_COLOR,
		ADDED_FOREGROUND_COLOR,
		CHANGE_BACKGROUND_COLOR,
		CHANGE_FOREGROUND_COLOR,
		CONFLICT_BACKGROUND_COLOR,
		CONFLICT_FOREGROUND_COLOR,
		IGNORED_BACKGROUND_COLOR,
		IGNORED_FOREGROUND_COLOR,
		DELETED_BACKGROUND_COLOR,
		DELETED_FOREGROUND_COLOR,
		REMOVED_BACKGROUND_COLOR,
		REMOVED_FOREGROUND_COLOR,
		UNKNOWN_BACKGROUND_COLOR,
		UNKNOWN_FOREGROUND_COLOR };

	private final static Set<String> interestingPrefs = new HashSet<String>();
	static {
		interestingPrefs.add(LABELDECORATOR_LOGIC_2MM);
		interestingPrefs.add(LABELDECORATOR_LOGIC);
		interestingPrefs.add(PREF_DECORATE_WITH_COLORS);
		interestingPrefs.add(RESOURCE_DECORATOR_SHOW_CHANGESET);
		interestingPrefs.add(RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET);
	}

	/** set to true when having 2 different statuses in a folder flags it has modified */
	private boolean folder_logic_2MM;
	private ITheme theme;
	private boolean colorise;
	private boolean showChangeset;
	private boolean showIncomingChangeset;

	public ResourceDecorator() {
		configureFromPreferences();
		STATUS_CACHE.addObserver(this);
		INCOMING_CACHE.addObserver(this);
		theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
		ensureFontAndColorsCreated(fonts, colors);

		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if(!IThemeManager.CHANGE_CURRENT_THEME.equals(event.getProperty())){
					return;
				}
				theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
				ensureFontAndColorsCreated(fonts, colors);
			}
		});

		MercurialEclipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if(interestingPrefs.contains(event.getProperty())){
					configureFromPreferences();
					fireLabelProviderChanged(new LabelProviderChangedEvent(ResourceDecorator.this));
				}
			}
		});
	}


	/**
	 * This method will ensure that the fonts and colors used by the decorator
	 * are cached in the registries. This avoids having to syncExec when
	 * decorating since we ensure that the fonts and colors are pre-created.
	 *
	 * @param f
	 *            fonts ids to cache
	 * @param c
	 *            color ids to cache
	 */
	private void ensureFontAndColorsCreated(final String[] f, final String[] c) {
		MercurialEclipsePlugin.getStandardDisplay().syncExec(new Runnable() {
			public void run() {
				for (int i = 0; i < c.length; i++) {
					theme.getColorRegistry().get(c[i]);
				}
				for (int i = 0; i < f.length; i++) {
					theme.getFontRegistry().get(f[i]);
				}
			}
		});
	}

	@Override
	public void dispose() {
		STATUS_CACHE.deleteObserver(this);
		INCOMING_CACHE.deleteObserver(this);
		super.dispose();
	}

	/**
	 * Init all the options we need from preferences to avoid doing this all the time
	 */
	private void configureFromPreferences() {
		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		folder_logic_2MM = LABELDECORATOR_LOGIC_2MM.equals(store.getString(LABELDECORATOR_LOGIC));
		colorise = store.getBoolean(PREF_DECORATE_WITH_COLORS);
		showChangeset = store.getBoolean(RESOURCE_DECORATOR_SHOW_CHANGESET);
		showIncomingChangeset = store.getBoolean(RESOURCE_DECORATOR_SHOW_INCOMING_CHANGESET);
	}

	public void decorate(Object element, IDecoration d) {
		IResource resource = (IResource) element;
		IProject project = resource.getProject();
		if (project == null || !project.isAccessible()) {
			return;
		}

		try {
			if (!MercurialUtilities.hgIsTeamProviderFor(resource, false)) {
				// Resource could be inside a link or something do nothing
				// in the future this could check is this is another repository
				return;
			}

			if (!STATUS_CACHE.isStatusKnown(project)) {
				// simply wait until the cache sends us an event
				d.addOverlay(DecoratorImages.notTrackedDescriptor);
				if(resource == project){
					d.addSuffix(" [Hg status pending...]");
				}
				return;
			}

			ImageDescriptor overlay = null;
			StringBuilder prefix = new StringBuilder(2);
			Integer output = STATUS_CACHE.getStatus(resource);
			if (output != null) {
				overlay = decorate(output.intValue(), prefix, d, colorise);
			} else {
				// empty folder, do nothing
			}
			if (overlay != null) {
				d.addOverlay(overlay);
			}

			if (!showChangeset) {
				if (resource.getType() == IResource.PROJECT) {
					d.addSuffix(getSuffixForProject(project));
				}
			} else {
				addChangesetInfo(d, resource, project, prefix);
			}

			// we want a prefix, even if no changeset is displayed
			if (prefix.length() > 0) {
				d.addPrefix(prefix.toString());
			}
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * @param statusBits non null hg status bits from cache
	 */
	private ImageDescriptor decorate(int statusBits, StringBuilder prefix, IDecoration d, boolean coloriseLabels) {
		ImageDescriptor overlay = null;
		// BitSet output = fr.getStatus();
		// "ignore" does not really count as modified
		if (folder_logic_2MM
				&& (Bits.cardinality(statusBits) > 2 || (Bits.cardinality(statusBits) == 2 && !Bits.contains(statusBits,
						MercurialStatusCache.BIT_IGNORE)))) {
			overlay = DecoratorImages.modifiedDescriptor;
			if (coloriseLabels) {
				setBackground(d, CHANGE_BACKGROUND_COLOR);
				setForeground(d, CHANGE_FOREGROUND_COLOR);
				setFont(d, CHANGE_FONT);
			} else {
				prefix.append('>');
			}
		} else {
			switch (Bits.highestBit(statusBits)) {
			case MercurialStatusCache.BIT_IGNORE:
				if (coloriseLabels) {
					setBackground(d, IGNORED_BACKGROUND_COLOR);
					setForeground(d, IGNORED_FOREGROUND_COLOR);
					setFont(d, IGNORED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_MODIFIED:
				overlay = DecoratorImages.modifiedDescriptor;
				if (coloriseLabels) {
					setBackground(d, CHANGE_BACKGROUND_COLOR);
					setForeground(d, CHANGE_FOREGROUND_COLOR);
					setFont(d, CHANGE_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_ADDED:
				overlay = DecoratorImages.addedDescriptor;
				if (coloriseLabels) {
					setBackground(d, ADDED_BACKGROUND_COLOR);
					setForeground(d, ADDED_FOREGROUND_COLOR);
					setFont(d, ADDED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_UNKNOWN:
				overlay = DecoratorImages.notTrackedDescriptor;
				if (coloriseLabels) {
					setBackground(d, UNKNOWN_BACKGROUND_COLOR);
					setForeground(d, UNKNOWN_FOREGROUND_COLOR);
					setFont(d, UNKNOWN_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_CLEAN:
				overlay = DecoratorImages.managedDescriptor;
				break;
				// case BIT_IGNORE:
				// do nothing
			case MercurialStatusCache.BIT_REMOVED:
				overlay = DecoratorImages.removedDescriptor;
				if (coloriseLabels) {
					setBackground(d, REMOVED_BACKGROUND_COLOR);
					setForeground(d, REMOVED_FOREGROUND_COLOR);
					setFont(d, REMOVED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_MISSING:
				overlay = DecoratorImages.deletedStillTrackedDescriptor;
				if (coloriseLabels) {
					setBackground(d, DELETED_BACKGROUND_COLOR);
					setForeground(d, DELETED_FOREGROUND_COLOR);
					setFont(d, DELETED_FONT);
				} else {
					prefix.append('>');
				}
				break;
			case MercurialStatusCache.BIT_CONFLICT:
				overlay = DecoratorImages.conflictDescriptor;
				if (coloriseLabels) {
					setBackground(d, CONFLICT_BACKGROUND_COLOR);
					setForeground(d, CONFLICT_FOREGROUND_COLOR);
					setFont(d, CONFLICT_FONT);
				} else {
					prefix.append('>');
				}
				break;
			}
		}
		return overlay;
	}

	private void addChangesetInfo(IDecoration d, IResource resource, IProject project, StringBuilder prefix) throws CoreException {
		// label info for incoming changesets
		ChangeSet newestIncomingChangeSet;
		if(showIncomingChangeset) {
			newestIncomingChangeSet = INCOMING_CACHE.getNewestChangeSet(resource);
		} else {
			newestIncomingChangeSet = null;
		}

		if (newestIncomingChangeSet != null) {
			if (prefix.length() == 0) {
				prefix.append('<');
			} else {
				prefix.insert(0, '<');
			}
		}

		// local changeset info
		try {
			// init suffix with project changeset information
			String suffix = ""; //$NON-NLS-1$
			if (resource.getType() == IResource.PROJECT) {
				suffix = getSuffixForProject(project);
			}

			// overwrite suffix for files
			if (resource.getType() == IResource.FILE) {
				suffix = getSuffixForFiles(resource, newestIncomingChangeSet);
			}

			// only decorate files and project with suffix
			if (resource.getType() != IResource.FOLDER && suffix.length() > 0) {
				d.addSuffix(suffix);
			}

		} catch (HgException e) {
			MercurialEclipsePlugin.logWarning(Messages.getString("ResourceDecorator.couldntGetVersionOfResource") + resource, e);
		}
	}

	private void setBackground(IDecoration d, String id) {
		d.setBackgroundColor(theme.getColorRegistry().get(id));
	}

	private void setForeground(IDecoration d, String id) {
		d.setForegroundColor(theme.getColorRegistry().get(id));
	}

	private void setFont(IDecoration d, String id) {
		d.setFont(theme.getFontRegistry().get(id));
	}

	private String getSuffixForFiles(IResource resource, ChangeSet cs) throws HgException {
		String suffix = ""; //$NON-NLS-1$
		// suffix for files
		if (!STATUS_CACHE.isAdded(resource.getLocation())) {
			ChangeSet fileCs = LOCAL_CACHE.getNewestChangeSet(resource);
			if (fileCs != null) {
				suffix = " [" + fileCs.getChangesetIndex() + " - " //$NON-NLS-1$ //$NON-NLS-2$
					+ fileCs.getAgeDate() + " - " + fileCs.getUser() + "]";

				if (cs != null) {
					suffix += "< [" + cs.getChangesetIndex() + ":" //$NON-NLS-1$
						+ cs.getNodeShort() + " - " + cs.getAgeDate()
						+ " - " + cs.getUser() + "]";
				}
			}
		}
		return suffix;
	}

	private String getSuffixForProject(IProject project) throws CoreException {
		ChangeSet changeSet = null;

		StringBuilder suffix = new StringBuilder();

		changeSet = LocalChangesetCache.getInstance().getChangesetByRootId(project);

		if (changeSet == null) {
			suffix.append(Messages.getString("ResourceDecorator.new"));
		} else {
			suffix.append(" ["); //$NON-NLS-1$
			String hex = changeSet.getNodeShort();
			String tags = changeSet.getTag();
			String merging = HgStatusClient.getMergeChangesetId(project);

			// rev info
			suffix.append(changeSet.getChangesetIndex()).append(':').append(hex);

			// branch
			String branch = (String) project.getSessionProperty(ResourceProperties.HG_BRANCH);
			if (branch == null || branch.length() == 0) {
				branch = Branch.DEFAULT;
			}
			suffix.append('@').append(branch);

			// tags
			if (tags != null && tags.length() > 0) {
				suffix.append('(').append(tags).append(')');
			}

			// merge flag
			if (merging != null && merging.length() > 0) {
				suffix.append(Messages.getString("ResourceDecorator.merging"));
			}
			suffix.append(']');
		}
		return suffix.toString();
	}

	public static String getDecoratorId() {
		String decoratorId = ResourceDecorator.class.getName();
		return decoratorId;
	}

	@SuppressWarnings("unchecked")
	public void update(Observable o, Object updatedObject) {
		if (updatedObject instanceof Set) {
			Set<IResource> changed = (Set<IResource>) updatedObject;
			List<IResource> notification = new ArrayList<IResource>(1000);
			int i = 0;
			for (IResource resource : changed) {
				if (i % 1000 == 0 && notification.size() > 0) {
					fireNotification(notification);
				}
				notification.add(resource);
				i++;
			}
			if (notification.size() > 0) {
				fireNotification(notification);
			}
		}
	}

	private void fireNotification(List<IResource> notification) {
		LabelProviderChangedEvent event = new LabelProviderChangedEvent(this, notification.toArray());
		fireLabelProviderChanged(event);
		notification.clear();
	}

	/**
	 * Fire a LabelProviderChangedEvent for this decorator if it is enabled, otherwise do nothing.
	 * <p>
	 * This method can be called from any thread as it will asynchroniously run a job in the user
	 * interface thread as widget updates may result.
	 * </p>
	 */
	public static void updateClientDecorations() {
		Runnable decoratorUpdate = new Runnable() {
			public void run() {
				PlatformUI.getWorkbench().getDecoratorManager().update(getDecoratorId());
			}
		};
		Display.getDefault().asyncExec(decoratorUpdate);
	}
}
