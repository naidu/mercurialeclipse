package com.vectrace.MercurialEclipse.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;

import com.vectrace.MercurialEclipse.exception.HgException;

public class HgIdentClient {

    public static String getCurrentRevision(IContainer root) throws HgException {
        HgCommand command = new HgCommand("ident", root, true);
        command.addOptions("-n", "-i");
        return command.executeToString().trim();
    }

    /**
     * This summary identifies the repository state using one or two parent hash
     * identifiers, followed by a "+" if there are uncommitted changes in the
     * working directory, a list of tags for this revision and a branch name for
     * non-default branches.
     * 
     * @return
     */
    public static String[] getChangeSets(String resultString) {
        // It consists of the revision id (hash), optionally a '+' sign
        // if the working tree has been modified, followed by a list of tags.
        // => we need to strip it ...
        // String changeset = getResult();
        // if (changeset.indexOf(" ") != -1) // is there a space?
        // {
        // changeset = changeset.substring(0, changeset.indexOf(" ")); // take
        // the begining until the first space
        // }
        // if (changeset.indexOf("+") != -1) // is there a +?
        // {
        // changeset = changeset.substring(0, changeset.indexOf("+")); // take
        // the begining until the first +
        // }

        // get result
        String hash = resultString.trim();
        // split it by its spaces
        String[] parts = hash.split(" ");

        // reverse it to get the revision to the front
        List<String> list = Arrays.asList(parts);
        Collections.reverse(list);

        // now we iterate over the parts to get the return value in the format
        // revision:hash
        String[] changeSets = new String[list.size() / 2];
        for (int i = 0; i < list.size(); i++) {
            if (i % 2 == 0) {
                changeSets[i] = list.get(i) + ":";
            } else {
                changeSets[i - 1] += list.get(i);
            }
        }

        // return the result to its original order
        list = Arrays.asList(changeSets);
        Collections.reverse(list);

        // ... and return it to caller
        return list.toArray(changeSets);
    }
}
