package cs584.project4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

public class RatingPredictionModel {
	
	// Training Data
	public Map<Integer, User> userIdMap = new HashMap<>();
	public Map<Integer, Movie> movieIdMap = new HashMap<>();
	
	public Map<User, Set<Rating>> userRatingMap = new HashMap<>();
	public Map<Movie, Set<Rating>> movieRatingMap = new HashMap<>();
	public List<Rating> ratingList = new ArrayList<>();
	
	// Genre Info
	public Map<String, Genre> genreDescriptionMap = new HashMap<>();
	public Map<Movie, Set<Genre>> movieGenreMap = new HashMap<>();
	
	// Director Info
	public Map<String, Director> directorKeyMap = new HashMap<>();
	public Map<Movie, Director> movieDirectorMap = new HashMap<>();
	
	// Actor Info
	public Map<String, Actor> actorKeyMap = new HashMap<>();
	public Map<Movie, Set<Actor>> movieActorMap = new HashMap<>();
			
	public void initialize() {
		System.out.println("Starting initialization. Reading training data.");
		readTrainingData();
		System.out.println("Done reading training data. Reading genre data.");
		readGenreInfo();
		System.out.println("Done reading genre data. Reading director data.");
		readDirectorInfo();
		System.out.println("Done reading director data. Reading actor data.");
		readActorInfo();
		System.out.println("Done reading actor data. Setting up cross-validation.");
		setupCrossValidation();
		System.out.println("Done setting up cross-validation. Finished initialization.");
	}
	
	private void createOutputFile(List<TestRecord> testRecords) {
		ZoneId zoneId = ZoneId.of("America/New_York");
		ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), zoneId);
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss");
		String timeOutput = zdt.format(formatter);
		
		String outputFileName = "output/prediction_waustin_" + timeOutput + ".dat";
		File outputFile = new File(outputFileName);
		
		try {
			FileWriter fileWriter = new FileWriter(outputFile);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			for(TestRecord testRecord : testRecords) {
				String output = testRecord.formatPredictedRating();
				bufferedWriter.write(output + "\n");
			}
			
			bufferedWriter.close();
			fileWriter.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("Finished creating output file: " + outputFileName);
	}
	
	public void createOutputBasic() {
		// User ratings
		Map<User, Double> averageRatingForUser = new HashMap<>();
		Double averageUserRating = null;
					
		for(Map.Entry<User, Set<Rating>> entry : userRatingMap.entrySet()) {
			User currentUser = entry.getKey();
			
			OptionalDouble avg = entry.getValue().stream()
					.mapToDouble(r -> r.ratingTrue)
					.average();
			if(avg.isPresent()) {
				averageRatingForUser.put(currentUser, avg.getAsDouble());
			}
		}
		averageUserRating = averageRatingForUser.entrySet().stream().mapToDouble(e -> e.getValue()).average().getAsDouble();
		
		// Movie Ratings
		Map<Movie, Double> averageRatingForMovie = new HashMap<>();
		Double averageMovieRating = null;
		
		for(Map.Entry<Movie, Set<Rating>> entry : movieRatingMap.entrySet()) {
			Movie currentMovie = entry.getKey();
			
			OptionalDouble avg = entry.getValue().stream()
					.mapToDouble(r -> r.ratingTrue)
					.average();
			if(avg.isPresent()) {
				averageRatingForMovie.put(currentMovie, avg.getAsDouble());
			}
		}
		averageMovieRating = averageRatingForMovie.entrySet().stream().mapToDouble(e -> e.getValue()).average().getAsDouble();
		
		List<TestRecord> testRecords = readTestData();
		
		for(TestRecord testRecord : testRecords) {
			Double prediction = averageMovieRating;
			
			User predictUser = userIdMap.get(testRecord.userId);
			Movie predictMovie = movieIdMap.get(testRecord.movieId);
			
			if(predictUser != null && predictMovie != null) {
				Double predictUserAverage = averageRatingForUser.get(predictUser);
				if(predictUserAverage == null) {
					predictUserAverage = averageUserRating;
				}
				
				Double predictMovieAverage = averageRatingForMovie.get(predictMovie);
				if(predictMovieAverage == null) {
					predictMovieAverage = averageMovieRating;
				}
				
				// Can also use / and *.
				Double movieQualityFactor = predictMovieAverage - averageMovieRating;
				
				prediction = Utilities.clamp(predictUserAverage + movieQualityFactor, 0.0, 5.0);
			}
			else if(predictUser != null && predictMovie == null) {
				prediction = averageRatingForUser.get(predictUser);
				if(prediction == null) {
					prediction = averageUserRating;
				}
			}
			else if(predictUser == null && predictMovie != null) {
				prediction = averageRatingForMovie.get(predictMovie);
				if(prediction == null) {
					prediction = averageMovieRating;
				}
			}
			
			testRecord.predictedRating = prediction;
		}
		
		createOutputFile(testRecords);
	}

	public void predictBasic() {
		int totalPredictionsMade = 0;
		double rmseSum = 0.0;
		
		for(int currentPartitionId = 1; currentPartitionId <= Constants.CV_PARTITIONS; currentPartitionId++) {
			
			// User ratings
			Map<User, Double> averageRatingForUser = new HashMap<>();
			Double averageUserRating = null;
			final int partitionId = currentPartitionId; 
						
			for(Map.Entry<User, Set<Rating>> entry : userRatingMap.entrySet()) {
				User currentUser = entry.getKey();
				
				OptionalDouble avg = entry.getValue().stream()
						.filter(r -> r.dataPartitionId != partitionId)
						.mapToDouble(r -> r.ratingTrue)
						.average();
				if(avg.isPresent()) {
					averageRatingForUser.put(currentUser, avg.getAsDouble());
				}
			}
			averageUserRating = averageRatingForUser.entrySet().stream().mapToDouble(e -> e.getValue()).average().getAsDouble();
			
			// Movie Ratings
			Map<Movie, Double> averageRatingForMovie = new HashMap<>();
			Double averageMovieRating = null;
			
			for(Map.Entry<Movie, Set<Rating>> entry : movieRatingMap.entrySet()) {
				Movie currentMovie = entry.getKey();
				
				OptionalDouble avg = entry.getValue().stream()
						.filter(r -> r.dataPartitionId != partitionId)
						.mapToDouble(r -> r.ratingTrue)
						.average();
				if(avg.isPresent()) {
					averageRatingForMovie.put(currentMovie, avg.getAsDouble());
				}
			}
			averageMovieRating = averageRatingForMovie.entrySet().stream().mapToDouble(e -> e.getValue()).average().getAsDouble();

			int ratingIndex = 0;
			// Now do the prediction
			for(Rating predictRating : ratingList) {
				final int predictDataPartitionId = predictRating.dataPartitionId;

				if(predictDataPartitionId == currentPartitionId) {
					User predictUser = predictRating.user;
					Movie predictMovie = predictRating.movie;
					
					/*
					// Ratings given to other movies that the user watched
					Set<Rating> otherRatingsForUserSet = 
						userRatingMap.get(predictUser).stream()
							.filter(r -> r.dataPartitionId != predictDataPartitionId)
							.collect(Collectors.toCollection(HashSet::new));
					
					// Ratings given by other users that watched this movie
					Set<Rating> otherRatingsForMovieSet = movieRatingMap.get(predictMovie);
					*/
					
					Double predictUserAverage = averageRatingForUser.get(predictUser);
					if(predictUserAverage == null) {
						predictUserAverage = averageUserRating;
					}
					
					Double predictMovieAverage = averageRatingForMovie.get(predictMovie);
					if(predictMovieAverage == null) {
						predictMovieAverage = averageMovieRating;
					}
					
					// Can also use / and *.
					Double movieQualityFactor = predictMovieAverage - averageMovieRating;
					
					Double prediction = Utilities.clamp(predictUserAverage + movieQualityFactor, 0.0, 5.0);
					
					/*
						otherRatingsForUserSet.stream()
						 					  .filter(rating -> rating.dataPartitionId != predictDataPartitionId)
						 					  .mapToDouble(rating -> rating.ratingTrue)
						 					  .average()
						 					  .orElse(-1.0);
					
					double movieAverage =
						otherRatingsForMovieSet.stream()
											   .filter(rating -> rating.dataPartitionId != predictDataPartitionId)
											   .mapToDouble(rating -> rating.ratingTrue)
											   .average()
											   .orElse(-1.0);
					*/
					/*
					System.out.println("Predicting (UserID = " + predictRating.user.userId + ", MovieID = " + predictRating.movie.movieId + ") with partition = " + currentPartitionId + ": " +
						"PredictUserAvg = " + predictUserAverage + ", PredictMovieAvg = " + predictMovieAverage + ", Factor = " +
							movieQualityFactor + ", Prediction = " + prediction + ", TRUE = " + predictRating.ratingTrue);*/
					
					double difference = prediction - predictRating.ratingTrue;
					rmseSum += (difference * difference);
					totalPredictionsMade++;
					
					//predictRatingIndex++;
				}
				
				ratingIndex++;
			}
		}
		
		double rmseScore = Math.sqrt(rmseSum / (1.0 * totalPredictionsMade));
		System.out.println("Done. RMSE score for " + totalPredictionsMade + " predictions is: " + rmseScore);
	}
	
	private List<Double> getMultiEstimatePredictions(Double averageUserRating, Map<User, Double> averageRatingForUser, 
		Double averageMovieRating, Map<Movie, Double> averageRatingForMovie, final int predictDataPartitionId, User predictUser, 
		Movie predictMovie, final boolean CV_MODE, List<ModelWeightConfig> configs) 
	{
		Double[] predictionsArray = new Double[ModelWeightConfig.ESTIMATE_COUNT];
		// [0] -> Average rating, across all users in training set.
		// [1] -> Average rating for the user we must make a prediction for.
		// [2] -> Average rating, across all movies in training set.
		// [3] -> Average rating for the movie we must make a prediction for.
		// [4] -> Average rating for user, adjusted by the movie likeability
		//        factor, which is "how much different from the user's average 
		//        ranking was the ranking for this movie, for all users in the
		//        training set that watched it?"
		// [5] -> Average rating given this user to movies, weighted by
		//        how similar they are to the genre(s) of the movie to be predicted
		// [6] -> Average rating given this user to movies with the same
		//        director as the movie to be predicted
		// [7] -> Average rating given this user to movies, weighted by
		//        how similar they are to the cast of the movie to be predicted
		
		// NOTE - Items 0, 1, 2, and 3 will not be null. Item 4 may be null for the test set.
		//        Items 5, 6, and 7 will frequently be null.

		Double predictUserAverageRating = averageUserRating;
		
		if(predictUser != null) {
			////////////////////////////////////////////////////////////////
			// Step 0. Average across all users
			////////////////////////////////////////////////////////////////
			predictionsArray[0] = averageUserRating;
			
			////////////////////////////////////////////////////////////////
			// Step 1. Compute the predicted average by taking this user's 
			// base average rating for all movies that they've watched.
			////////////////////////////////////////////////////////////////
			Double predictUserAverage = averageRatingForUser.get(predictUser);
			if(predictUserAverage != null) {
				predictUserAverageRating = predictUserAverage;
				predictionsArray[1] = predictUserAverageRating;
			}
		}

		Double predictMovieAverageRating = averageMovieRating;
		
		if(predictMovie != null) {
			////////////////////////////////////////////////////////////////
			// Step 2. Average across all movies 
			////////////////////////////////////////////////////////////////
			predictionsArray[2] = averageMovieRating;
			
			////////////////////////////////////////////////////////////////
			// Step 3. Compute the predicted average by taking this movies's 
			// base average rating for all users that have seen it.
			////////////////////////////////////////////////////////////////
			Double predictMovieAverage = averageRatingForMovie.get(predictMovie);
			if(predictMovieAverage != null) {
				predictMovieAverageRating = predictMovieAverage;
				predictionsArray[3] = predictMovieAverageRating;
			}
		}
		
		if(predictUser != null && predictMovie != null) {
			////////////////////////////////////////////////////////////////
			// Step 4. Compute the predicted average by taking this user's 
			// base average and adjusting it by how others liked the movie
			// as compared to their base average.
			////////////////////////////////////////////////////////////////

			
			// Can also use / and *.
			Double movieLikeablilityFactor = predictMovieAverageRating - averageMovieRating;
			
			Double movieLikeabilityBasedRating =
				Utilities.clamp(predictUserAverageRating + movieLikeablilityFactor, 0.0, 5.0);
			
			predictionsArray[4] = movieLikeabilityBasedRating;
								
			////////////////////////////////////////////////////////////////
			// Step 5. Compute the predicted average based on just the genre
			////////////////////////////////////////////////////////////////
			Double predictedRatingForUserByGenre = null;
			Set<Rating> otherRatingsForUser = userRatingMap.get(predictUser);
			Double similaritySum = 0.0;
			Double weightedRatingSum = 0.0;
			for(Rating otherRating : otherRatingsForUser) {
				if(!CV_MODE || (otherRating.dataPartitionId != predictDataPartitionId)) {
					Double movieGenreSimilarity = getMovieGenreSimilarity(predictMovie, otherRating.movie);
					if(movieGenreSimilarity > 0.0) {
						weightedRatingSum += (movieGenreSimilarity * otherRating.ratingTrue);
						similaritySum += movieGenreSimilarity;
					}
				}
			}
			
			if(similaritySum > 0.0) {
				predictedRatingForUserByGenre = (weightedRatingSum / similaritySum);
				predictionsArray[5] = predictedRatingForUserByGenre;
			}
			
			////////////////////////////////////////////////////////////////
			// Step 6. Compute the predicted average based on just the director
			////////////////////////////////////////////////////////////////
			Double predictedRatingForUserByDirector = null;
			similaritySum = 0.0;
			weightedRatingSum = 0.0;
			for(Rating otherRating : otherRatingsForUser) {
				if(!CV_MODE || (otherRating.dataPartitionId != predictDataPartitionId)) {
					Double movieDirectorSimilarity = getMovieDirectorSimilarity(predictMovie, otherRating.movie);
					if(movieDirectorSimilarity > 0.0) {
						weightedRatingSum += (movieDirectorSimilarity * otherRating.ratingTrue);
						similaritySum += movieDirectorSimilarity;
					}
				}
			}
			if(similaritySum > 0.0) {
				predictedRatingForUserByDirector = (weightedRatingSum / similaritySum);
				predictionsArray[6] = predictedRatingForUserByDirector;
			}
			
			////////////////////////////////////////////////////////////////
			// Step 7. Compute the predicted average based on just the cast
			////////////////////////////////////////////////////////////////
			Double predictedRatingForUserByActors = null;
			similaritySum = 0.0;
			weightedRatingSum = 0.0;
			for(Rating otherRating : otherRatingsForUser) {
				if(!CV_MODE || (otherRating.dataPartitionId != predictDataPartitionId)) {
					Double movieActorsSimilarity = getMovieActorsSimilarity(predictMovie, otherRating.movie);
					if(movieActorsSimilarity > 0.0) {
						weightedRatingSum += (movieActorsSimilarity * otherRating.ratingTrue);
						similaritySum += movieActorsSimilarity;
					}
				}
			}
			
			if(similaritySum > 0.0) {
				predictedRatingForUserByActors = (weightedRatingSum / similaritySum);
				predictionsArray[7] = predictedRatingForUserByActors;
			}
			
			////////////////////////////////////////////////////////////////
			// Step 8. Compute the predicted rating based on the rating delta of other user's that
			//         watched the movie, and have a movie in common with this user,
			//         and weighted by similarity of the users calculated by their common
			//         watch ratings.
			////////////////////////////////////////////////////////////////
			Map<Movie, Double> predictUserMovieRatingDeltas = new HashMap<>();
			
			// 8a. Other movies for predict user - filterd by training set.
			Map<Movie, Rating> predictUserMovieRatings = new HashMap<>();
			for(Rating otherMovieRatingForPredictUser : otherRatingsForUser) {
				if(!CV_MODE || (otherMovieRatingForPredictUser.dataPartitionId != predictDataPartitionId)) {
					predictUserMovieRatings.put(otherMovieRatingForPredictUser.movie, otherMovieRatingForPredictUser);
					predictUserMovieRatingDeltas.put(otherMovieRatingForPredictUser.movie, 
										otherMovieRatingForPredictUser.ratingTrue - predictUserAverageRating);
				}
			}

			// 8b. Other users for predict movie, filtered by training set.
			Map<User, Rating> predictMovieUserRatings = new HashMap<>();
			
			Set<Rating> otherRatingsForPredictMovie = movieRatingMap.get(predictMovie);
			
			for(Rating otherUserRatingPredictMovie : otherRatingsForPredictMovie) {
				if(!CV_MODE || (otherUserRatingPredictMovie.dataPartitionId != predictDataPartitionId)) {
					predictMovieUserRatings.put(otherUserRatingPredictMovie.user, otherUserRatingPredictMovie);
				}
			}
			
			// 8c. For each other user that watched the movie to predict (8b), find all the additional movies they watched.
			Map<User, Set<Rating>> otherUserCommonRatingsMap = new HashMap<>();
			
			for(Map.Entry<User, Rating> potentialCommonRating : predictMovieUserRatings.entrySet()) {
				User otherUser = potentialCommonRating.getKey();
				Rating otherUserPredictMovieRating = potentialCommonRating.getValue();
				
				Set<Rating> ratingsForOtherUser = new HashSet<>();
				for(Rating ratingForOtherUser : userRatingMap.get(otherUser)) {
					if(!CV_MODE || (ratingForOtherUser.dataPartitionId != predictDataPartitionId)) {
						Movie ratingForOtherUserMovie = ratingForOtherUser.movie;
						if(predictUserMovieRatings.containsKey(ratingForOtherUserMovie)) {
							Set<Rating> currentCommonRatings = otherUserCommonRatingsMap.get(otherUser);
							if(currentCommonRatings == null) {
								currentCommonRatings = new HashSet<>();
								otherUserCommonRatingsMap.put(otherUser, currentCommonRatings);
							}
							currentCommonRatings.add(ratingForOtherUser);
						}
					}
				}
			}
			
			// 8d. Calculate the metrics for each of the users with common movies
			Double maxAverageDelta = null;
			Double minAverageDelta = null;
			Map<User, Double> otherUserAverageDeltaMap = new HashMap<>();
			Map<User, Double> otherUserAverageRatingMap = new HashMap<>();
						
			for(Map.Entry<User, Set<Rating>> otherUserRatingsEntry : otherUserCommonRatingsMap.entrySet()) {
				User otherUser = otherUserRatingsEntry.getKey();
				Set<Rating> otherUserRatings = otherUserRatingsEntry.getValue();
				Double otherUserAverageRating = averageRatingForUser.get(otherUser);
				if(otherUserAverageRating == null) {
					otherUserAverageRating = averageUserRating;
				}
				otherUserAverageRatingMap.put(otherUser, otherUserAverageRating);
				
				int movieCount = 0;
				Double totalDelta = 0.0;
				for(Rating commonRating : otherUserRatings) {
					Movie commonMovie = commonRating.movie;
					Double otherUserRatingDelta = commonRating.ratingTrue - otherUserAverageRating;
					Double predictUserRatingDelta = predictUserMovieRatingDeltas.get(commonMovie);
					totalDelta += Math.abs(otherUserRatingDelta - predictUserRatingDelta);
					movieCount++;
				}
				
				Double averageDelta = totalDelta / (1.0 * movieCount);
				otherUserAverageDeltaMap.put(otherUser, averageDelta);
				
				if(maxAverageDelta == null || averageDelta > maxAverageDelta) {
					maxAverageDelta = averageDelta;
				}
				
				if(minAverageDelta == null || averageDelta < minAverageDelta) {
					minAverageDelta = averageDelta;
				}
			}
			
			// 8e. Calculate the weights for each of the common users, based on how similar their ratings for their common movies are.
			Map<User, Double> userWeightsMap = new HashMap<>();
			Double sumOfUserWeights = 0.0;
			
			for(Map.Entry<User, Double> otherUserAverageDeltaEntry : otherUserAverageDeltaMap.entrySet()) {
				User otherUser = otherUserAverageDeltaEntry.getKey();
				Double otherUserAverageDelta = otherUserAverageDeltaEntry.getValue();
				Double weight = 1.0;
				
				if(maxAverageDelta > minAverageDelta) {
					// Normalize to [0, 1]
					Double normalizedError = 1.0 - ((otherUserAverageDelta - minAverageDelta) / (maxAverageDelta - minAverageDelta));
					weight = normalizedError * normalizedError;
				}
				
				userWeightsMap.put(otherUser, weight);
				sumOfUserWeights += weight;
			}
			
			// 8f. Make weighted prediction
			if(sumOfUserWeights > 0.0) {
				Double estimateTotalSum = 0.0;
				Double estimateWeightsSum = 0.0;
				
				for(Map.Entry<User, Double> userWeightsEntry : userWeightsMap.entrySet()) {
					User otherUser = userWeightsEntry.getKey();
					
					Rating otherUserMovieRatingObj = predictMovieUserRatings.get(otherUser);
					Double otherUserMovieRating = 1.0 * otherUserMovieRatingObj.ratingTrue;
					Double otherUserMovieDelta = otherUserMovieRating - otherUserAverageRatingMap.get(otherUser);
					
					Double prediction = predictUserAverageRating + otherUserMovieDelta;
					Double userWeight = userWeightsEntry.getValue();
					estimateTotalSum += (prediction * userWeight);
					estimateWeightsSum += userWeight;
				}
				
				predictionsArray[8] = estimateTotalSum / estimateWeightsSum;
			}
			
				
				/*
			for(Map.Entry<Movie, Rating> otherMovieRating : predictUserMovieRatings.entrySet()) {
				Movie movieWatchedByPredictUser = otherMovieRating.getKey();
				for(Rating commonMovieRatings : movieRatingMap.get(movieWatchedByPredictUser))
					
			}
			
			Set<Movie> predictUserMovies =
				userRatingMap.get
			*/
			
			/*
			Set<Genre> predictMovieGenres = movieGenreMap.get(predictMovie);
			Set<Rating> otherRatingsForUserSet = 
			userRatingMap.get(predictUser).stream()
				.filter(r -> r.dataPartitionId != predictDataPartitionId)
				.collect(Collectors.toCollection(HashSet::new));
			*/
			
			/*
			// Ratings given to other movies that the user watched
			Set<Rating> otherRatingsForUserSet = 
				userRatingMap.get(predictUser).stream()
					.filter(r -> r.dataPartitionId != predictDataPartitionId)
					.collect(Collectors.toCollection(HashSet::new));
			
			
			
			// Ratings given by other users that watched this movie
			Set<Rating> otherRatingsForMovieSet = movieRatingMap.get(predictMovie);
			*/
			

			
			/*
				otherRatingsForUserSet.stream()
				 					  .filter(rating -> rating.dataPartitionId != predictDataPartitionId)
				 					  .mapToDouble(rating -> rating.ratingTrue)
				 					  .average()
				 					  .orElse(-1.0);
			
			double movieAverage =
				otherRatingsForMovieSet.stream()
									   .filter(rating -> rating.dataPartitionId != predictDataPartitionId)
									   .mapToDouble(rating -> rating.ratingTrue)
									   .average()
									   .orElse(-1.0);
			*/
			/*
			System.out.println("Predicting (UserID = " + predictRating.user.userId + ", MovieID = " + predictRating.movie.movieId + ") with partition = " + currentPartitionId + ": " +
				"PredictUserAvg = " + predictUserAverage + ", PredictMovieAvg = " + predictMovieAverage + ", Factor = " +
					movieQualityFactor + ", Prediction = " + prediction + ", TRUE = " + predictRating.ratingTrue);*/
		}
		/*
		else if(predictUser != null && predictMovie == null) {
			prediction = averageRatingForUser.get(predictUser);
			if(prediction == null) {
				prediction = averageUserRating;
			}
		}
		else if(predictUser == null && predictMovie != null) {
			prediction = averageRatingForMovie.get(predictMovie);
			if(prediction == null) {
				prediction = averageMovieRating;
			}
		}*/
		
		List<Double> results = new ArrayList<>();
		
		for(ModelWeightConfig config : configs) {
			Double[] currentWeights = config.weights;
			Double sumOfActiveWeights = 0.0;
			Double weightedEstimateSum = 0.0;
			
			for(int i = 0; i < ModelWeightConfig.ESTIMATE_COUNT; i++) {
				if(currentWeights[i] != null && currentWeights[i] > 0.0) {
					if(predictionsArray[i] != null) {
						weightedEstimateSum += (currentWeights[i] * predictionsArray[i]);
						sumOfActiveWeights += currentWeights[i];
					}
				}
			}
			
			Double configResult = null;
			if(sumOfActiveWeights > 0.0) {
				configResult = weightedEstimateSum / sumOfActiveWeights;
			}
			
			results.add(configResult);
		}
				
		return results;
	}
	
	private Double firstNonNull(List<Double> doubleList) {
		for(Double d : doubleList) {
			if(d != null) {
				return d;
			}
		}
		
		return null;
	}
	
	public void predictMultiEstimate(boolean isCrossValidation, List<ModelWeightConfig> configs, int cvPredictionSize) {
		int cvConfigsToTrack = configs.size() + 1;
		int trueCvIndex = configs.size();
		
		int[] cvNonNullCount = new int[cvConfigsToTrack];
		Double[] cvRmseSum = new Double[cvConfigsToTrack];
		for(int i = 0; i < cvConfigsToTrack; i++) {
			cvRmseSum[i] = 0.0;
		}
				
		Double submissionRmseSum = 0.0;
		int submissionNonNullCount = 0;
		
		int totalPredictions = 0;
		
		final boolean CV_MODE = isCrossValidation;
		int minPartitionId = (CV_MODE ? 1 : 0);
		int maxPartitionId = (CV_MODE ? Constants.CV_PARTITIONS : 0);
		
		for(int currentPartitionId = minPartitionId; currentPartitionId <= maxPartitionId; currentPartitionId++) {
			
			// User ratings
			Map<User, Double> averageRatingForUser = new HashMap<>();
			Double averageUserRating = null;
			final int partitionId = currentPartitionId; 
						
			for(Map.Entry<User, Set<Rating>> entry : userRatingMap.entrySet()) {
				User currentUser = entry.getKey();
				
				OptionalDouble avg = entry.getValue().stream()
						.filter(r -> (!CV_MODE || (r.dataPartitionId != partitionId)))
						.mapToDouble(r -> r.ratingTrue)
						.average();
				if(avg.isPresent()) {
					averageRatingForUser.put(currentUser, avg.getAsDouble());
				}
			}
			averageUserRating = averageRatingForUser.entrySet().stream().mapToDouble(e -> e.getValue()).average().getAsDouble();
			
			// Movie Ratings
			Map<Movie, Double> averageRatingForMovie = new HashMap<>();
			Double averageMovieRating = null;
			
			for(Map.Entry<Movie, Set<Rating>> entry : movieRatingMap.entrySet()) {
				Movie currentMovie = entry.getKey();
				
				OptionalDouble avg = entry.getValue().stream()
						.filter(r -> (!CV_MODE || (r.dataPartitionId != partitionId)))
						.mapToDouble(r -> r.ratingTrue)
						.average();
				if(avg.isPresent()) {
					averageRatingForMovie.put(currentMovie, avg.getAsDouble());
				}
			}
			averageMovieRating = averageRatingForMovie.entrySet().stream().mapToDouble(e -> e.getValue()).average().getAsDouble();
			
			if(CV_MODE) {
				int ratingIndex = 0;
			
				for(Rating predictRating : ratingList) {
					final int predictDataPartitionId = predictRating.dataPartitionId;

					if(predictDataPartitionId == currentPartitionId) {
						User predictUser = predictRating.user;
						Movie predictMovie = predictRating.movie;
						
						List<Double> predictions = 
							getMultiEstimatePredictions(averageUserRating, averageRatingForUser, averageMovieRating, averageRatingForMovie,
								currentPartitionId, predictUser, predictMovie, CV_MODE, configs);
						
						int predictionIndex = 0;
						for(Double prediction : predictions) {
							if(prediction != null) {
								Double difference = prediction - predictRating.ratingTrue;
								cvRmseSum[predictionIndex] += (difference * difference);
								cvNonNullCount[predictionIndex] += 1;
							}
							predictionIndex++;
						}
						
						Double cvPrediction = firstNonNull(predictions);
						if(cvPrediction != null) {
							Double cvDifference = cvPrediction - predictRating.ratingTrue;
							cvRmseSum[trueCvIndex] += (cvDifference * cvDifference);
							cvNonNullCount[trueCvIndex] += 1;
						}
						
						totalPredictions++;
						
						if(totalPredictions % 100 == 0) {
							int cvPredictionsSizeActual = (cvPredictionSize >= 0 ? cvPredictionSize : ratingList.size());
							System.out.println("Cross-validation progress: " + totalPredictions + " / " + cvPredictionsSizeActual);
						}
					}
					ratingIndex++;
					
					

					// Possibly only do predictions for some of the records.
					if(cvPredictionSize >= 0 && ratingIndex >= cvPredictionSize) {
						break;
					}
				}
			}
			else {
				// Submit mode. We define submission mode to be partition 0, so this will only run once.
				List<TestRecord> testRecords = readTestData();
				
				for(TestRecord testRecord : testRecords) {
					User predictUser = userIdMap.get(testRecord.userId);
					Movie predictMovie = movieIdMap.get(testRecord.movieId);
				
					List<Double> predictions = 
							getMultiEstimatePredictions(averageUserRating, averageRatingForUser, averageMovieRating, averageRatingForMovie,
								currentPartitionId, predictUser, predictMovie, CV_MODE, configs);
					
					Double prediction = firstNonNull(predictions);
					if(prediction == null) {
						// This should not happen, but just in case ...
						prediction = (averageUserRating + averageMovieRating) / 2.0;
					}
					
					testRecord.predictedRating = prediction;
					totalPredictions++;
					
					if(totalPredictions % 100 == 0) {
						System.out.println("Test set progress: " + totalPredictions + " / " + testRecords.size());
					}
				}
			
				createOutputFile(testRecords);
			}
		}
		
		if(CV_MODE) {
			System.out.println("Printing summary statistics for cross-validation. Last record is real combined CV output.");
			for(int i = 0; i < configs.size(); i++) {
				System.out.println("i = " + i + ", Config = " + configs.get(i));
				int predictionCount = cvNonNullCount[i];
				if(predictionCount > 0) {
					double rmseScore = Math.sqrt(cvRmseSum[i] / (predictionCount * 1.0));
					System.out.println("Predictions made for " + predictionCount + " / " + totalPredictions + ". RMSE = " + rmseScore);
				}
				else {
					System.out.println("No predictions made for this config!!");
				}
				System.out.println("--------------------------------------------------------");
			}
			
			System.out.println("i = " + trueCvIndex + ", True cross-validation score, calculated using the combined configs from above.");
			int predictionCount = cvNonNullCount[trueCvIndex];
			if(predictionCount > 0) {
				double rmseScore = Math.sqrt(cvRmseSum[trueCvIndex] / (predictionCount * 1.0));
				System.out.println("Predictions made for " + predictionCount + " / " + totalPredictions + ". RMSE = " + rmseScore);
			}
			else {
				System.out.println("Uh-oh!! No predictions made for cross-validation!!");
			}
		}
		else {
			System.out.println("Finished creating output file for " + totalPredictions + " test records.");
		}
	}
	
	private Double getMovieGenreSimilarity(Movie a, Movie b) {
		Double similarity = 0.0;
		if(a != null && b != null) {
			Set<Genre> aGenres = movieGenreMap.get(a);
			Set<Genre> bGenres = movieGenreMap.get(b);
			
			if(aGenres != null && bGenres != null && aGenres.size() > 0 && bGenres.size() > 0) {
				int overlap = 0;
				for(Genre bGenre : bGenres) {
					if(aGenres.contains(bGenre)) {
						overlap++;
					}
				}
				similarity = (1.0 * overlap) / Math.sqrt(1.0 * aGenres.size() * bGenres.size());
			}
		}
		return similarity;
	}
	
	private Double getMovieDirectorSimilarity(Movie a, Movie b) {
		Double similarity = 0.0;
		if(a != null && b != null) {
			Director aDirector = movieDirectorMap.get(a);
			Director bDirector = movieDirectorMap.get(b);
			
			if(aDirector != null && bDirector != null) {
				if(aDirector.directorId == bDirector.directorId) {
					similarity = 1.0; 
				}
			}
		}
		return similarity;
	}
	
	private Double getMovieActorsSimilarity(Movie a, Movie b) {
		Double similarity = 0.0;
		if(a != null && b != null) {
			Set<Actor> aActors = movieActorMap.get(a);
			Set<Actor> bActors = movieActorMap.get(b);
			
			if(aActors != null && bActors != null && aActors.size() > 0 && bActors.size() > 0) {
				int overlap = 0;
				for(Actor bActor : bActors) {
					if(aActors.contains(bActor)) {
						overlap++;
					}
				}
				similarity = (1.0 * overlap) / Math.sqrt(1.0 * aActors.size() * bActors.size());
			}
		}
		return similarity;
	}
	
	private Double predictScoreFromOtherMoviesForUser(Movie predictMovie, Set<Rating> otherRatingsForUser) {
		if(otherRatingsForUser == null || otherRatingsForUser.isEmpty()) {
			return null;
		}
		
		Double weightedTotal = 0.0;
		Double basicFactor = 2.0;
		Double genreFactor = 5.0;
		
		for(Rating otherRatingForUser : otherRatingsForUser) {
			
		}
		
		return 0.0;
	}
	

	private Double calculateMovieSimilarityByGenre(Movie a, Movie b) {
		Set<Genre> aGenreSet = movieGenreMap.get(a);
		Set<Genre> bGenreSet = movieGenreMap.get(b);
		int totalOverlap = 0;
		
		for(Genre aGenre : aGenreSet) {
			for(Genre bGenre : bGenreSet) {
				if(aGenre.genreId == bGenre.genreId) {
					totalOverlap++;
				}
			}
		}
		
		// Use Dice's Coefficient		
		// return (2.0 * totalOverlap) / (1.0 * (aGenreSet.size() + bGenreSet.size()));
		
		// Use the Otsuka-Ochiai coefficient / cosine similarity
		return totalOverlap / Math.sqrt(1.0 * aGenreSet.size() * bGenreSet.size());
	}
	
	
	private Double calculateMovieSimilarityByWatchRating(Movie a, Movie b, final int predictDataPartitionId) {
		int totalCommon = 0;
		Double ratingSum = 0.0;
		Set<Rating> aRatings = 
			movieRatingMap.get(a).stream()
				.filter(r -> r.dataPartitionId != predictDataPartitionId)
				.collect(Collectors.toCollection(HashSet::new));
		
		Set<Rating> bRatings =
				movieRatingMap.get(b).stream()
				.filter(r -> r.dataPartitionId != predictDataPartitionId)
				.collect(Collectors.toCollection(HashSet::new));
		
		for(Rating aRating : aRatings) {
			for(Rating bRating : bRatings) {
				if(aRating.user.userId == bRating.user.userId) {
					totalCommon++;
					Double ratingIntermediate = 5 - Math.abs((1.0 * aRating.ratingTrue) - (1.0 * bRating.ratingTrue));
					
					// This should be in the interval [0, 1]
					Double rating = (ratingIntermediate * ratingIntermediate) / 25.0;
					ratingSum += rating;
				}
			}
		}
		
		if(totalCommon > 0) {
			return ratingSum / (totalCommon * 1.0);
		}
		
		return null;
	}
	
	private void setupCrossValidation() {
		Collections.shuffle(ratingList);
		int currentIndex = 0;
		for(Rating rating : ratingList) {
			rating.dataPartitionId = (currentIndex % Constants.CV_PARTITIONS) + 1;
			rating.ratingPredicted = -1.0f;
			currentIndex++;
		}
	}
	
	private void readTrainingData() {
		Path filePath = Paths.get("resources/train.dat");
		try (
			BufferedReader bufferedReader = Files.newBufferedReader(filePath);
		) {
			// Header Line
			bufferedReader.readLine();
			
			//int totalRecordsParsed = 0;
			String userMovieRatingLine = bufferedReader.readLine();
			while(userMovieRatingLine != null) {
				String[] parts = userMovieRatingLine.split(" ");
				if(parts != null && parts.length == 3) {
					int userId = Integer.parseInt(parts[0]);
					int movieId = Integer.parseInt(parts[1]);
					float ratingScore = Float.parseFloat(parts[2]);
					
					User user = userIdMap.get(userId);
					if(user == null) {
						user = new User(userId);
						userIdMap.put(userId, user);
					}
					
					Movie movie = movieIdMap.get(movieId);
					if(movie == null) {
						movie = new Movie(movieId);
						movieIdMap.put(movieId, movie);
					}
					
					Rating rating = new Rating(user, movie, ratingScore);
					
					Set<Rating> ratingsForUser = userRatingMap.get(user);
					if(ratingsForUser == null) {
						ratingsForUser = new HashSet<Rating>();
						userRatingMap.put(user, ratingsForUser);
					}
					ratingsForUser.add(rating);
					
					Set<Rating> ratingsForMovie = movieRatingMap.get(movie);
					if(ratingsForMovie == null) {
						ratingsForMovie = new HashSet<Rating>();
						movieRatingMap.put(movie, ratingsForMovie);
					}
					ratingsForMovie.add(rating);
					
					ratingList.add(rating);
					
					/*
					totalRecordsParsed++;
					if(totalRecordsParsed % 1000 == 0) {
						System.out.println("Parsed records count = " + totalRecordsParsed);
					}*/
				}
				
				userMovieRatingLine = bufferedReader.readLine();				
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readGenreInfo() {
		Path filePath = Paths.get("resources/movie_genres.dat");
		try (
			BufferedReader bufferedReader = Files.newBufferedReader(filePath);
		){
			// Header Line
			bufferedReader.readLine();
			
			int nextGenreId = 1;
			String movieGenreLine = bufferedReader.readLine();
			while(movieGenreLine != null) {
				String[] parts = movieGenreLine.split("\t");
				if(parts != null && parts.length == 2) {
					int movieId = Integer.parseInt(parts[0]);
					String genreDescription = parts[1];
					
					Genre genre = genreDescriptionMap.get(genreDescription);
					if(genre == null) {
						genre = new Genre(genreDescription, nextGenreId);
						genreDescriptionMap.put(genreDescription, genre);
						nextGenreId++;
					}
					
					Movie movie = movieIdMap.get(movieId);
					if(movie != null) {
						Set<Genre> movieGenreSet = movieGenreMap.get(movie);
						if(movieGenreSet == null) {
							movieGenreSet = new HashSet<>();
							movieGenreMap.put(movie, movieGenreSet);
						}
						movieGenreSet.add(genre);
					}
				}
				
				movieGenreLine = bufferedReader.readLine();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readDirectorInfo() {
		Path filePath = Paths.get("resources/movie_directors.dat");
		try (
			BufferedReader bufferedReader = Files.newBufferedReader(filePath, StandardCharsets.ISO_8859_1);
		) {
			// Header Line
			bufferedReader.readLine();
			
			int nextDirectorId = 1;
			String movieDirectorLine = bufferedReader.readLine();
			while(movieDirectorLine != null) {
				String[] parts = movieDirectorLine.split("\t");
				if(parts != null && parts.length == 3) {
					int movieId = Integer.parseInt(parts[0]);
					String directorKey = parts[1];
					String directorName = parts[2];
					
					Director director = directorKeyMap.get(directorKey);
					if(director == null) {
						director = new Director(directorName, directorKey, nextDirectorId);
						directorKeyMap.put(directorKey, director);
						nextDirectorId++;
					}
					
					Movie movie = movieIdMap.get(movieId);
					if(movie != null) {
						movieDirectorMap.put(movie, director);
					}
				}
				
				movieDirectorLine = bufferedReader.readLine();
			}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void readActorInfo() {
		Path filePath = Paths.get("resources/movie_actors.dat");
		String movieActorLine = null;
		try (
				BufferedReader bufferedReader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
			){
			// Header Line
			bufferedReader.readLine();
			
			int nextActorId = 1;
			movieActorLine = bufferedReader.readLine();
			while(movieActorLine != null) {
				String[] parts = movieActorLine.split("\t");
				if(parts != null && parts.length == 4) {
					int movieId = Integer.parseInt(parts[0]);
					String actorKey = parts[1];
					String actorName = parts[2];
					int ranking = Integer.parseInt(parts[3]);
					
					// Filter to only load the top 7 actors for each movie. The rest don't really seem so important?
					if(ranking <= 10) {
						Actor actor = actorKeyMap.get(actorKey);
						
						if(actor == null) {
							actor = new Actor(actorName, actorKey, nextActorId);
							actorKeyMap.put(actorKey, actor);
							nextActorId++;
						}
						
						Movie movie = movieIdMap.get(movieId);
						if(movie != null) {
							Set<Actor> movieActorSet = movieActorMap.get(movie);
							if(movieActorSet == null) {
								movieActorSet = new HashSet<>();
								movieActorMap.put(movie, movieActorSet);
							}
							movieActorSet.add(actor);
						}
					}
				}
				
				movieActorLine = bufferedReader.readLine();
			}
		}
		catch (IOException e) {
			System.out.println("Got exception!! Line was: " + movieActorLine);
			e.printStackTrace();
		}
	}
	
	private List<TestRecord> readTestData() {
		Path filePath = Paths.get("resources/test.dat");
		String testLine = null;
		List<TestRecord> testRecords = new ArrayList<>();
		try (
				BufferedReader bufferedReader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
			){
			// Header Line
			bufferedReader.readLine();
			
			testLine = bufferedReader.readLine();
			while(testLine != null) {
				String[] parts = testLine.split(" ");
				if(parts != null && parts.length == 2) {
					int userId = Integer.parseInt(parts[0]);
					int movieId = Integer.parseInt(parts[1]);
					
					TestRecord record = new TestRecord(userId, movieId);
					testRecords.add(record);
					/*
					User user = userIdMap.get(userId);
					Movie movie = movieIdMap.get(movieId);
					
					if(user != null && movie != null) {

					}
					else {
						System.out.println("Something is null!");
						System.out.println("User ID = " + userId + ", Movie ID = " + movieId);
						System.out.println("User null = " + (user == null) + ", Movie null = " + (movie == null));
						
					}
					*/
				}
				
				testLine = bufferedReader.readLine();
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		return testRecords;
	}
}
