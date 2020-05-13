package cs584.project4;

import java.util.Arrays;

public class ModelWeightConfig {
	public static final int ESTIMATE_COUNT = 9;
	
	public static final int AVG_ALL_USERS_RATING = 0;
	public static final int AVG_PREDICT_USER_RATING = 1;
	public static final int AVG_ALL_MOVIES_RATING = 2;
	public static final int AVG_PREDICT_MOVIE_RATING = 3;
	public static final int LIKEABILITY_ADJ_RATING = 4;
	public static final int USER_GENRE_RATING = 5;
	public static final int USER_DIRECTOR_RATING = 6;
	public static final int USER_ACTOR_RATING = 7;
	public static final int COMMON_WATCH_USER_SIMILARITY = 8;
	
	public Double[] weights = new Double[ESTIMATE_COUNT];
	
	public ModelWeightConfig() { }	
	
	public void set(int index, Double weight) {
		weights[index] = weight;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(weights);
	}
	
	/*
	public Predict2Config(Double... initWeights) {
		if(initWeights != null && initWeights.length == ESTIMATE_COUNT) {
			System.arraycopy(initWeights, 0, weights, 0, ESTIMATE_COUNT);
		}
	}
	*/
}
