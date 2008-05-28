package com.vectrace.MercurialEclipse.commands;

import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgLogClient extends AbstractParseChangesetClient {

    private static final Pattern GET_REVISIONS_PATTERN = Pattern
            .compile("^([0-9]+):([a-f0-9]+) ([^ ]+ [^ ]+ [^ ]+) ([^#]+)#(.*)$");

    public static ChangeSet[] getRevisions(IProject project) throws HgException {
        HgCommand command = new HgCommand("log", project, true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        return getRevisions(command);
    }

    public static ChangeSet[] getRevisions(IFile file) throws HgException {
        HgCommand command = new HgCommand("log", file.getParent(), true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        command.addOptions("-f");
        command.addFiles(file.getName());
        return getRevisions(command);
    }

    public static ChangeSet[] getHeads(IProject project) throws HgException {
        HgCommand command = new HgCommand("heads", project, true);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        return getRevisions(command);
    }

    public static String getGraphicalLog(IProject project, String template,
            String filename) throws HgException {
        HgCommand command = new HgCommand("glog", project, false);
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        command.addOptions("--template", template);
        command.addOptions("--config", "extensions.hgext.graphlog=");
        command.addOptions(filename);
        return command.executeToString();
    }        

    /**
     * 
     * @param command
     *            a command with optionally its Files set
     * @return
     * @throws HgException
     */
    private static ChangeSet[] getRevisions(HgCommand command)
            throws HgException {
        command.addOptions("--template",
                "{rev}:{node} {date|isodate} {author|person}#{branches}\n");
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        String[] lines = null;
        try {
            lines = command.executeToString().split("\n");
        } catch (HgException e) {
            if (!e
                    .getMessage()
                    .contains(
                            "abort: can only follow copies/renames for explicit file names")) {
                throw new HgException(e);
            }
            return null;
        }
        int length = lines.length;
        ChangeSet[] changeSets = new ChangeSet[length];
        for (int i = 0; i < length; i++) {
            Matcher m = GET_REVISIONS_PATTERN.matcher(lines[i]);
            if (m.matches()) {
                ChangeSet changeSet = new ChangeSet(Integer
                        .parseInt(m.group(1)), m.group(2), m.group(4), m
                        .group(3), m.group(5));
                changeSets[i] = changeSet;
            } else {
                throw new HgException("Parse exception: '" + lines[i] + "'");
            }

        }

        return changeSets;
    }

    public static Map<IResource, SortedSet<ChangeSet>> getCompleteProjectLog(
            IResource res, boolean withFiles) throws HgException {
        return getProjectLog(res, -1, -1, withFiles);
    }

    public static Map<IResource, SortedSet<ChangeSet>> getProjectLogBatch(
            IResource res, int batchSize, int startRev, boolean withFiles) throws HgException {
        return getProjectLog(res, batchSize, startRev, withFiles);
    }

    public static Map<IResource, SortedSet<ChangeSet>> getRecentProjectLog(
            IResource res, int limitNumber, boolean withFiles) throws HgException {
        return getProjectLogBatch(res, limitNumber, -1, withFiles);
    }

    public static Map<IResource, SortedSet<ChangeSet>> getProjectLog(
            IResource res, int limitNumber, int startRev, boolean withFiles) throws HgException {
        HgCommand command = new HgCommand("log", res.getProject(), false);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        command.addOptions("--debug", "--style", AbstractParseChangesetClient.getStyleFile(withFiles).getAbsolutePath());
       
        if (startRev >= 0) {
            int last = Math.max(startRev - limitNumber, 0);
            command.addOptions("-r");
            command.addOptions(startRev + ":" + last);
        }

        if (limitNumber > -1) {
            command.addOptions("-l", limitNumber + "");
        }
        if (!(res instanceof IProject)) {
            //command.addOptions("-f");
            command.addOptions(res.getProjectRelativePath().toOSString());
        }
        String result = command.executeToString();
        if (result.length() == 0) {
            return null;
        }
        Map<IResource, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                result, res.getProject(), withFiles, Direction.LOCAL,
                null, null);
        return revisions;
    }

}
