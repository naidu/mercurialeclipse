/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     Stefan Groschupf          - logError
 *     Charles O'Farrell         - Annotation with color
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.actions.ActionDelegate;

import com.vectrace.MercurialEclipse.HgFile;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.annotations.ShowAnnotationOperation;

public class ActionAnnotate extends ActionDelegate implements
    IObjectActionDelegate, IViewActionDelegate
{
  private IStructuredSelection selection;
  private IWorkbenchPart part;

  public ActionAnnotate()
  {
    super();
  }

  /**
   * We can use this method to dispose of any system resources we previously
   * allocated.
   * 
   * @see IWorkbenchWindowActionDelegate#dispose
   */
  @Override
public void dispose()
  {

  }

  /**
   * The action has been activated. The argument of the method represents the
   * 'real' action sitting in the workbench UI.
   * 
   * @see IWorkbenchWindowActionDelegate#run
   */

  @Override
public void run(IAction action)
  {
    for (Object obj : selection.toList())
    {
      if (!(obj instanceof IFile)) {
        continue;
    }
      try
      {
        new ShowAnnotationOperation(part, new HgFile((IFile) obj)).run();
      } catch (Exception e)
      {
        MercurialEclipsePlugin.logError(e);
      }
    }
  }

  /**
   * Selection in the workbench has been changed. We can change the state of the
   * 'real' action here if we want, but this can only happen after the delegate
   * has been created.
   * 
   * @see IWorkbenchWindowActionDelegate#selectionChanged
   */
  @Override
public void selectionChanged(IAction action, ISelection in_selection)
  {
    if (in_selection != null && in_selection instanceof IStructuredSelection)
    {
      selection = (IStructuredSelection) in_selection;
    }
  }

  public void setActivePart(IAction action, IWorkbenchPart part)
  {
    this.part = part;

  }

  public void init(IViewPart part)
  {
    this.part = part;
  }

}
