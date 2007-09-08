package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;

public class MercurialFileHistoryVariant implements IResourceVariant {
  private final IStorage myIStorage;

  public MercurialFileHistoryVariant(IStorage res) 
  {
   this.myIStorage = res;
//   System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::FileHistoryVariant()" );
  }

  
  public String getName() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getName()" );
    return myIStorage.getName();
  }

  public boolean isContainer() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::isContainer()" );
    return false;
  }

  public IStorage getStorage(IProgressMonitor monitor) throws TeamException 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getStorage()" );
    return myIStorage;
  }

  public String getContentIdentifier() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::getContentIdentifier()" );
    return myIStorage.getFullPath().toString();
  }

  public byte[] asBytes() 
  {
//    System.out.println("FileHistoryVariant(" + myIStorage.getName() +")::asBytes() what is this for?" );
    return null;
  }
}  
