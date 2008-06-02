/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError 
 *     Stefan C                  - Code cleanup
 *******************************************************************************/

/*
 * OpenMercurialRevisionAction
 * 
 * Open an "old" revision in an editor from like "History" view.
 * 
 */
package com.vectrace.MercurialEclipse.actions;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.progress.IProgressService;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;


public class OpenMercurialRevisionAction extends BaseSelectionListenerAction
{

  public class MercurialRevisionEditorInput extends PlatformObject implements IWorkbenchAdapter, IStorageEditorInput 
  {

    private IFileRevision fileRevision;
    private IStorage storage;

    public MercurialRevisionEditorInput(IFileRevision revision) 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::MercurialRevisionEditorInput()");

      this.fileRevision = revision;
      try 
      {
        this.storage = revision.getStorage(new NullProgressMonitor());
      } 
      catch (CoreException e) 
      {
    	  MercurialEclipsePlugin.logError(e);
      }
    }

    public Object[] getChildren(Object o) 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getChildren()");
      return new Object[0];
    }

    public ImageDescriptor getImageDescriptor(Object object) 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getImageDescriptor()");
      return null;
    }

    public String getLabel(Object o) 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getLabel()");
      if (storage != null) 
      {
        return storage.getName();
      }
      return ""; //$NON-NLS-1$
    }

    public Object getParent(Object o) 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getParent()");
      return null;
    }

    public IStorage getStorage() throws CoreException 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getStorage()");
      return storage;
    }

    public boolean exists() 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::exists()");
      return true;
    }

    public ImageDescriptor getImageDescriptor() 
    {
      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getImageDescriptor()");
      return null;
    }

    public String getName() 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getName()");
      String ret="";//$NON-NLS-1$
      if (fileRevision != null)
      {
        ret = fileRevision.getName() + " " + fileRevision.getContentIdentifier();  
      }
      else if (storage != null) 
      {
        ret = storage.getName() + " " + DateFormat.getInstance().format(new Date(((IFileState) storage).getModificationTime())); 
      }
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getName() return:" + ret);
      return ret; 
    }

    public IPersistableElement getPersistable() 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getPersistable()");
      return null;  //Can to save editor changes
    }

    public String getToolTipText() 
    {
//      System.out.println("OpenMercurialRevisionAction::MercurialRevisionEditorInput::getToolTipText()");
      if (fileRevision != null)
      {
        try 
        {
          return getStorage().getFullPath().toString();
        } 
        catch (CoreException e) 
        {
        	MercurialEclipsePlugin.logError(e);
        }
      }

      if (storage != null)
      {
        return storage.getFullPath().toString();
      }

      return ""; 
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class adapter) 
    {
      if (adapter == IWorkbenchAdapter.class) 
      {
        return this;
      }
      if (adapter == IFileRevision.class)
      {
        return fileRevision;
      }
      else if  (adapter == IFileState.class)
      {
        if (storage != null && storage instanceof IFileState)
        {
          return storage;
        }
      } 
      return super.getAdapter(adapter);
    }

  }

  
  
  private IStructuredSelection selection;
  private HistoryPage page;
  
  public OpenMercurialRevisionAction(String text)
  {
    super(text);
//    System.out.println("OpenMercurialRevisionAction::OpenMercurialRevisionAction(" + text + ")");
  }

  @Override
public void run() 
  {
//    System.out.println("OpenMercurialRevisionAction::run()");

    IStructuredSelection structSel = selection;

    Object[] objArray = structSel.toArray();

    for (int i = 0; i < objArray.length; i++) 
    {
      Object tempRevision = objArray[i];

      final IFileRevision revision = (IFileRevision) tempRevision;
      if (revision == null || !revision.exists()) 
      {
        MessageDialog.openError(page.getSite().getShell(), "Deleted Revision", "Can't open a deleted revision");
      } 
      else 
      {
        IRunnableWithProgress runnable = new IRunnableWithProgress() 
          {
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
              IStorage file;
              try 
              {
                file = revision.getStorage(monitor);
                String id = getEditorID(file.getName(), file.getContents());
  
                if (file instanceof IFile) 
                {
//                  System.out.println("OpenMercurialRevisionAction::run() is IFile");

                  //if this is the current workspace file, open it
                  IDE.openEditor(page.getSite().getPage(), (IFile) file);
                } 
                else 
                {
//                  System.out.println("OpenMercurialRevisionAction::run() is NOT IFile");
                  MercurialRevisionEditorInput fileRevEditorInput = new MercurialRevisionEditorInput(revision);
                  if (!editorAlreadyOpenOnContents(fileRevEditorInput))
                  {
//                    System.out.println("OpenMercurialRevisionAction::run() !editorAlreadyOpenOnContents(fileRevEditorInput)");
                    page.getSite().getPage().openEditor(fileRevEditorInput, id);
                  }
                }
              } 
              catch (CoreException e) 
              {
                throw new InvocationTargetException(e);
              }
  
            }
          };
        IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
        try 
        {
          progressService.run(true, false, runnable);
        } 
        catch (InvocationTargetException e) 
        {
        } 
        catch (InterruptedException e) 
        {
        }
      }

    }
  }

  String getEditorID(String fileName, InputStream contents) 
  {
//    System.out.println("OpenMercurialRevisionAction::getEditorID()");
    IWorkbench workbench = PlatformUI.getWorkbench();
    IEditorRegistry registry = workbench.getEditorRegistry();
    IContentType type = null;
    if (contents != null) 
    {
      try 
      {
        type = Platform.getContentTypeManager().findContentTypeFor(contents, fileName);
      } 
      catch (IOException e) 
      {
    	  MercurialEclipsePlugin.logError(e);
      }
    }
    if (type == null) 
    {
      type = Platform.getContentTypeManager().findContentTypeFor(fileName);
    }
    IEditorDescriptor descriptor = registry.getDefaultEditor(fileName, type);
    String id;
    if (descriptor == null || descriptor.isOpenExternal()) 
    {
      id = "org.eclipse.ui.DefaultTextEditor"; 
    } 
    else 
    {
      id = descriptor.getId();
    }

    return id;
  }

  @Override
protected boolean updateSelection(IStructuredSelection selection1) 
  {
//    System.out.println("OpenMercurialRevisionAction::updateSelection()");
    this.selection = selection1;
    return shouldShow();
  }

  public void setPage(HistoryPage page) 
  {
//    System.out.println("OpenMercurialRevisionAction::setPage()");
    this.page = page;
  }

  private boolean shouldShow() 
  {
//    System.out.println("OpenMercurialRevisionAction::shouldShow()");
    IStructuredSelection structSel = selection;
    Object[] objArray = structSel.toArray();

    if (objArray.length == 0) {
        return false;
    }

    for (int i = 0; i < objArray.length; i++) 
    {
      IFileRevision revision = (IFileRevision) objArray[i];
      //check to see if any of the selected revisions are deleted revisions
      if (revision != null && !revision.exists()) {
        return false;
    }
    }

    return true;
  }

  private boolean editorAlreadyOpenOnContents(MercurialRevisionEditorInput input) 
  {
//    System.out.println("OpenMercurialRevisionAction::editorAlreadyOpenOnContents()");
    IEditorReference[] editorRefs = page.getSite().getPage().getEditorReferences();
    for (int i = 0; i < editorRefs.length; i++) {
      IEditorPart part = editorRefs[i].getEditor(false);
      if (part != null && part.getEditorInput() instanceof MercurialRevisionEditorInput) {
        IFileRevision inputRevision = (IFileRevision) input.getAdapter(IFileRevision.class);
        IFileRevision editorRevision = (IFileRevision) part.getEditorInput().getAdapter(IFileRevision.class);

        if (inputRevision.equals(editorRevision)) 
        {
          //make the editor that already contains the revision current
          page.getSite().getPage().activate(part);
          return true;
        }
      }
    }
    return false;
  }

}
