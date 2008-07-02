package com.vectrace.MercurialEclipse.menu;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.HgFile;
import com.vectrace.MercurialEclipse.annotations.ShowAnnotationOperation;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * 
 */
public class ShowAnnotationHandler extends AbstractHandler {

    protected Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    @SuppressWarnings("unchecked")
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            IEvaluationContext context = ((IEvaluationContext) event.getApplicationContext());

            Object selectionObject = context.getDefaultVariable();
            IAdaptable selectionAdaptable = (IAdaptable) ((List) selectionObject).get(0);
            IFile file = (IFile) selectionAdaptable.getAdapter(IResource.class);

            IWorkbenchPart part = (IWorkbenchPart) context.getVariable("activePart");

            new ShowAnnotationOperation(part, new HgFile(file)).run();
        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Hg says...", e.getMessage()
                    + "\nSee Error Log for more details.");
            throw new ExecutionException(e.getMessage(), e);
        }
        return null;
    }

}
