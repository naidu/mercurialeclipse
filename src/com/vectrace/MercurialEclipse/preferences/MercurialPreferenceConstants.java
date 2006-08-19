package com.vectrace.MercurialEclipse.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public class MercurialPreferenceConstants
{
  public static final String MERCURIAL_EXECUTABLE = "hg";
  //username should be per project in the future, different repositories could have differernt names (sub optimal I know but it really could)
  public static final String MERCURIAL_USERNAME = System.getProperty ( "user.name" ) ;
}


