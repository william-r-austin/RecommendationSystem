package cs584.project4;

public class Utilities {
	
	public static Double clamp(Double x, Double min, Double max) {
		if(x > max) {
			return max;
		}
		
		if(x < min) {
			return min;
		}
		
		return x;
	}

}
