package com.vectrace.MercurialEclipse.model;

public class Branch {

	/** name of the branch, unique in the repository */
	private final String name;
	private final int revision;
	private final String globalId;
	private final boolean active;
	
	public Branch(String name, int revision, String globalId, boolean active) {
		super();
		this.name = name;
		this.revision = revision;
		this.globalId = globalId;
		this.active = active;
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
	
	public boolean isActive() {
	    return active;
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
		final Branch other = (Branch) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
