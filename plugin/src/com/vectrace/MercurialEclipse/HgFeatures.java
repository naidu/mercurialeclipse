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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Version;

/**
 * Features of the underlined mercurial executable. HgFeatures are version dependent and so will be
 * enabled/disabled based on the currently used mercurial binaries.
 * <p>
 * Note that the right enablement state will be set some time after plugin startup, so that in a
 * short time between plugin activation and {@link MercurialEclipsePlugin#checkHgInstallation()}
 * features might be not yet initialized properly. Initially all features are disabled.
 *
 * @author andrei
 */
public enum HgFeatures {

	/**
	 * TODO: find exact version
	 */
	COMMAND_SERVER (new Version(2,0,0), "command server", true),

	COMMAND_SERVER_RECOMMENDED (new Version(2,1,0), "tested version of command server", false),

	/**
	 * Whether commmit --amend is supported
	 */
	COMMIT_AMEND (new Version(2,2,0), "commit --amend", false);

	private final Version required;
	private final String[] optionalPreferenceKeys;
	private boolean enabled;
	private final String cmd;
	private final boolean mandatory;

	private HgFeatures(Version required, String cmd, boolean mandatory, String ... optionalPreferenceKeys) {
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
	 * features might be not yet initialized properly. Initially all features are disabled.
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
		HgFeatures[] values = HgFeatures.values();
		for (HgFeatures feature : values) {
			feature.setEnabled(doCompare(feature.getRequired(), current)<= 0);
		}
	}

	/**
	 * @param current
	 *            observed mercurial version
	 * @return true if <b>mandatory</b> options are satisfied with given version, false if at
	 *         least one requires greater mercurial version
	 */
	public static boolean isSupported(Version current) {
		HgFeatures[] values = HgFeatures.values();
		boolean result = true;
		for (HgFeatures feature : values) {
			if(!feature.isMandatory()) {
				continue;
			}
			result &= doCompare(feature.getRequired(), current)<= 0;
		}
		return result;
	}

	/**
	 * @param current
	 *            observed mercurial version
	 * @return true if <b>all</b> features are satisfied with given version, false if at
	 *         least one requires greater mercurial version
	 */
	public static boolean isHappyWith(Version current) {
		HgFeatures[] values = HgFeatures.values();
		boolean result = true;
		for (HgFeatures feature : values) {
			result &= doCompare(feature.getRequired(), current)<= 0;
		}
		return result;
	}

	public static void applyAllTo(IPreferenceStore store) {
		HgFeatures[] values = HgFeatures.values();
		for (HgFeatures feature : values) {
			feature.applyTo(store);
		}
	}

	public static Version getPreferredVersion() {
		return Collections.max(Arrays.asList(HgFeatures.values()), new Comparator<HgFeatures>() {
			public int compare(HgFeatures o1, HgFeatures o2) {
				return doCompare(o1.getRequired(), o2.getRequired());
			}
		}).getRequired();
	}

	public static Version getLowestWorkingVersion() {
		return Collections.min(Arrays.asList(HgFeatures.values()), new Comparator<HgFeatures>() {
			public int compare(HgFeatures o1, HgFeatures o2) {
				if(o1.isMandatory() && !o2.isMandatory()) {
					return -1;
				} else if(!o1.isMandatory() && o2.isMandatory()){
					return 1;
				}
				return doCompare(o1.getRequired(), o2.getRequired());
			}
		}).getRequired();
	}

	public static String printSummary() {
		StringBuilder sb = new StringBuilder();
		HgFeatures[] values = HgFeatures.values();
		for (HgFeatures feature : values) {
			sb.append(feature.toString()).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Support comparing versions without requiring a particular version of OSGi.
	 *
	 * See: http://computerfloss.com/2011/11/a-little-problem-in-the-osgi-version-class/
	 *
	 * Remove after Eclipse 3.6 is not supported.
	 *
	 * @param v1 The left version
	 * @param v2 The right version
	 * @return The result of left.compareTo(right)
	 * @see Version#compareTo(Version)
	 * @see Version#compareTo(Object)
	 */
	protected static int doCompare(Version v1, Version v2) {
		Class<Version> versionClass = Version.class;
		Method compareToMethod = null;

		try {
		    // Works on Eclipse 3.7
		    compareToMethod = versionClass.getMethod("compareTo", Version.class);
		} catch (NoSuchMethodException e1) {
		    // "We're on Eclipse 3.6 or earlier. Fall back compareTo(Object).";
		    try {
		        // Works on Eclipse 3.6 and earlier
		        compareToMethod = versionClass.getMethod("compareTo", Object.class);
		    } catch (NoSuchMethodException e2) {
		        MercurialEclipsePlugin.logError("Unexpected error: cannot find compareTo() in Version", e2);
		        return 0;
		    }
		}

		try {
			return ((Integer)compareToMethod.invoke(v1, v2)).intValue();
		} catch (IllegalArgumentException e) {
			MercurialEclipsePlugin.logError("Unexpected error: comparing version", e);
		} catch (IllegalAccessException e) {
			MercurialEclipsePlugin.logError("Unexpected error: comparing version", e);
		} catch (InvocationTargetException e) {
			MercurialEclipsePlugin.logError("Unexpected error: comparing version", e);
		}

		return 0;
	}
}
