/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Andrei	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

public class Bits {
    private Bits(){

    }

    public static boolean contains(int source, int bit) {
        return (source & bit) != 0;
    }

    public static int clear(int source, int bit) {
        return source & ~bit;
    }


    public static int highestBit(int source) {
        return Integer.highestOneBit(source);
    }

    public static int cardinality(int source) {
        return Integer.bitCount(source);
    }

}