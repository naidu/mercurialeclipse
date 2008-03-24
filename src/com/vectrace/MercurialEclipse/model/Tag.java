package com.vectrace.MercurialEclipse.model;

public class Tag {

	/** name of the tag, unique in the repository */
	private final String name;
	private final int revision;
	private final String globalId;
	private final boolean local;
	
	public Tag(String name, int revision, String globalId, boolean local) {
		super();
		this.name = name;
		this.revision = revision;
		this.globalId = globalId;
		this.local = local;
	}

	public String getName() {
		return name;
	}

	public int getRevision() {
		return revision;
	}

	public String getGlobalId() {
		return globalId;
	}

	public boolean isLocal() {
		return local;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Tag other = (Tag) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
