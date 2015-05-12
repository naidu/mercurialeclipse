# Latest Release: 1.9.4 #

  * #49592 - When trying to show histroy an invalid cast occurs with Eclipse 3.8
  * #50098: MergeView - allow selecting multiple files to mark resolved/unresolved
  * #35888: Fix NPE in synchronize view
  * #13599: Merge using Eclipse diff tool does not perform any automatic merging on simple conflicts

# Previous releases #

## MercurialEclipse 1.9.3 ##

  * More explicit prompt when doing an update which might lose data
  * Allow binding a key to "Show root history" action

## MercurialEclipse 1.9.2 ##

  * BUG-20757 - internal error: does not match outer scope rule: HgRootRule
  * BUG-20854 - ME does not allow to track symbolic links
  * BUG-20897 - Cannot delete a project
  * BUG-26819 - Potential for losing changes switching revisions because warning message is not clear
  * BUG-34088 - Key binding for ''Show Root History'' does nothing

### Full list ###

  * [Changes](http://javaforge.com/proj/cmdb/releaseNotes.spr?task_id=20758)

## MercurialEclipse 1.9.1 ##

### Features ###
  * Better shelve and unshelve usability
  * Better conflict handling for import, unshelve, qpush, qfold

### Bug fixes ###

  * Shelve/Unshelve problem - can not merge shelved files- when the shelved files  has conflict with the recent changes
  * Clone wizard should show specific error message or display full error output
  * Eclipse hangs with "Refresh status...: (0%)" after MercurialEclipse operations changing the working copy
  * Annotate only showing first letter of name (or email) if  ' name &lt;email> ' user name format is used.
  * Errors after upgrading to 1.9
  * Error after pull and update: Cannot populate patch view table
  * Error updating
  * Clone wizard invalid password error not shown
  * Deadlock at startup if empty default username in preferences
  * Cat command does not handle file encodings correctly
  * GPG: Couldn't load keys. See log for details.

### Full list ###

  * [Changes](http://www.javaforge.com/proj/cmdb/releaseNotes.spr?task_id=20056)

## MercurialEclipse 1.9.0 ##

### Feature Requests ###
  * **Need named (and multiple) und uncommited change-sets**
  * Support for linked resources
  * **Patch Queue View enhancements**
    * mq: for qrefresh and qnew allow selecting files to use
    * Reflect changes to mercurial queues (MQ) in GUI
    * Various
  * **Compare two revisions for project-tree.**
  * Synv view: Better indication when incoming or outgoing filters are selected
  * Expose some key bindings
  * Merge view usability improvements
  * Request for distribution of feature
  * Key shortcut handlers should operate on selection rather than currently open file if appropriate
  * The pull window should remember the previously used settings

### Bug Fixes ###
  * Is it necessary to show the "Mercurial Merge" view even if there are no conflicts?
  * No diff is available in the sync view after renaming
  * Multiple eclipse projects in one repository: .hgignore changes being ignored
  * qnew user name seems not use per-repository hgrc settings
  * transaction abort! due to unknown encoding: x-windows-949
  * Mercurial Merge View - Merge Editor throw an exception on unresolved files
  * "Import uncommitted changes into patch" for qnew and qrefresh does not work
  * Amend current commit will put repo in bad state if the commit fails (eg because of inconsistent line endings)
  * Amend commit with locally modified files that are also in the amended changeset results in the modification being part of the change even if the files are not selected to be committed
  * Mercurial user names are truncated at the first dot '.'
  * qdelete -r not working as it should from GUI
  * "Clone existing repository" leads to a dead-end wizard pane.
  * Amending a commit: Commit message is not properly escaped
  * Password not being saved for Repository
  * Automatic merge/rebase dialog not appearing
  * Compile errors in latest 1.9.0 head against Eclipse 3.6.+ target platform
  * Various error messages when Flash Builder starts.
  * qrefresh broken
  * Usability problem with incoming/outgoing mode in the sync view
  * cannot Push or Update under eclipse 4.1.0 due to exception in mercurialeclipse
  * linked resources: 'not under root' error message on library source save
  * Mercurial Queue New Patch Wizard drops the last character in the "Patch Name" text box.
  * Plugin startup fails if no Mercurial projects are in workspace
  * locale error in Windows7
  * rebase: unresolved conflicts not detected using Mercurial 1.8+
  * An internal error occurred during: "Retrieving Mercurial revisions...".
  * NullPointerException in MercurialProjectSetCapability
  * Moving files between projects fails
  * Eclipse Text editor's default encoding is MS950 which cause cannot commit code to mercurial
  * Automatic merge fails with an abort?
  * Annotate almost never working.

### Full List ###

  * [Changes](http://www.javaforge.com/proj/cmdb/releaseNotes.spr?task_id=13991)


## MercurialEclipse 1.8.2 (21.07.2011) ##
Minor bug-fix release:
  * Better support for Mercurial 1.8 and 1.9 for conflict detection for rebase and transplant and new head detection.
  * Fix a history view memory leak exposed in Eclipse Indigo


## MercurialEclipse 1.8.1 (19.04.2011) ##
### Bug Fixes ###
  * Many operations fails if hg command line is too large (requires hg 1.8.0)
  * Eclipse slow because Hg-Plugin calls hg for every file once (requires hg 1.8.0)
  * Project / repo cache is not cleaned up after deleting a project
  * incorrect handling of '>' and '<' characters in branch name
  * Project -> refactor -> move is not working as expected
  * Renaming a tracked Java file fails with "Resource is out of sync with the file system"
  * Nullpointer may be thrown if project doesn't have a hg root.
  * Refactor->Rename project throws lots of errors
  * Various actions fail for recursive projects if the root project is closed
  * MercurialEclipse sometimes loses the status of some files
  * Moving folders/packages doesn't move new files (not yet in the repo)
  * Removing linked source folder, deletes contents of the linked folder.
  * Double quote in branch name prevents history from being viewed

### Feature Requests ###
  * Added support for new hg listfile: pattern-matching (requires hg 1.8.0)

### Full List ###
  * [Changes](http://www.javaforge.com/proj/cmdb/releaseNotes.spr?task_id=14156)

# Older releases #
  * [Changes in 1.8.0](http://www.javaforge.com/proj/cmdb/releaseNotes.spr?task_id=13138)
  * [Changes in 1.7.1](http://www.javaforge.com/proj/cmdb/releaseNotes.spr?task_id=13386)
  * [Changes in 1.7.0](http://www.javaforge.com/proj/cmdb/releaseNotes.spr?task_id=11926)