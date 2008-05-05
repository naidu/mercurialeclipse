package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.dialogs.MultiLineDialog;

public class GLogHandler extends SingleResourceHandler {

    @Override
    protected void run(IResource resource) throws Exception {
        new MultiLineDialog(
                getShell(),
                "Graphical Log",
                HgLogClient
                        .getGraphicalLog(
                                resource.getProject(),
                                "{rev}:{node|short} {date|isodate} {author|person} {desc|escape} \n",
                                resource.getProjectRelativePath().toOSString())
        ).open();
    }

}
