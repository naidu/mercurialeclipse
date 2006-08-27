/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2006-aug-27
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

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
  String sourceFilename;
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
    sourceFilename = ( res.getLocation() ).toString();
  }

  public IStorageMercurialRevision(IProject proj,IResource res, int rev)
  {
    super();
    project = proj;
    resource = res;
    revision=String.valueOf(rev);   
    sourceFilename = ( res.getLocation() ).toString();
  }
  
  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter)
  {
//    System.out.println("IStorageMercurialRevision(" + sourceFilename + "," + revision + ")::getAdapter()" );
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
    //  Should generate data content of the so called "file" in this case a revision, e.g. a hg cat --rev "rev" <file>
    String Repository;
//    System.out.println("IStorageMercurialRevision(" + sourceFilename + "," + revision + ")::getContent()" );
    Repository=MercurialUtilities.getRepositoryPath(project);
    if(Repository==null)
    {
      Repository="."; //never leave this empty add a . to point to current path
    }  

    String launchCmd[] = { MercurialUtilities.getHGExecutable(),"--cwd", Repository ,"cat", "--rev" , revision, sourceFilename };
    return MercurialUtilities.ExecuteCommandToInputStream(launchCmd);
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#getFullPath()
   */
  public IPath getFullPath()
  {
//    System.out.println("IStorageMercurialRevision(" + sourceFilename + "," + revision + ")::getFullPath()" );
    return resource.getFullPath().append(revision);
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.resources.IStorage#getName()
   */
  public String getName()
  {
//    System.out.println("IStorageMercurialRevision(" + sourceFilename + "," + revision + ")::getName()" );
    String name;
    name = resource.getName() + "_rev_" + revision;
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
//    System.out.println("IStorageMercurialRevision(" + sourceFilename + "," + revision + ")::isReadOnly()" );
    return true;
  }

  
}
