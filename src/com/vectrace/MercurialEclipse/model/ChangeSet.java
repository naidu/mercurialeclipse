package com.vectrace.MercurialEclipse.model;

public class ChangeSet
{
  private String changeset;
  private String user;
  private String date;
  private String files;
  private String description;
  
  public ChangeSet(String changeSet,String user, String date, String files, String description)
  {
    this.changeset=changeSet;
    this.user = user;
    this.date = date;
    this.files = files;
    this.description = description;
  }

  public String getChangeset()
  {
    return changeset;
  }

  public String getUser()
  {
    return user;
  }

  public String getDate()
  {
    return date;
  }

  public String getFiles()
  {
    return files;
  }

  public String getDescription()
  {
    return description;
  }
}
