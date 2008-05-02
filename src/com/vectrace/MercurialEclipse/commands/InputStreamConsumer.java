/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class InputStreamConsumer extends Thread {
    private final InputStream stream;
    private byte[] output;

    public InputStreamConsumer(InputStream stream) {
        this.stream = new BufferedInputStream(stream);
    }

    @Override
    public void run() {
        try {
            int length;
            byte[] buffer = new byte[1024];
            ByteArrayOutputStream myOutput = new ByteArrayOutputStream();
            while ((length = stream.read(buffer)) != -1) {
                myOutput.write(buffer, 0, length);
            }
            stream.close();
            this.output = myOutput.toByteArray();
        } catch (IOException e) {
            // TODO report the error to the caller thread
            MercurialEclipsePlugin.logError(e);
        }
    }

    public byte[] getBytes() {
        return output;
    }

}