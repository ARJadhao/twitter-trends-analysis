package com.serverless.dev.twitter_trends.logs;



import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import twitter4j.Trend;
import twitter4j.Trends;

public class LogService {

	public void logTweetDetails(Trends trends, Context context) throws JsonProcessingException{
		
		String jsonString = "";
		LambdaLogger logger = context.getLogger();
		
		for(Trend trend : trends.getTrends()){
			Log log = new Log();
			log.setHashtag(trend.getName());
			log.setTweetVolume(trend.getTweetVolume());
			
			if(trend.getTweetVolume() > 10000){
				ObjectMapper mapper = new ObjectMapper();
				jsonString = mapper.writeValueAsString(log);
				logger.log(jsonString);
			}
			
		}
		
	}
}
