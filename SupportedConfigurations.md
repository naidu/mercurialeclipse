# Introduction #

MercurialEclipse supports multiple Eclipse project to Mercurial repository relationships.

# Details #

| **Configuration** |**Description** | **Support** | **Notes** |
|:------------------|:---------------|:------------|:----------|
| Simple | One Eclipse project to one Mercurial repository | Full |  |
| Linked resource | A project contains links to resources in another project | Full in 1.9, partial in 1.8.1 |  |
| Multi-project | One Mercurial repository is presented as multiple Eclipse projects | Full | Operations on resource not in an Eclipse project not fully supported. In the clone wizard a project is identified by a <tt>.project</tt> file |
| Subrepositories | An Eclipse project containing containing Mercurial [subrepos](http://mercurial.selenic.com/wiki/Subrepository) | Partial | See configuration in preferences and please file bugs for unsupported use-cases |