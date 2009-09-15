/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Bastian Doetsch
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import static com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class FlagPropertyTester extends org.eclipse.core.expressions.PropertyTester {

    public final static String PROPERTY_STATUS = "status"; //$NON-NLS-1$

    @SuppressWarnings({ "serial", "boxing" })
    private final static Map<Object, Integer> BIT_MAP = new HashMap<Object, Integer>() {
        {
            put("added", BIT_ADDED); //$NON-NLS-1$
            put("clean", BIT_CLEAN); //$NON-NLS-1$
            put("deleted", BIT_MISSING); //$NON-NLS-1$
            put("ignore", BIT_IGNORE); //$NON-NLS-1$
            put("modified", BIT_MODIFIED); //$NON-NLS-1$
            put("removed", BIT_REMOVED); //$NON-NLS-1$
            put("unknown", BIT_UNKNOWN); //$NON-NLS-1$
        }
    };

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if(PROPERTY_STATUS.equals(property)) {
            try {
                IResource res = (IResource)receiver;
                int test = 0;
                for(Object arg: args) {
                    Integer statusBit = BIT_MAP.get(arg);
                    if(statusBit == null){
                        String message = "Could not test status " + property + " on "  //$NON-NLS-1$ //$NON-NLS-2$
                            + receiver + " for argument: " + arg; //$NON-NLS-1$
                        MercurialEclipsePlugin.logWarning(message, new IllegalArgumentException(message));
                        continue;
                    }
                    test |= statusBit.intValue();
                }
                MercurialStatusCache cache = MercurialStatusCache.getInstance();
                Integer status = cache.getStatus(res);
                if (status != null) {
                    test &= status.intValue();
                    return test != 0;
                } else if(test == MercurialStatusCache.BIT_IGNORE) {
                    // ignored files are not tracked by cache, so the state is always null
                    // we assume it is ignored if the project state is known
                    return cache.isStatusKnown(res.getProject());
                }
            } catch (Exception e) {
                MercurialEclipsePlugin.logWarning("Could not test status " + property + " on " + receiver, e); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
        }
        return false;
    }

}
