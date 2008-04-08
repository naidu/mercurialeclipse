package com.vectrace.MercurialEclipse.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgParentClient {

    private static final Pattern ANCESTOR_PATTERN = Pattern.compile("^([0-9]+):([0-9a-f]+)$");
    
    public static int[] getParents(IProject project) throws HgException {
        HgCommand command = new HgCommand("parents", project, false);
        command.addOptions("--template", "{rev}\n");
        String[] lines = command.executeToString().split("\n");
        int[] parents = new int[lines.length];
        for(int i=0; i<lines.length; i++) {
            parents[i] = Integer.parseInt(lines[i]);
        }
        return parents;
    }
    
    public static int findCommonAncestor(IProject project, int r1, int r2) throws HgException {
        HgCommand command = new HgCommand("debugancestor", project, false);
        command.addOptions(Integer.toString(r1), Integer.toString(r2));
        String result = command.executeToString().trim();
        Matcher m = ANCESTOR_PATTERN.matcher(result);
        if(m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        throw new HgException("Parse exception: '"+result+"'");
    }

}
