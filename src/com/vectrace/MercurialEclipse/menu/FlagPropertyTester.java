/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.FlaggedResource;

public class FlagPropertyTester extends org.eclipse.core.expressions.PropertyTester {

    public final static String PROPERTY_STATUS = "status";
    
    @SuppressWarnings("serial")
    private final static Map<Object, Integer> BIT_MAP = new HashMap<Object, Integer>() {
        {
            put("added", FlaggedResource.BIT_ADDED);
            put("clean", FlaggedResource.BIT_CLEAN);
            put("deleted", FlaggedResource.BIT_DELETED);
            put("ignore", FlaggedResource.BIT_IGNORE);
            put("modified", FlaggedResource.BIT_MODIFIED);
            put("removed", FlaggedResource.BIT_REMOVED);
            put("unknown", FlaggedResource.BIT_UNKNOWN);
        }
    };
    
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if(PROPERTY_STATUS.equals(property)) {
            try {
                FlaggedResource fg = (FlaggedResource)receiver;
                BitSet test = new BitSet();
                for(Object arg: args) {
                    test.set(BIT_MAP.get(arg));
                }
                BitSet status = fg.getStatus();
                
                test.and(status);
                
                return !test.isEmpty();
                
            } catch (Exception e) {
                MercurialEclipsePlugin.logWarning("Could not test status field "+expectedValue, e);
                return false;
            }
        }
        return false;
    }
    
}
