/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2006-aug-27
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * @author zingo
 *
 * This is a IStorage subclass that handle file revision
 *  
 */
public class IStorageMercurialRevision implements IStorage
{
//  String sourceFilename;
  String revision;
  IProject project;
  IResource resource;

  /**
   * 
   */
  
  public IStorageMercurialRevision(IProject proj,IResource res, String rev)
  {
    super();
    project = proj;
    resource = res;
    revision=rev;
//    sourceFilename = ( res.getLocation() ).toString();
  }

  public IStorageMercurialRevision(IProject proj,IResource res, int rev)
  {
    super();
    project = proj;
    resource = res;
    revision=String.valueOf(rev);   
//    sourceFilename = ( res.getLocation() ).toString();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter)
  {
//    System.out.println("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::getAdapter()" );
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#getContents()
   * 
   * generate data content of the so called "file" in this case a revision,
   * e.g. a hg cat --rev "rev" <file>
   * 
   */
  public InputStream getContents() throws CoreException
  {
//    System.out.println("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::getContent()" );
    
    //  Should generate data content of the so called "file" in this case a revision, e.g. a hg cat --rev "rev" <file>
//    String Repository;
//    Repository=MercurialUtilities.getRepositoryPath(project);
//    if(Repository==null)
//    {
//      Repository="."; //never leave this empty add a . to point to current path
//    }  
//    System.out.println("IStorageMercurialRevision::getContents() Repository=" + Repository);

    //Setup and run command
    String rev=revision;
    /*convert <rev number>:<hash> to <hash>*/
    int separator=rev.indexOf(':');
    if(separator!=-1)
    {
      rev=rev.substring(separator+1);
    }
    String launchCmd[] = { MercurialUtilities.getHGExecutable(),
                           "cat", 
                           "--rev", 
                           rev,
                           MercurialUtilities.getResourceName(resource) 
                           };
    File workingDir=MercurialUtilities.getWorkingDir(resource);
    
    
    //    return MercurialUtilities.ExecuteCommandToInputStream(launchCmd,false);
    return MercurialUtilities.ExecuteCommandToInputStream(launchCmd,workingDir,true);
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#getFullPath()
   */
  public IPath getFullPath()
  {
//    System.out.println("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::getFullPath()" );
    return resource.getFullPath().append(revision);
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#getName()
   */
  public String getName()
  {
//    System.out.print("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::getName()" );
    String name;
    name = "[" + revision + "]" + resource.getName();
//    System.out.println("=" + name );
    
    return  name;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#isReadOnly()
   * 
   * You can't write to old revisions e.g. ReadOnly
   * 
   */
  public boolean isReadOnly()
  {
//    System.out.println("IStorageMercurialRevision(" + resource.toString() + "," + revision + ")::isReadOnly()" );
    return true;
  }

  
}
