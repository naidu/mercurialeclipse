/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 			Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Version;

import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * Options for the mercurial executable. HgOptions are version dependent and so will be
 * enabled/disabled based on the currently used mercurial binaries.
 * <p>
 * Note that the right enablement state will be set some time after plugin startup, so that in a
 * short time between plugin activation and {@link MercurialEclipsePlugin#checkHgInstallation()}
 * options might be not yet initialized properly. Initially all options are disabled.
 *
 * @author andrei
 */
public enum HgOptions {

	BRANCH (new Version(1,5,0), "--branch", true),
	NEW_BRANCH (new Version(1,6,0), "--new-branch", false, MercurialPreferenceConstants.PREF_PUSH_NEW_BRANCH),
	LISTFILE (new Version(1,8,0), "listfile:", false);

	private final Version required;
	private final String[] optionalPreferenceKeys;
	private boolean enabled;
	private final String cmd;
	private final boolean mandatory;

	private HgOptions(Version required, String cmd, boolean mandatory, String ... optionalPreferenceKeys) {
		this.required = required;
		this.cmd = cmd;
		this.mandatory = mandatory;
		this.optionalPreferenceKeys = optionalPreferenceKeys;
	}

	@Override
	public String toString() {
		return getHgCmd() + (isEnabled() ? "(enabled)" : "(disabled)") + ", requires "
				+ getRequired() + " hg version";
	}

	/**
	 * @return true if this is a mandatory command and there is no chance to
	 * workaround the absence of the required mercurial version
	 */
	public boolean isMandatory() {
		return mandatory;
	}

	public String getHgCmd() {
		return cmd;
	}

	/**
	 * @return the (smallest) required version, never null
	 */
	public Version getRequired() {
		return required;
	}

	public void applyTo(IPreferenceStore store) {
		for (String key : optionalPreferenceKeys) {
			store.setDefault(key, enabled);
			if(!enabled) {
				store.setValue(key, false);
			}
		}
	}

	/**
	 * @param enabled true if the option should be enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Note that the right enablement state will be set some time after plugin startup, so that in a
	 * short time between plugin activation and {@link MercurialEclipsePlugin#checkHgInstallation()}
	 * options might be not yet initialized properly. Initially all options are disabled.
	 *
	 * @return true if the option is enabled (supported by mercurial)
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param current observed mercurial version
	 */
	public static void setToVersion(Version current) {
		HgOptions[] values = HgOptions.values();
		for (HgOptions option : values) {
			option.setEnabled(option.getRequired().compareTo(current)<= 0);
		}
	}

	/**
	 * @param current
	 *            observed mercurial version
	 * @return true if <b>mandatory</b> options are satisfied with given version, false if at
	 *         least one requires greater mercurial version
	 */
	public static boolean isSupported(Version current) {
		HgOptions[] values = HgOptions.values();
		boolean result = true;
		for (HgOptions option : values) {
			if(!option.isMandatory()) {
				continue;
			}
			result &= option.getRequired().compareTo(current)<= 0;
		}
		return result;
	}

	/**
	 * @param current
	 *            observed mercurial version
	 * @return true if <b>all</b> options are satisfied with given version, false if at
	 *         least one requires greater mercurial version
	 */
	public static boolean isHappyWith(Version current) {
		HgOptions[] values = HgOptions.values();
		boolean result = true;
		for (HgOptions option : values) {
			result &= option.getRequired().compareTo(current)<= 0;
		}
		return result;
	}

	public static void applyAllTo(IPreferenceStore store) {
		HgOptions[] values = HgOptions.values();
		for (HgOptions option : values) {
			option.applyTo(store);
		}
	}

	public static Version getPreferredVersion() {
		return Collections.max(Arrays.asList(HgOptions.values()), new Comparator<HgOptions>() {
			public int compare(HgOptions o1, HgOptions o2) {
				return o1.getRequired().compareTo(o2.getRequired());
			}
		}).getRequired();
	}

	public static Version getLowestWorkingVersion() {
		return Collections.min(Arrays.asList(HgOptions.values()), new Comparator<HgOptions>() {
			public int compare(HgOptions o1, HgOptions o2) {
				if(o1.isMandatory() && !o2.isMandatory()) {
					return -1;
				} else if(!o1.isMandatory() && o2.isMandatory()){
					return 1;
				}
				return o1.getRequired().compareTo(o2.getRequired());
			}
		}).getRequired();
	}

	public static String printSummary() {
		StringBuilder sb = new StringBuilder();
		HgOptions[] values = HgOptions.values();
		for (HgOptions option : values) {
			sb.append(option.toString()).append("\n");
		}
		return sb.toString();
	}
}
