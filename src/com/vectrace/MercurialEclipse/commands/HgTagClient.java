package com.vectrace.MercurialEclipse.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Tag;

public class HgTagClient {

    private static final Pattern GET_TAGS_PATTERN = Pattern
            .compile("^(.+[^ ]) +([0-9]+):([a-f0-9]+)( local)?$");

    public static Tag[] getTags(IProject project) throws HgException {
        HgCommand command = new HgCommand("tags", project, false);
        command.addOptions("-v");
        String[] lines = command.executeToString().split("\n");
        int length = lines.length;
        Tag[] tags = new Tag[length];
        for (int i = 0; i < length; i++) {
            Matcher m = GET_TAGS_PATTERN.matcher(lines[i]);
            if (m.matches()) {
                Tag tag = new Tag(m.group(1), Integer.parseInt(m.group(2)), m
                        .group(3), m.group(4) != null);
                tags[i] = tag;
            } else {
                throw new HgException("Parse exception: '" + lines[i] + "'");
            }
        }
        return tags;
    }

    /**
     * 
     * @param resource
     * @param name
     * @param user
     *            if null, uses the default user
     * @param local
     * @throws HgException
     */
    public static void addTag(IResource resource, String name, String rev,
            String user, boolean local, boolean force) throws HgException {
        HgCommand command = new HgCommand("tag", resource.getProject(), false);
        if (local) {
            command.addOptions("-l");
        }
        if (force) {
            command.addOptions("-f");
        }
        if (rev != null) {
            command.addOptions("-r", rev);
        }
        command.addUserName(user);
        command.addOptions(name);
        command.executeToBytes();
    }

}
