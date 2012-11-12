
# Developer setup

MercurialEclipse uses Maven. MercurialEclipse depends on JavaHg and extensions for JavaHg.

1. Install maven
   * Install the command line tool
   * Install the Maven Eclipse plugin - m2eclipse 

2. Get the source code
   * Clone MercurialEclipse into your workspace as two projects - one for /feature and one for /plugin 
   * Clone JavaHg projects (recommended)
     * https://bitbucket.org/aragost/javahg 
     * https://bitbucket.org/aragost/javahg-ext-mq
     * https://bitbucket.org/aragost/javahg-ext-rebase
     * https://bitbucket.org/aragost/javahg-parent

3. Set up the projects for build (Using m2eclipse)
   * In your run configuration dropdown there will be a 'MercurialEclipse-MavenInit' launch - select the 
     plugin project in the navigator view and then run this launch.
   * This will download dependencies and generate .classpath and other files
   * If there are Java errors related to Mylyn install the Mylyn plugin into your Eclipse
   * For each of the JavaHg projects invoke the maven 'eclipse:eclipse' goal. This sets them up so Eclipse 
     will build them  (like MercurialEclipse-MavenInit does).
     * Create a new 'Maven Build' run configuration for that project with 'eclipse:eclipse' as the Goal.
     * Refresh the projects after running this 
   * So that MercurialEclipse uses the JavaHg code from your workspace use the Maven 'install' goal. 
     * Create and invoke 'Maven build' launches with 'install' goal
     * Re-run MercurialEclipse-MavenInit launch

# Building releases
  
1. Update to the appropriate tag
2. Re-run the `MercurialEclipse-MavenInit` launch. 
   * If the build fails due to missing dependent versions (if the version is not uploaded to the central 
     maven repository) then do the following.
     * Update to the appropriate tag of the missing javahg dependency
     * Invoke the install maven task
     * Re-run the `MercurialEclipse-MavenInit` launch.
