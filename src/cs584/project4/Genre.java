package cs584.project4;

public class Genre {
	public String genreDescription;
	public int genreId;
	
	public Genre(String genreDescription, int genreId) {
		this.genreDescription = genreDescription;
		this.genreId = genreId;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + genreId;
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
		Genre other = (Genre) obj;
		if (genreId != other.genreId)
			return false;
		return true;
	}

	
}
