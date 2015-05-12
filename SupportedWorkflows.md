# Introduction #

Mercurial and MercurialEclipse support a variety of workflows. These are described in more detail [here](http://mercurial.selenic.com/wiki/WorkingPractices). This is not a complete list of possible workflows. These apply mostly to a centralized repository where code is reviewed before being pushed but they apply to distributed cases as well.

# CVS-style #

**Summary:** Do not commit your changes until you're ready to push. Always do an update when you pull. **This workflow is not recommended**

**Support:** Moderate

**Pros:**
  * CVS users understand this workflow easily.

**Cons:**
  * Uncommitted workspace update is dangerous
    * There is no way to abort an uncommitted workspace update for example if there are conflicts that you don't want to resolve now.
    * MercurialEclipse support for conflict handling is poor (in this context).
  * More steps than would be needed in CVS or Subversion.
  * Doesn't take advantage of Mercurial's merge intelligence


# Anonymous heads #
**Summary:** Commit your changes and merge or rebase prior to pushing.  Either use commit with the amend option or rebase with the collapse option if you don't want your intermediate changes on the final history.

**Support:** Good

**Pros:**
  * Takes full advantage of Mercurial's merge and rebase intelligence (and these operations can be aborted)
  * Better support for concurrent development
    * Just switch to a "public" revision before starting on a new unrelated changeset

**Cons:**
  * More complicated initially for someone not familiar with DVCS


# MQ (Mercurial Patch Queue) #
**Summary:** Use MQ (To enable Window->Show View->Mercurial Patch Queue)

**Support:** Good in 1.9