/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     Stefan C                  - Code cleanup
 *******************************************************************************/

package com.vectrace.MercurialEclipse.storage;

/*
 * A class abstracting a Mercurial repository location which may be either local or remote.
 */
public class HgRepositoryLocation implements Comparable
{
  private String location;
  
  public HgRepositoryLocation(String url)
  {
    this.location = url;
  }
  
  public String getUrl()
  {
    return this.location;
  }
  
  static public boolean validateLocation(String validate)
  {
    return validate.trim().length() > 0;
/* TODO: Something like this would be nice, but it doesn't understand ssh and allows several
 *       other protocols.   
    try
    {
      URL url = new URL(validate);
    }
    catch(MalformedURLException e)
    {
      return false;
    }
    return true;
    */    
  }

  public int compareTo(Object arg0)
  {
    return this.location.compareTo(((HgRepositoryLocation)arg0).location);
  }

 
}
