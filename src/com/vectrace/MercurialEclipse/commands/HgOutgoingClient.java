package com.vectrace.MercurialEclipse.commands;

import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocation;

public class HgOutgoingClient {

    public static Map<IResource, SortedSet<ChangeSet>> getOutgoing(
            IResource res, HgRepositoryLocation loc) throws HgException {
        try {
            HgCommand command = new HgCommand("outgoing", res.getProject(),
                    false);

            command.addOptions("--template", HgIncomingClient.TEMPLATE);

            command.addOptions(loc.toString());
            String result = command.executeToString();
            if (result.contains("no changes found")) {
                return null;
            }
            Map<IResource, SortedSet<ChangeSet>> revisions = HgIncomingClient
                    .createMercurialRevisions(result, res.getProject(), null,
                            loc, Direction.OUTGOING);
            return revisions;
        } catch (HgException hg) {
            if (hg.getMessage().contains("return code: 1")) {
                return null;
            }
            throw new HgException(hg.getMessage(), hg);
        }
    }

}
