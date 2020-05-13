package cs584.project4;

public class Director {
	public String directorKey;
	public String directorName;
	public int directorId;
	
	public Director(String directorName, String directorKey, int directorId) {
		this.directorName = directorName;
		this.directorKey = directorKey;
		this.directorId = directorId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + directorId;
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
		Director other = (Director) obj;
		if (directorId != other.directorId)
			return false;
		return true;
	}

}
