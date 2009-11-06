/*******************************************************************************
 * Copyright (c) 2009 Intland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Berkes (Intland) - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Create a new crypter using stored secret key
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class HgRepositoryAuthCrypterFactory {

    public final static String DEFAULT_KEY_FILENAME = ".key";

    public static HgRepositoryAuthCrypter create(File keyFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
        KeySpec keySpec = new DESedeKeySpec(getBytesFromFile(keyFile));
        SecretKey key = SecretKeyFactory.getInstance(HgRepositoryAuthCrypter.DEFAULT_ALGORITHM).generateSecret(keySpec);
        return new HgRepositoryAuthCrypter(key);
    }

    public static HgRepositoryAuthCrypter create() {
        try {
            File keyFile = MercurialEclipsePlugin.getDefault().getStateLocation().append(DEFAULT_KEY_FILENAME).toFile();
            if (keyFile != null && keyFile.isFile()) {
                return create(keyFile);
            }
            SecretKey key = HgRepositoryAuthCrypter.generateKey();
            writeBytesToFile(key.getEncoded(), keyFile);
            return new HgRepositoryAuthCrypter(key);
        } catch (Exception ex) {
            MercurialEclipsePlugin.logError(ex);
        }
        return null;
    }

    private static byte[] getBytesFromFile(File file) throws IOException {
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
            long length = file.length();
            byte[] bytes = new byte[(int)length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
            return bytes;
        }
        finally {
            is.close();
        }
    }

    protected static void writeBytesToFile(byte[] content, File file) throws IOException {
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        try {
            os.write(content, 0, content.length);
        } finally {
            os.close();
            // Ensure file availability
            file.setReadable(true, true);
            file.setWritable(false, true);
        }
    }
}
