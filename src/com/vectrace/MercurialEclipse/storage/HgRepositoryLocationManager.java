/*******************************************************************************
 * Copyright (c) 2006 Software Balm Consulting Inc.
 * 
 * TODO: LICENSE DETAILS
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/*
 * A manager for all Mercurial repository locations.
 * 
 * TODO: Need a way to delete these repos. Repo Explorer perspective a la subclipse?
 * TODO: Should probably use a text based format for the locations. This is just a crude
 *       object dump right now.
 */
public class HgRepositoryLocationManager
{
  static private SortedSet repos = new TreeSet();
  final static private String REPO_LOCACTION_FILE = "repo_locations.txt";

  /*
   * Return a <code>File</code> object representing the location file.
   * The file may or may not exist and must be checked before use.
   */
  private File getLocationFile()
  {
    return MercurialEclipsePlugin.getDefault().getStateLocation().
             append(REPO_LOCACTION_FILE).toFile();    
  }
  
  /*
   * Load all saved repository locations from the plug-in's default area.
   */
  public void start() throws Exception
  {
    File locationFile = getLocationFile();

    // If the file doesn't exist, then there's nothing to do.
    if( !locationFile.exists() ) return;

    FileInputStream istream = new FileInputStream(locationFile);
    ObjectInputStream p = new ObjectInputStream(istream);

    try
    {
      while(true)
      {
        String url = (String) p.readObject();
        addRepoLocation(new HgRepositoryLocation(url));
      }
    }
    catch(IOException e)
    {
    	MercurialEclipsePlugin.logError(e);
      // Expected exception when there are no more records.
    }
    finally
    {
      p.close();
      istream.close();
    }
  }

  /*
   * Flush all repository locations out to a file in the plug-in's default area.
   */
  public void stop() throws Exception
  {
    // Determine if there's anything that we need to save out to file.
    if(getAllRepoLocations().size() == 0) return;

    File locationFile = getLocationFile();

    // If the file doesn't exist, then create it.
    if( !locationFile.exists() )
    {
      locationFile.createNewFile();
    }

    FileOutputStream ostream = new FileOutputStream(locationFile);
    ObjectOutputStream p = new ObjectOutputStream(ostream);

    // Add any previously existing URLs to the combo box for ease of use.
    Iterator locIter = getAllRepoLocations().iterator();
    while(locIter.hasNext())
    {
      HgRepositoryLocation loc = ((HgRepositoryLocation)locIter.next());
      p.writeObject( loc.getUrl() );
    }

    p.flush();
    ostream.close();
  }

  /*
   * Return an ordered list of all repository locations that are presently known.
   */
  public Set getAllRepoLocations()
  {
    return repos;
  }

  /*
   * Add a repository location to the database.
   */
  public boolean addRepoLocation( HgRepositoryLocation loc )
  {
    return repos.add(loc);
  }
}
