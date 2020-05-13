package cs584.project4;

import java.util.ArrayList;
import java.util.List;

public class RunRecommendationSystem {

	public static void main(String[] args) {
		// 1. Initial Experimentation and Submission. Miner RMSE = 0.86
		//calculateBasedOnMovieLikeability();
		
		// 2. Additional estimates based on other movies watched by 
		//    the user. Integrate CV and Submission logic so it runs
		//    the same code to produce the estimate.
		//    This is Test #1.
		//crossValidateEstimatesTest1();
		
		// 3. Test #2. Likeability / Genre 
		//crossValidateEstimatesTest2();
		
		// 4. Test #3. Likeability / Genre / Actors 
		//crossValidateEstimatesTest3();
		
		// 5. Submission based on Test #3 results. Miner RMSE = 0.83
		// crossValidateAndSubmitFromTest3();
		
		// 6. Test #4: Added estimate based on other users that watched the
		//    movie to predict, weighted according to the rating similarity
		//    of common movies they've watched.
		//crossValidateWatchSimilarity();
		
		// 7. Test #5. Tune the inclusion of watch similarity.
		//tuneWatchSimilarityWeight();
		
		// 8. Submission based on Test #5 results. Miner RMSE = 0.81.
		submitFromTest5();
	}
	
	// Results for CV and Miner are both 0.86
	private static void calculateBasedOnMovieLikeability() {
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictBasic();
		model.createOutputBasic();
	}

	// Test #1: See notes.txt for results from this configuration.
	//          Individual Estimates
	private static void crossValidateEstimatesTest1() {
		List<ModelWeightConfig> configs = testIndividualEstimateConfigs();
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictMultiEstimate(true, configs, 10000);
	}
	
	// Test #2: See notes.txt for results from this configuration.
	//          LIKEABILITY / GENRE
	private static void crossValidateEstimatesTest2() {
		List<ModelWeightConfig> configs = testLikeabilityGenreConfigs();
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictMultiEstimate(true, configs, 10000);
	}
	
	// Test #3: See notes.txt for results from this configuration.
	//          Combining Test #2 results with ACTORS
	private static void crossValidateEstimatesTest3() {
		List<ModelWeightConfig> configs = testLikeabilityGenreActorConfigs();
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictMultiEstimate(true, configs, 10000);
	}

	// CV and Miner Submission based on Test #3
	private static void crossValidateAndSubmitFromTest3() {
		List<ModelWeightConfig> configs = submissionConfigBasedOnTest3();
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictMultiEstimate(true, configs, 10000);
		model.predictMultiEstimate(false, configs, -1);
	}
	
	// Test #4: See notes for results. Testing estimate from watch ratings of
	//          common users.
	private static void crossValidateWatchSimilarity() {
		List<ModelWeightConfig> configs = testIndividualEstimateConfigs();
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictMultiEstimate(true, configs, 1000);
	}
	
	// Test #5: Tune the inclusion of user similarity from common movie ratings.
	private static void tuneWatchSimilarityWeight() {
		List<ModelWeightConfig> configs = new ArrayList<>();
		
		for(int i = 0; i <= 10; i++) {
			Double watchSimilarityWeight = (1.0 * i) / 10.0;
			Double remainderWeight = 1.0 - watchSimilarityWeight;
			
			ModelWeightConfig newConfig = new ModelWeightConfig();
			newConfig.set(ModelWeightConfig.COMMON_WATCH_USER_SIMILARITY, watchSimilarityWeight);
			newConfig.set(ModelWeightConfig.LIKEABILITY_ADJ_RATING, 0.595 * remainderWeight);
			newConfig.set(ModelWeightConfig.USER_GENRE_RATING, 0.255 * remainderWeight);
			newConfig.set(ModelWeightConfig.USER_ACTOR_RATING, 0.15 * remainderWeight);
			configs.add(newConfig);
		}
		
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictMultiEstimate(true, configs, 10000);
	}
	
	private static void submitFromTest5() {
		List<ModelWeightConfig> configs = new ArrayList<>();
		
		Double watchSimilarityWeight = 0.8;
		Double remainderWeight = 1.0 - watchSimilarityWeight;
		
		ModelWeightConfig newConfig = new ModelWeightConfig();
		newConfig.set(ModelWeightConfig.COMMON_WATCH_USER_SIMILARITY, watchSimilarityWeight);
		newConfig.set(ModelWeightConfig.LIKEABILITY_ADJ_RATING, 0.595 * remainderWeight);
		newConfig.set(ModelWeightConfig.USER_GENRE_RATING, 0.255 * remainderWeight);
		newConfig.set(ModelWeightConfig.USER_ACTOR_RATING, 0.15 * remainderWeight);
		configs.add(newConfig);
		
		configs.addAll(getBackupConfigs());
		
		RatingPredictionModel model = new RatingPredictionModel();
		model.initialize();
		model.predictMultiEstimate(false, configs, -1);
	}
	
	// Test #1: See notes.txt for results from this configuration.
	//          Individual Estimates
	private static List<ModelWeightConfig> testIndividualEstimateConfigs() {
		List<ModelWeightConfig> configs = new ArrayList<>();

		for(int i = 0; i < ModelWeightConfig.ESTIMATE_COUNT; i++) {
			ModelWeightConfig newConfig = new ModelWeightConfig();
			newConfig.set(i, 1.0);
			configs.add(newConfig);
		}
		
		return configs;
	}
	
	// Test #2: See notes.txt for results from this configuration.
	//          LIKEABILITY / GENRE
	private static List<ModelWeightConfig> testLikeabilityGenreConfigs() {
		List<ModelWeightConfig> configs = new ArrayList<>();
		

		for(int i = 10; i >= 0; i--) {
			ModelWeightConfig newConfig = new ModelWeightConfig();
			Double w = (1.0 * i) / 10.0;
			newConfig.set(ModelWeightConfig.LIKEABILITY_ADJ_RATING, w);
			newConfig.set(ModelWeightConfig.USER_GENRE_RATING, 1.0 - w);
			configs.add(newConfig);
		}
		
		return configs;
	}
	
	// Test #3: See notes.txt for results from this configuration.
	//          Combining Test #2 results with ACTORS
	private static List<ModelWeightConfig> testLikeabilityGenreActorConfigs() {
		List<ModelWeightConfig> configs = new ArrayList<>();
		
		for(int i = 10; i >= 0; i--) {
			ModelWeightConfig newConfig = new ModelWeightConfig();
			Double likeabilityGenreWeight = 0.7 + (0.3 * i) / 10.0;
			newConfig.set(ModelWeightConfig.LIKEABILITY_ADJ_RATING, likeabilityGenreWeight * 0.7);
			newConfig.set(ModelWeightConfig.USER_GENRE_RATING, likeabilityGenreWeight * 0.3);
			newConfig.set(ModelWeightConfig.USER_ACTOR_RATING, 1.0 - likeabilityGenreWeight);
			configs.add(newConfig);
		}
		
		return configs;
	}
	
	// Submission based on Test #3 results WEIGHTS: (LIKEABILITY = 0.595, GENRE = 0.255, ACTORS = 0.15)
	private static List<ModelWeightConfig> submissionConfigBasedOnTest3() {
		List<ModelWeightConfig> configs = new ArrayList<>();
		
		ModelWeightConfig newConfig = new ModelWeightConfig();
		newConfig.set(ModelWeightConfig.LIKEABILITY_ADJ_RATING, 0.595);
		newConfig.set(ModelWeightConfig.USER_GENRE_RATING, 0.255);
		newConfig.set(ModelWeightConfig.USER_ACTOR_RATING, 0.15);
		configs.add(newConfig);

		// Add the backup configs so that the bad test data is estimated better
		configs.addAll(getBackupConfigs());
		
		return configs;
	}
	
	private static List<ModelWeightConfig> getBackupConfigs() {
		List<ModelWeightConfig> configs = new ArrayList<>();
		// Use the Likeability, if available
		ModelWeightConfig bc1 = new ModelWeightConfig();
		bc1.set(ModelWeightConfig.LIKEABILITY_ADJ_RATING, 1.0);
		configs.add(bc1);
		
		// Fall back to user's average if available
		ModelWeightConfig bc2 = new ModelWeightConfig();
		bc2.set(ModelWeightConfig.AVG_PREDICT_USER_RATING, 1.0);
		configs.add(bc2);
		
		// Fall back to movie's average if available
		ModelWeightConfig bc3 = new ModelWeightConfig();
		bc3.set(ModelWeightConfig.AVG_PREDICT_MOVIE_RATING, 1.0);
		configs.add(bc3);
		
		// Fall back to all user average.
		ModelWeightConfig bc4 = new ModelWeightConfig();
		bc4.set(ModelWeightConfig.AVG_ALL_USERS_RATING, 1.0);
		configs.add(bc4);
		
		// Fall back to all movie average.
		ModelWeightConfig bc5 = new ModelWeightConfig();
		bc5.set(ModelWeightConfig.AVG_ALL_MOVIES_RATING, 1.0);
		configs.add(bc5);
		
		return configs;
	}
	
}
