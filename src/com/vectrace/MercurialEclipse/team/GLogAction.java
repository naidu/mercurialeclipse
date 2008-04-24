package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.dialogs.MultiLineDialog;

public class GLogAction extends SingleResourceAction {

    @Override
    protected void run(IResource resource) throws Exception {
        new MultiLineDialog(
                getShell(),
                "Graphical Log",
                HgLogClient.getGraphicalLog(resource.getProject())
        ).open();
    }

}
