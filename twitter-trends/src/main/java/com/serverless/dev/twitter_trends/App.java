package com.serverless.dev.twitter_trends;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serverless.dev.twitter_trends.logs.LogService;

import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class App implements RequestHandler<Object, String> {

	public String handleRequest(Object obj, Context context) {
		// TODO Auto-generated method stub
		
		LogService logService = new LogService();
		HashMap<String, String> secrets = new HashMap<String, String>();
		
		// Read Lambda Env variables... 
		String secretName = System.getenv("secretname");
		String region = System.getenv("region");
		String bucketName = System.getenv("bucketname");

		// WhereOnEarthID of India
		int WOEID = 23424848;
		
		//Read twitter secrets from aws secret manager
		AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().withRegion(region).build();
		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
		GetSecretValueResult getSecretValueResult;
		getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		
		try {
			
			secrets = new ObjectMapper().readValue(getSecretValueResult.getSecretString(), HashMap.class);
			
			// Create twitter client
			ConfigurationBuilder cb = new ConfigurationBuilder();
			cb.setDebugEnabled(true).setOAuthConsumerKey(secrets.get("ConsumerKey"))
					.setOAuthConsumerSecret(secrets.get("ConsumerSecret")).setOAuthAccessToken(secrets.get("AccessToken"))
					.setOAuthAccessTokenSecret(secrets.get("AccessTokenSecret"));

			TwitterFactory tf = new TwitterFactory(cb.build());
			Twitter twitter = tf.getInstance();

			// get trending topics for India
			Trends trends = twitter.getPlaceTrends(WOEID);

			// Custom log the response for processing in Elasticssearch
			logService.logTweetDetails(trends, context);

			// save response to s3 bucket
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(region).build();
			s3Client.putObject(bucketName, java.time.Clock.systemUTC().instant().toString(), trends.toString());

		} catch (IOException | TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "success";
	}

}
