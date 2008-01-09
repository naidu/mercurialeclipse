/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
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
package com.vectrace.MercurialEclipse.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

/**
 * @author Peter Hunnisett <peter_hge at softwarebalm dot com>
 *
 */
public abstract class HgOperation extends TeamOperation {

	private String result;

	/**
	 * @param part
	 */
	public HgOperation(IWorkbenchPart part) {
		super(part);
	}

	/**
	 * @param context
	 */
	public HgOperation(IRunnableContext context) {
		super(context);
	}

	/**
	 * @param part
	 * @param context
	 */
	public HgOperation(IWorkbenchPart part, IRunnableContext context) {
		super(part, context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,InterruptedException 
	{
	    // TODO: Would be nice to have something that indicates progress
	    //       but that would require that functionality from the utilities.
	    monitor.beginTask(getActionDescription(), 1);

	    try
	    {
	      result = MercurialUtilities.ExecuteCommand(getHgCommand(),getHgWorkingDir(), true);
	    } 
	    catch (HgException e)
	    {
	      System.out.println(getActionDescription() + " failed: " + e.getMessage());
	    }
	    finally
	    {
	      monitor.done();
	    }
	}
	
	protected String[] getHgCommand()
	{
		return null;
	}
	
  protected File getHgWorkingDir()
  {
    return null;
  }
	  
    public String getResult()
    {
    	return result;
    }
	
	// TODO: No background for now.
	protected boolean canRunAsJob()
	{
		return false;
	}

	protected String getJobName()
	{
		return getActionDescription();
	}
	
	abstract protected String getActionDescription();
}
