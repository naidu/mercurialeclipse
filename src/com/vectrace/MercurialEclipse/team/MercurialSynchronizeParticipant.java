/**
 * com.vectrace.MercurialEclipse (c) Vectrace 2006-aug-31
 * Created by zingo
 */
package com.vectrace.MercurialEclipse.team;

import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.IMemento;

/**
 * @author zingo
 *
 */
public class MercurialSynchronizeParticipant extends SubscriberParticipant
{

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#initializeConfiguration(org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
   */
  protected void initializeConfiguration(ISynchronizePageConfiguration configuration)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialSynchronizeParticipant::initializeConfiguration()" );
    super.initializeConfiguration(configuration);
  }

  /* (non-Javadoc)
   * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#saveState(org.eclipse.ui.IMemento)
   */
  public void saveState(IMemento memento)
  {
    // TODO Auto-generated method stub
//    System.out.println("MercurialSynchronizeParticipant::saveState()" );
    super.saveState(memento);
  }

 //   System.out.println("MercurialSynchronizeParticipant::dispose()" );

  
  

}
