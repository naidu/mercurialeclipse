/*******************************************************************************
 * Copyright (c) ?
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     StefanC           - jobs framework, code cleenup
 *******************************************************************************/

package com.vectrace.MercurialEclipse.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;

import com.vectrace.MercurialEclipse.HgFile;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.model.ChangeSet;

public class ShowAnnotationOperation extends TeamOperation
{
  private static final String DEFAULT_TEXT_EDITOR_ID = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
  private final HgFile remoteFile;

  public ShowAnnotationOperation(IWorkbenchPart part, HgFile remoteFile)
  {
    super(part);
    this.remoteFile = remoteFile;
  }

  public void run(IProgressMonitor monitor) throws InvocationTargetException,
      InterruptedException
  {
    monitor.beginTask(null, 100);

    try
    {
      final AnnotateBlocks annotateBlocks = new AnnotateCommand(remoteFile).execute();

      // this is not needed if there is no live annotate
      final RevisionInformation information = createRevisionInformation(
          annotateBlocks, monitor);

      // We aren't running from a UI thread
      new SafeUiJob("Hg Annotate") 
      {
          @Override
          protected IStatus runSafe(IProgressMonitor monitor) 
          {
              monitor.beginTask("Applying Annotation", IProgressMonitor.UNKNOWN);
              final AbstractDecoratedTextEditor editor = getEditor();
              if (editor != null)
              {
                  editor
                  .showRevisionInformation(information,
                  HgPristineCopyQuickDiffProvider.HG_REFERENCE_PROVIDER); //$NON-NLS-1$
                  
              }
              monitor.done();
              return super.runSafe(monitor);
          }
      }.schedule();

    } catch(Exception e) {
      MercurialEclipsePlugin.logError(e);
    } finally
    {
      monitor.done();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.team.ui.TeamOperation#getGotoAction()
   */
  @Override
protected IAction getGotoAction()
  {
    return super.getGotoAction();
  }

  private AbstractDecoratedTextEditor getEditor()
  {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
    IEditorReference[] references = window.getActivePage()
        .getEditorReferences();
    IResource resource = remoteFile.getFile();
    if (resource == null) {
        return null;
    }

    for (int i = 0; i < references.length; i++)
    {
      IEditorReference reference = references[i];
      try
      {
        if (resource != null
            && resource.equals(reference.getEditorInput().getAdapter(
                IFile.class)))
        {
          IEditorPart editor = reference.getEditor(false);
          if (editor instanceof AbstractDecoratedTextEditor) {
            return (AbstractDecoratedTextEditor) editor;
        }
        // editor opened is not a text editor - reopen file using the
        // defualt text editor
        IEditorPart part = getPart().getSite().getPage().openEditor(
            new FileEditorInput((IFile) resource),
            DEFAULT_TEXT_EDITOR_ID, true,
            IWorkbenchPage.MATCH_NONE);
        if (part != null && part instanceof AbstractDecoratedTextEditor) {
            return (AbstractDecoratedTextEditor) part;
        }
        }
      } catch (PartInitException e)
      {
        // ignore
      }
    }

    // no existing editor references found, try to open a new editor for the
    // file
    if (resource instanceof IFile)
    {
      try
      {
        IEditorDescriptor descrptr = IDE.getEditorDescriptor((IFile) resource);
        // try to open the associated editor only if its an internal editor
        if (descrptr.isInternal())
        {
          IEditorPart part = IDE.openEditor(getPart().getSite().getPage(),
              (IFile) resource);
          if (part instanceof AbstractDecoratedTextEditor) {
            return (AbstractDecoratedTextEditor) part;
        }

          // editor opened is not a text editor - close it
          getPart().getSite().getPage().closeEditor(part, false);
        }
        // open file in default text editor
        IEditorPart part = IDE.openEditor(getPart().getSite().getPage(),
            (IFile) resource, DEFAULT_TEXT_EDITOR_ID);
        if (part != null && part instanceof AbstractDecoratedTextEditor) {
            return (AbstractDecoratedTextEditor) part;
        }

      } catch (PartInitException e)
      {
      }
    }

    return null;
  }

  private RevisionInformation createRevisionInformation(
      final AnnotateBlocks annotateBlocks, IProgressMonitor monitor)
  {
    Map<String, ChangeSet> logEntriesByRevision = new HashMap<String, ChangeSet>();
    // TODO Get logs for the description...

    RevisionInformation info = new RevisionInformation();

    try
    {
      Class infoClass = info.getClass();
      Class[] paramTypes =
      { IInformationControlCreator.class };
      Method method1 = infoClass
          .getMethod("setHoverControlCreator", paramTypes);
      Method method2 = infoClass.getMethod(
          "setInformationPresenterControlCreator", paramTypes);

      final class AnnotationControlCreator implements
          IInformationControlCreator
      {
        private final String statusFieldText;

        public AnnotationControlCreator(String statusFieldText)
        {
          this.statusFieldText = statusFieldText;
        }

        public IInformationControl createInformationControl(Shell parent)
        {
          return new SourceViewerInformationControl(parent, SWT.TOOL, SWT.NONE,
              JFaceResources.DEFAULT_FONT, statusFieldText);
        }
      }

      method1.invoke(info, new Object[]
      { new AnnotationControlCreator("Press F2 for focus.") });
      method2.invoke(info, new Object[]
      { new AnnotationControlCreator(null) });

    } catch (Exception e)
    {
      // ignore
    }

    final CommitterColors colors = CommitterColors.getDefault();

    HashMap<String, Revision> sets = new HashMap<String, Revision>();

    for (final AnnotateBlock block : annotateBlocks.getAnnotateBlocks())
    {
      final String revisionString = block.getRevision().toString();
      final ChangeSet logEntry = logEntriesByRevision.get(revisionString);

      Revision revision = sets.get(revisionString);
      if (revision == null)
      {
        revision = new Revision()
        {
          @Override
        public Object getHoverInfo()
          {
            return block.getUser()
                + " " + revisionString + " " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(block.getDate()) + "\n\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                (logEntry != null ? logEntry.getDescription() : ""); //$NON-NLS-1$
          }

          @Override
        public String getAuthor()
          {
            return block.getUser();
          }

          @Override
        public String getId()
          {
            return revisionString;
          }

          @Override
        public Date getDate()
          {
            return block.getDate();
          }

          @Override
        public RGB getColor()
          {
            return colors.getCommitterRGB(getAuthor());
          }

        };
        sets.put(revisionString, revision);
        info.addRevision(revision);
      }
      revision.addRange(new LineRange(block.getStartLine(), block.getEndLine()
          - block.getStartLine() + 1));
    }
    return info;
  }
}
