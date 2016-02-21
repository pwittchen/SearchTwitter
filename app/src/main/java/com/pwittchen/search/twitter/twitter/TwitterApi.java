package com.pwittchen.search.twitter.twitter;

import java.util.List;
import rx.Observable;
import twitter4j.Status;

public interface TwitterApi {
  Observable<List<Status>> searchTweets(final String keyword);

  Observable<List<Status>> searchTweets(final String keyword, final long maxTweetId);

  int getApiRateLimitExceededErrorCode();

  int getMaxTweetsPerRequest();

  boolean canSearchTweets(final String keyword);
}
