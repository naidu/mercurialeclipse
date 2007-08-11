package com.vectrace.MercurialEclipse.model;

import java.util.Vector;

public class ChangeLog
{
  private Vector<ChangeSet> changeLog=new Vector<ChangeSet>();
  
  public ChangeLog(String changeLog)
  {
    super();
    
    this.changeLog.add( new ChangeSet("82:03d5f8754","zingo","20070616","filen2.c","Muhahahah"));
    this.changeLog.add( new ChangeSet("81:1d34fgsd8","zingo","20070506","file_pilen.c filen2.c","Some changes are made to the files"));
    this.changeLog.add( new ChangeSet("80:1d34fgsd8","zingo","20070426","file_pilen.c filen2.c","Some changes are made to the files"));
        
  }
  
  public Vector<ChangeSet> getChangeLog()
  {
    return changeLog;
  }
  
  
}
