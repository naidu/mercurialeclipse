# Update site maintenance #

  1. Import the wiki as projects into Eclipse workspace (use the repo url below)
  1. Open site.xml in Eclipse Update Site editor
  1. In the code view, "feature" entry, change "version" attribute to "1.7.1.qualifier" (it will be replaced by the automatically generated feature version)
  1. Select existing MercurialEclipse feature entry in the left pane
  1. Click on "Build" button
  1. Check the /plugins and /features folder: there are new files now
  1. Upload generated com.vectrace.MercurialEclipse\_x.y.z.jar from /plugins folder to project download section
  1. Update the "archive" section in the site.xml with the proper jar file version (2 places, should match generated plugin version)
  1. Commit all changed and newly added files except plugin jar
  1. Done!

## Updating snapshots on javaforge too ##

After all steps above, you can update javaforge snapshots update site with few clicks, if you is a member of MercurialEclipse project on javaforge.
  1. Login at http://www.javaforge.com/project/HGE
  1. Go here: http://www.javaforge.com/proj/doc.do?doc_id=78660
  1. Click on "features" node
  1. Click on a "New File" button at the left upper corner
  1. Select the mercurialeclipse\_x.y.z.jar and click on "upload"
  1. Go back to the root directory - click on "mercurialeclipse-snapshots" node
  1. hover over a small triangle after the site.xml node and select "new version"
  1. Select site.xml from local snapshots folder and click on "upload"
  1. Done!

This are the update site urls for Eclipse update manager:

Stable releases:
http://mercurialeclipse.eclipselabs.org.codespot.com/hg.wiki/update_site/stable

Snapshots:
http://mercurialeclipse.eclipselabs.org.codespot.com/hg.wiki/update_site/snapshots

And this is the wiki hg repo:
http://code.google.com/a/eclipselabs.org/p/mercurialeclipse/source/checkout?repo=wiki