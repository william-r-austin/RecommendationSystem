package cs584.project4;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class TestRecord {
	private static final DecimalFormat df;
	
	static {
		df = new DecimalFormat("0.0");
		df.setRoundingMode(RoundingMode.HALF_UP);
	}
	
	public int userId;
	public int movieId;
	public double predictedRating;
	
	public TestRecord(int userId, int movieId) {
		this.userId = userId;
		this.movieId = movieId;
	}
	
	public String formatPredictedRating() {
		return df.format(predictedRating);
	}
}
