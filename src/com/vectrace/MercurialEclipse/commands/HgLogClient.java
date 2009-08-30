package com.vectrace.MercurialEclipse.commands;

import java.io.File;
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
import org.eclipse.core.runtime.Path;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistory;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class HgLogClient extends AbstractParseChangesetClient {

    private static final Pattern GET_REVISIONS_PATTERN = Pattern
            .compile("^([0-9]+):([a-f0-9]+) ([^ ]+ [^ ]+ [^ ]+) ([^#]+)#(.*)\\*\\*#(.*)$"); //$NON-NLS-1$

    public static ChangeSet[] getHeads(IProject project) throws HgException {
        HgCommand command = new HgCommand("heads", project, true); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        return getRevisions(command);
    }

    /**
     * @param command
     *            a command with optionally its Files set
     */
    private static ChangeSet[] getRevisions(HgCommand command)
            throws HgException {
        command.addOptions("--template", //$NON-NLS-1$
                "{rev}:{node} {date|isodate} {author|person}#{branches}**#{desc|firstline}\n"); //$NON-NLS-1$
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
        String[] lines = null;
        try {
            lines = command.executeToString().split("\n"); //$NON-NLS-1$
        } catch (HgException e) {
            if (!e.getMessage()
                    .contains(
                            "abort: can only follow copies/renames for explicit file names")) { //$NON-NLS-1$
                throw new HgException(e);
            }
            return null;
        }
        int length = lines.length;
        ChangeSet[] changeSets = new ChangeSet[length];
        HgRoot root = command.getHgRoot();
        for (int i = 0; i < length; i++) {
            Matcher m = GET_REVISIONS_PATTERN.matcher(lines[i]);
            if (m.matches()) {
                ChangeSet changeSet = new ChangeSet.Builder(
                        Integer.parseInt(m.group(1)), // revisions
                        m.group(2), // changeset
                        m.group(5), // branch
                        m.group(3), // date
                        m.group(4), // user
                        root).description(m.group(6)).build();

                changeSets[i] = changeSet;
            } else {
                throw new HgException(Messages.getString("HgLogClient.parseException") + lines[i] + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }

        }

        return changeSets;
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getCompleteProjectLog(
            IResource res, boolean withFiles) throws HgException {
        return getProjectLog(res, -1, -1, withFiles);
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getProjectLogBatch(
            IResource res, int batchSize, int startRev, boolean withFiles)
            throws HgException {
        return getProjectLog(res, batchSize, startRev, withFiles);
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getRecentProjectLog(
            IResource res, int limitNumber, boolean withFiles) throws HgException {
        return getProjectLogBatch(res, limitNumber, -1, withFiles);
    }

    /**
     * @return map where the key is an absolute file path
     */
    public static Map<IPath, SortedSet<ChangeSet>> getProjectLog(IResource res,
            int limitNumber, int startRev, boolean withFiles)
            throws HgException {
        try {
            AbstractShellCommand command = new HgCommand("log", getWorkingDirectory(res), //$NON-NLS-1$
                    false);
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
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

    public static Map<IPath, SortedSet<ChangeSet>> getPathLog(boolean isFile, File path,
            HgRoot root, int limitNumber, int startRev, boolean withFiles)
            throws HgException {
        try {
            AbstractShellCommand command = new HgCommand("log", root, //$NON-NLS-1$
                    false);
            command.setUsePreferenceTimeout(MercurialPreferenceConstants.LOG_TIMEOUT);
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

            if (isFile) {
                command.addOptions("-f"); //$NON-NLS-1$
            }

            command.addOptions(root.toRelative(path));

            String result = command.executeToString();
            if (result.length() == 0) {
                return null;
            }
            Map<IPath, SortedSet<ChangeSet>> revisions = createMercurialRevisions(
                    new Path(path.getAbsolutePath()),
                    result, Direction.LOCAL, null, null, new IFilePatch[0], root);
            return revisions;
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
    }


    public static ChangeSet getLogWithBranchInfo(MercurialRevision rev,
            int limitNumber, MercurialHistory history) throws HgException {
        ChangeSet changeSet = rev.getChangeSet();
        Map<IPath, SortedSet<ChangeSet>> map = getProjectLog(rev.getResource(), limitNumber, changeSet
                .getChangesetIndex(), true);
        if(map != null) {
            return map.get(rev.getResource().getLocation()).first();
        }
        File possibleParent = rev.getParent();
        MercurialRevision next = rev;
        if(possibleParent == null){
            // go up one revision, looking for the fist time "branch" occurence
            while((next = history.getNext(next)) != null){
                if(next.getParent() == null) {
                    possibleParent = HgStatusClient.getPossibleSourcePath(
                            changeSet.getHgRoot(),
                            next.getResource().getLocation().toFile(),
                            next.getRevision());
                    if(possibleParent != null){
                        break;
                    }
                } else {
                    possibleParent = next.getParent();
                    break;
                }
            }
            if(possibleParent != null) {
                while((next = history.getPrev(next)) != rev){
                    if(next == null) {
                        break;
                    }
                    next.setParent(possibleParent);
                }
            }
        }
        if(possibleParent != null){
            rev.setParent(possibleParent);
            // TODO now one can check the changesets which may have exist for the *branched*
            // file only.
            map = getPathLog(rev.getResource().getType() == IResource.FILE,
                    possibleParent, MercurialTeamProvider.getHgRoot(rev.getResource()),
                    limitNumber, rev.getRevision(), true);
            if(map!=null) {
                return  map.get(new Path(possibleParent.getAbsolutePath())).first();
            }
        }
        return null;
    }

    public static ChangeSet getChangeset(IResource res, String nodeId,
            boolean withFiles) throws HgException {

        try {
            Assert.isNotNull(nodeId);

            AbstractShellCommand command = new HgCommand("log", res.getProject().getLocation().toFile(), //$NON-NLS-1$
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
