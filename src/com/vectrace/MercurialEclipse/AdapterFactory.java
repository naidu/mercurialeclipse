/**
 * 
 */
package com.vectrace.MercurialEclipse;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.history.IHistoryPageSource;

import com.vectrace.MercurialEclipse.history.MercurialHistoryProvider;
import com.vectrace.MercurialEclipse.history.MercurialHistoryPageSource;
/**
 * @author zingo
 *
 */
public class AdapterFactory implements IAdapterFactory
{

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
   */
  public Object getAdapter(Object adaptableObject, Class adapterType)
  {
    // TODO Auto-generated method stub
//    System.out.println("AdapterFactory::getAdapter()");
    
    if((adaptableObject instanceof MercurialHistoryProvider) && adapterType == IHistoryPageSource.class) 
    {
//      System.out.println("AdapterFactory::getAdapter() MercurialHistoryPageSource");
      return new MercurialHistoryPageSource((MercurialHistoryProvider)adaptableObject);
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
   */
  public Class[] getAdapterList()
  {
    // TODO Auto-generated method stub
//    System.out.println("AdapterFactory::getAdapterList()");
    return new Class[] {IHistoryPageSource.class};
  }

}
