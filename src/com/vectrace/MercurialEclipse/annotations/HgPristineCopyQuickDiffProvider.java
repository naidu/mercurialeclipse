/*******************************************************************************
 * Copyright (c) ?
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     StefanC           - jobs framework
 *******************************************************************************/
package com.vectrace.MercurialEclipse.annotations;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.team.core.TeamException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.quickdiff.IQuickDiffReferenceProvider;

import com.vectrace.MercurialEclipse.HgFile;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * 
 * A QuickDiff provider that provides a reference to the pristine copy of a file
 * managed in the Hg repository. The provider notifies when the file's sync
 * state changes and the diff should be recalculated (e.g. commit, update...) or
 * when the file is changed (e.g. replace with).
 * 
 * Here are the file states and what this provider does for each:
 * 
 * 1. File is unmanaged : reference == empty document 2. Unmanaged file
 * transitions to managed : empty reference updated with new remote revision 3.
 * A managed file has new remote (commit, refresh remote) : reference updated
 * with new remote revision 4. A managed file cleaned, remote is the same
 * (replace with, update) : refresh diff bar with existing reference
 * 
 * [Note: Currently an empty document must be returned for an unmanaged file.
 * This results in the entire document appearing as outgoing changes in the
 * quickdiff bar. This is required because the quickdiff support relies on
 * IDocument change events to update the quickdiff, and returning null for the
 * reference document doesn't allow the transition to later return a IDocument.]
 * 
 * @since 3.0
 */
public class HgPristineCopyQuickDiffProvider implements
    IQuickDiffReferenceProvider
{

  public static final String HG_REFERENCE_PROVIDER = "com.vectrace.MercurialEclipse.annotatations.HgReferenceProvider";
  
  // The editor showing this quickdiff and provides access to the editor input
  // and
  // ultimatly the IFile.
  private ITextEditor editor = null;

  // The document containing the remote file. Can be null if the assigned editor
  // doesn't have
  // a Hg remote resource associated with it.
  private IDocument referenceDocument = null;

  // Will be true when the document has been read and initialized.
  private boolean isReferenceInitialized = false;

  // Document provider allows us to register/deregister the element state change
  // listener.
  private IDocumentProvider documentProvider = null;

  // Unique id for this reference provider as set via setId().
  private String id;

  // Job that re-creates the reference document.
  private Job fUpdateJob;

  /**
   * Updates the document if the document is changed (e.g. replace with)
   */
  private IElementStateListener documentListener = new IElementStateListener()
  {
    public void elementDirtyStateChanged(Object element, boolean isDirty)
    {
    }

    public void elementContentAboutToBeReplaced(Object element)
    {
    }

    public void elementContentReplaced(Object element)
    {
      if (editor != null && editor.getEditorInput() == element)
      {
        fetchContentsInJob();
      }
    }

    public void elementDeleted(Object element)
    {
    }

    public void elementMoved(Object originalElement, Object movedElement)
    {
    }
  };



  /*
   * @see org.eclipse.test.quickdiff.DocumentLineDiffer.IQuickDifreferenceDocumentProvider#getReference()
   */
  public IDocument getReference(IProgressMonitor monitor) throws CoreException
  {
    if (!isReferenceInitialized) {
        return null;
    }
    if (referenceDocument == null)
    {
      readDocument(monitor);
    }
    return referenceDocument;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation#setActiveEditor(org.eclipse.ui.texteditor.ITextEditor)
   */
  public void setActiveEditor(ITextEditor targetEditor)
  {
    if (!(targetEditor.getEditorInput() instanceof IFileEditorInput)) {
        return;
    }
    editor = targetEditor;
    documentProvider = editor.getDocumentProvider();

    if (documentProvider != null)
    {
      (documentProvider)
          .addElementStateListener(documentListener);
    }
    isReferenceInitialized = true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.quickdiff.IQuickDiffProviderImplementation#isEnabled()
   */
  public boolean isEnabled()
  {
    if (!isReferenceInitialized) {
        return false;
    }
    return getManagedHgFile() != null;
  }

  /*
   * @see org.eclipse.jface.text.source.diff.DocumentLineDiffer.IQuickDifreferenceDocumentProvider#dispose()
   */
  public void dispose()
  {
    isReferenceInitialized = false;
    // stop update job
    if (fUpdateJob != null && fUpdateJob.getState() != Job.NONE)
    {
      fUpdateJob.cancel();
    }

    // remove listeners
    if (documentProvider != null)
    {
      documentProvider.removeElementStateListener(documentListener);
    }
  }

  /*
   * @see org.eclipse.quickdiff.QuickDiffTestPlugin.IQuickDiffProviderImplementation#setId(java.lang.String)
   */
  public void setId(String id)
  {
    this.id = id;
  }

  /*
   * @see org.eclipse.jface.text.source.diff.DocumentLineDiffer.IQuickDifreferenceDocumentProvider#getId()
   */
  public String getId()
  {
    return id;
  }

  /**
   * Determine if the file represented by this quickdiff provider has changed
   * with respect to it's remote state. Return true if the remote contents
   * should be refreshed, and false if not.
   */
  private boolean computeChange(IProgressMonitor monitor) throws TeamException
  {
    boolean needToUpdateReferenceDocument = false;
    if (isReferenceInitialized)
    {
      needToUpdateReferenceDocument = getFileFromEditor().isSynchronized(
          IResource.DEPTH_ONE);
    }
    return needToUpdateReferenceDocument;
  }

  /**
   * Creates a document and initializes it with the contents of a Hg file
   * resource.
   * 
   * @param monitor
   *          the progress monitor
   * @throws CoreException
   */
  private void readDocument(IProgressMonitor monitor) throws CoreException
  {
    if (!isReferenceInitialized) {
        return;
    }
    if (referenceDocument == null) {
        referenceDocument = new Document();
    }
    if (computeChange(monitor))
    {
      IFile remoteFile = getFileFromEditor();
      if (remoteFile != null
          && documentProvider instanceof IStorageDocumentProvider)
      {
        IStorageDocumentProvider provider = (IStorageDocumentProvider) documentProvider;
        String encoding = provider.getEncoding(editor.getEditorInput());
        if (encoding == null)
        {
          encoding = provider.getDefaultEncoding();
        }
        if (monitor.isCanceled()) {
            return;
        }
        InputStream stream = remoteFile.getContents();
        if (stream == null || monitor.isCanceled() || !isReferenceInitialized)
        {
          return;
        }
        setDocumentContent(referenceDocument, stream, encoding);
      } else
      {
        // the remote is null, so ensure that the document is null
        if (monitor.isCanceled()) {
            return;
        }
        referenceDocument.set(""); //$NON-NLS-1$
      }
    }
  }

  /**
   * Intitializes the given document with the given stream using the given
   * encoding.
   * 
   * @param document
   *          the document to be initialized
   * @param contentStream
   *          the stream which delivers the document content
   * @param encoding
   *          the character encoding for reading the given stream
   * @exception CoreException
   *              if the given stream can not be read
   */
  private static void setDocumentContent(IDocument document,
      InputStream contentStream, String encoding) throws CoreException
  {
    Reader in = null;
    try
    {
      final int DEFAULT_FILE_SIZE = 15 * 1024;

      in = new BufferedReader(new InputStreamReader(contentStream, encoding),
          DEFAULT_FILE_SIZE);
      CharArrayWriter caw = new CharArrayWriter(DEFAULT_FILE_SIZE);
      char[] readBuffer = new char[2048];
      int n = in.read(readBuffer);
      while (n > 0)
      {
        caw.write(readBuffer, 0, n);
        n = in.read(readBuffer);
      }
      document.set(caw.toString());
    } catch (IOException x)
    {
      throw new HgException("RemoteRevisionQuickDiffProvider.readingFile", x); //$NON-NLS-1$
    } finally
    {
      if (in != null)
      {
        try
        {
          in.close();
        } catch (IOException x)
        {
          throw new HgException(
              "RemoteRevisionQuickDiffProvider.closingFile", x); //$NON-NLS-1$
        }
      }
    }
  }

  /**
   * Returns the HgFile associated with he active editor or <code>null</code>
   * if the provider doesn't not have access to a Hg managed file.
   * 
   * @return the handle to a Hg file
   */
  private HgFile getManagedHgFile()
  {
    if (editor != null)
    {
      IFile file = getFileFromEditor();
      if (file != null)
      {
        HgFile hgFile = new HgFile(file);
        if (hgFile.exists())
        {
          return hgFile;
        }
        return null;
      }
    }
    return null;
  }

  private IFile getFileFromEditor()
  {
    if (editor != null)
    {
      IEditorInput input = editor.getEditorInput();
      if (input instanceof IFileEditorInput)
      {
        return ((IFileEditorInput) input).getFile();
      }
    }
    return null;
  }

  /**
   * Runs a job that updates the document. If a previous job is already running
   * it is stopped before the new job can start.
   */
  private void fetchContentsInJob()
  {
    if (!isReferenceInitialized) {
        return;
    }
    if (fUpdateJob != null && fUpdateJob.getState() != Job.NONE)
    {
      fUpdateJob.cancel();
    }
    fUpdateJob = new Job("RemoteRevisionQuickDiffProvider.fetchingFile") { //$NON-NLS-1$
      @Override
    protected IStatus run(IProgressMonitor monitor)
      {
        try
        {
          readDocument(monitor);
        } catch (CoreException e)
        {
          // continue and return ok for now. The error will be reported
          // when the quick diff supports calls getReference() again.
          // continue
        }
        return Status.OK_STATUS;
      }
    };
    fUpdateJob.schedule();
  }
}
