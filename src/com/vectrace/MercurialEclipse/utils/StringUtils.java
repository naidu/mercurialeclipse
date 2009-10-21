/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * adam.berkes	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

/**
 * General string manipulation functions set.
 *
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class StringUtils {

    public static final String NEW_LINE = System.getProperty("line.separator");

    public static String removeLineBreaks(String text) {
        if (text != null && !text.isEmpty()) {
            text = text.replace("\r", " ");
            return text.replace("\n", " ");
        }
        return text;
    }
}
