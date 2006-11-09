/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * TODO: LICENSE DETAILS
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
  
  private HgRepositoryLocation()
  {
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
