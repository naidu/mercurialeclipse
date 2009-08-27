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

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class FlagPropertyTester extends org.eclipse.core.expressions.PropertyTester {

    public final static String PROPERTY_STATUS = "status"; //$NON-NLS-1$
    
    @SuppressWarnings("serial")
    private final static Map<Object, Integer> BIT_MAP = new HashMap<Object, Integer>() {
        {
            put("added", MercurialStatusCache.BIT_ADDED); //$NON-NLS-1$
            put("clean", MercurialStatusCache.BIT_CLEAN); //$NON-NLS-1$
            put("deleted", MercurialStatusCache.BIT_MISSING); //$NON-NLS-1$
            put("ignore", MercurialStatusCache.BIT_IGNORE); //$NON-NLS-1$
            put("modified", MercurialStatusCache.BIT_MODIFIED); //$NON-NLS-1$
            put("removed", MercurialStatusCache.BIT_REMOVED); //$NON-NLS-1$
            put("unknown", MercurialStatusCache.BIT_UNKNOWN); //$NON-NLS-1$
        }
    };
    
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if(PROPERTY_STATUS.equals(property)) {
            try {
                IResource res = (IResource)receiver;
                BitSet test = new BitSet();
                for(Object arg: args) {
                    test.set(BIT_MAP.get(arg));
                }
                BitSet status = MercurialStatusCache.getInstance().getStatus(res);
                if (status != null) {
                    test.and(status);                
                    return !test.isEmpty();
                }                
            } catch (Exception e) {
                MercurialEclipsePlugin.logWarning("Could not test status field "+expectedValue+" on "+receiver, e); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
        }
        return false;
    }
    
}
