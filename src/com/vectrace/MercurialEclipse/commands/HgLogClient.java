package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgLogClient extends AbstractParseChangesetClient {

    private static final Pattern GET_REVISIONS_PATTERN = Pattern
            .compile("^([0-9]+):([a-f0-9]+) ([^ ]+ [^ ]+ [^ ]+) ([^#]+)#(.*)\\*\\*#(.*)$"); //$NON-NLS-1$
    
    public static ChangeSet[] getHeads(IProject project) throws HgException {
        AbstractShellCommand command = new HgCommand("heads", project, true); //$NON-NLS-1$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        return getRevisions(command);
    }

    /**
     * 
     * @param command
     *            a command with optionally its Files set
     * @return
     * @throws HgException
     */
    private static ChangeSet[] getRevisions(AbstractShellCommand command)
            throws HgException {
        command.addOptions("--template", //$NON-NLS-1$
                "{rev}:{node} {date|isodate} {author|person}#{branches}**#{desc|firstline}\n"); //$NON-NLS-1$
        command
                .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        String[] lines = null;
        try {
            lines = command.executeToString().split("\n"); //$NON-NLS-1$
        } catch (HgException e) {
            if (!e
                    .getMessage()
                    .contains(
                            "abort: can only follow copies/renames for explicit file names")) { //$NON-NLS-1$
                throw new HgException(e);
            }
            return null;
        }
        int length = lines.length;
        ChangeSet[] changeSets = new ChangeSet[length];
        for (int i = 0; i < length; i++) {
            Matcher m = GET_REVISIONS_PATTERN.matcher(lines[i]);
            if (m.matches()) {
                ChangeSet changeSet = new ChangeSet.Builder(
                        Integer.parseInt(m.group(1)), // revisions
                        m.group(2), // changeset
                        m.group(5), // branch
                        m.group(3), // date
                        m.group(4) // user
                        ).description(m.group(6)).build();
                
                changeSets[i] = changeSet;
            } else {
                throw new HgException(Messages.getString("HgLogClient.parseException") + lines[i] + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }

        }

        return changeSets;
    }

    public static Map<IPath, SortedSet<ChangeSet>> getCompleteProjectLog(
            IResource res, boolean withFiles) throws HgException {
        return getProjectLog(res, -1, -1, withFiles);
    }

    public static Map<IPath, SortedSet<ChangeSet>> getProjectLogBatch(
            IResource res, int batchSize, int startRev, boolean withFiles)
            throws HgException {
        return getProjectLog(res, batchSize, startRev, withFiles);
    }

    public static Map<IPath, SortedSet<ChangeSet>> getRecentProjectLog(
            IResource res, int limitNumber, boolean withFiles) throws HgException {
        return getProjectLogBatch(res, limitNumber, -1, withFiles);
    }

    public static Map<IPath, SortedSet<ChangeSet>> getProjectLog(IResource res,
            int limitNumber, int startRev, boolean withFiles)
            throws HgException {
        try {
            AbstractShellCommand command = new HgCommand("log", getWorkingDirectory(res), //$NON-NLS-1$
                    false);
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
            command.addOptions("--debug", "--style", //$NON-NLS-1$ //$NON-NLS-2$
                    AbstractParseChangesetClient.getStyleFile(withFiles)
                            .getCanonicalPath());

            if (startRev >= 0 && startRev != Integer.MAX_VALUE) {                
                int last = Math.max(startRev - limitNumber, 0);
                command.addOptions("-r"); //$NON-NLS-1$
                command.addOptions(startRev + ":" + last); //$NON-NLS-1$
            }

            if (limitNumber > 0) {
                command.addOptions("-l", limitNumber + ""); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (res.getType() == IResource.FILE) {
                command.addOptions("-f"); //$NON-NLS-1$
            }

            if (res.getType() != IResource.PROJECT) {
                command.addOptions(res.getLocation().toOSString());
            }

            String result = command.executeToString();
            if (result.length() == 0) {
                return null;
            }
            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    res, result, withFiles, Direction.LOCAL, null, null, new IFilePatch[0]);
            return revisions;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }
       

    /**
     * @param nodeId
     * @throws HgException
     */
    public static ChangeSet getChangeset(IResource res, String nodeId,
            boolean withFiles) throws HgException {

        try {
            Assert.isNotNull(nodeId);

            AbstractShellCommand command = new HgCommand("log", getWorkingDirectory(res), //$NON-NLS-1$
                    false);
            command
                    .setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
            command.addOptions("--debug", "--style", AbstractParseChangesetClient //$NON-NLS-1$ //$NON-NLS-2$
                    .getStyleFile(withFiles).getCanonicalPath());
            command.addOptions("--rev", nodeId); //$NON-NLS-1$
            String result = command.executeToString();

            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    res, result, withFiles, Direction.LOCAL, null, null, new IFilePatch[0]);
            SortedSet<ChangeSet> set = revisions.get(res.getLocation());
            if (set != null) {
                return set.first();
            }
            return null;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }
}
