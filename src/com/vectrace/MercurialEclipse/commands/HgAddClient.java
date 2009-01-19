package com.vectrace.MercurialEclipse.commands;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public class HgAddClient {

    public static void addResources(List<IResource> resources,
            IProgressMonitor monitor) throws HgException {
        Map<HgRoot, List<IResource>> resourcesByRoot;
        try {
            resourcesByRoot = HgCommand
                    .groupByRoot(resources);
        } catch (IOException e) {
            throw new HgException(e.getLocalizedMessage(), e);
        }
        for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
            HgRoot root = mapEntry.getKey();
            if (monitor != null) {
                monitor.subTask(Messages.getString("HgAddClient.addingResourcesFrom") + root.getName()); //$NON-NLS-1$
            }
            // if there are too many resources, do several calls
            int size = resources.size();
            int delta = AbstractShellCommand.MAX_PARAMS - 1;
            for (int i = 0; i < size; i += delta) {
                AbstractShellCommand command = new HgCommand("add", root, //$NON-NLS-1$
                        true);
                command.setUsePreferenceTimeout(MercurialPreferenceConstants.ADD_TIMEOUT);
                command.addFiles(mapEntry.getValue().subList(i,
                        Math.min(i + delta, size - i)));
                command.executeToBytes();
            }
        }
    }
}
