/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan Groschupf          - logError
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

import sun.security.jgss.spi.MechanismFactory;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;

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
	    	MercurialEclipsePlugin.logError(getActionDescription() + " failed:" , e);
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
