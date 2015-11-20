package be.waines.maven.model;

import java.io.Serializable;

public class BuildId implements Serializable {
	
	private static final long serialVersionUID = 7980644222771177545L;

	private String name;

	public BuildId(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
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
		BuildId other = (BuildId) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	

}
