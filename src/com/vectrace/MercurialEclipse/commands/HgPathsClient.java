package com.vectrace.MercurialEclipse.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgPathsClient {
    public static final String DEFAULT = "default";
    public static final String DEFAULT_PULL = "default-pull";
    public static final String DEFAULT_PUSH = "default-push";

    public static Map<String, HgRepositoryLocation> getPaths(IProject project)
            throws HgException {
        HgCommand command = new HgCommand("paths", project, true);
        return getPaths(command);
    }

    private static Map<String, HgRepositoryLocation> getPaths(HgCommand command)
            throws HgException {
        String result = command.executeToString();

        return parse(result);
    }

    static Map<String, HgRepositoryLocation> parse(String result) {
        StringReader string = new StringReader(result);

        BufferedReader reader = new BufferedReader(string);

        Map<String, HgRepositoryLocation> urlByName = new HashMap<String, HgRepositoryLocation>();
        String line;
        try {
            while (null != (line = reader.readLine())) {
                String[] parts = line.split("=", 2);
                String name = parts[0].trim();
                String url = parts[1].trim();
                HgRepositoryLocation location = new HgRepositoryLocation(url);
                urlByName.put(name, location);
            }
        } catch (IOException e) {
            // This should never happen
            throw new RuntimeException(e);
        }
        return urlByName;
    }
}
