package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgPathsClient {
    public static final String DEFAULT = "default"; //$NON-NLS-1$
    public static final String DEFAULT_PULL = "default-pull"; //$NON-NLS-1$
    public static final String DEFAULT_PUSH = "default-push"; //$NON-NLS-1$

    public static Map<String, String> getPaths(IProject project)
            throws HgException {
        HgCommand command = new HgCommand("paths", project, true); //$NON-NLS-1$
        return getPaths(command);
    }

    private static Map<String, String> getPaths(HgCommand command)
            throws HgException {
        String result = command.executeToString();

        return parse(result);
    }

    static Map<String, String> parse(String result) {
        StringReader string = new StringReader(result);

        BufferedReader reader = new BufferedReader(string);

        Map<String, String> urlByName = new HashMap<String, String>();
        String line;
        try {
            while (null != (line = reader.readLine())) {
                String[] parts = line.split("=", 2); //$NON-NLS-1$
                String name = parts[0].trim();
                String url = parts[1].trim();
                urlByName.put(name, url);
            }
        } catch (Exception e) {
            // This should never happen
            throw new RuntimeException(e);
        }
        return urlByName;
    }
}
