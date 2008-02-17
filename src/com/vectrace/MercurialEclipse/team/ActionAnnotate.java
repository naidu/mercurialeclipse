/*******************************************************************************
 * Copyright (c) 2008 Vectrace (Zingo Andersen) 
 * 
 * This software is licensed under the zlib/libpng license.
 * 
 * This software is provided 'as-is', without any express or implied warranty. 
 * In no event will the authors be held liable for any damages arising from the
 * use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it freely,
 * subject to the following restrictions:
 *
 *  1. The origin of this software must not be misrepresented; you must not 
 *            claim that you wrote the original software. If you use this 
 *            software in a product, an acknowledgment in the product 
 *            documentation would be appreciated but is not required.
 *
 *   2. Altered source versions must be plainly marked as such, and must not be
 *            misrepresented as being the original software.
 *
 *   3. This notice may not be removed or altered from any source distribution.
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.actions.ActionDelegate;

import com.vectrace.MercurialEclipse.HgFile;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.annotations.ShowAnnotationOperation;

/**
 * @author zingo
 * 
 */
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
  public void dispose()
  {

  }

  /**
   * The action has been activated. The argument of the method represents the
   * 'real' action sitting in the workbench UI.
   * 
   * @see IWorkbenchWindowActionDelegate#run
   */

  public void run(IAction action)
  {
    for (Object obj : selection.toList())
    {
      if (!(obj instanceof IFile))
        continue;
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
