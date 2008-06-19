package com.vectrace.MercurialEclipse.commands;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgStatusClient {

    public static String getStatus(IContainer root) throws HgException {
        HgCommand command = new HgCommand("status", root, true);
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduic");
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        return command.executeToString();
    }

    public static String getStatus(IResource res) throws HgException {
        HgCommand command = new HgCommand("status", res.getProject(), true);
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduic");
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions(res.getProjectRelativePath().toOSString());
        return command.executeToString();
    }

    public static String[] getUntrackedFiles(IContainer root)
            throws HgException {
        HgCommand command = new HgCommand("status", root, true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-u", "-n");
        return command.executeToString().split("\n");
    }

    public static boolean isDirty(List<? extends IResource> resources)
            throws HgException {
        HgCommand command = new HgCommand("status", true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-mard");// modified, added, removed, deleted
        command.addFiles(resources);
        return command.executeToBytes().length != 0;
    }

    public static boolean isDirty(IProject project) throws HgException {
        HgCommand command = new HgCommand("status", project, true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions("-mard");// modified, added, removed, deleted
        return command.executeToBytes().length != 0;
    }
    
    public static String getMergeStatus(IResource res) throws HgException {
        HgCommand command = new HgCommand("id", res.getProject(), true);
        // Full global IDs
        command.addOptions("-i","--debug");
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        command.addOptions(res.getProjectRelativePath().toOSString());
        String versionIds = command.executeToString().trim();
        
        Pattern p = Pattern.compile("^[0-9a-z]+\\+([0-9a-z]+)\\+$", Pattern.MULTILINE);
        Matcher m = p.matcher(versionIds);
        if(m.matches()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * @param array
     * @return
     * @throws HgException 
     */
    public static String getStatus(IProject proj, List<IResource> files) throws HgException {
        HgCommand command = new HgCommand("status", proj, true);
        command.setUsePreferenceTimeout(MercurialPreferenceConstants.STATUS_TIMEOUT);
        // modified, added, removed, deleted, unknown, ignored, clean
        command.addOptions("-marduic");
        command.addFiles(files);
        return command.executeToString();
    }

}
