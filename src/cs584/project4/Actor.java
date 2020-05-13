package cs584.project4;

public class Actor {
	public String actorKey;
	public String actorName;
	public int actorId;
	
	public Actor(String actorName, String actorKey, int actorId) {
		this.actorName = actorName;
		this.actorKey = actorKey;
		this.actorId = actorId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + actorId;
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
		Actor other = (Actor) obj;
		if (actorId != other.actorId)
			return false;
		return true;
	}
	

}
