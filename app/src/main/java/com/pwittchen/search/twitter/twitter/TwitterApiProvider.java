package com.pwittchen.search.twitter.twitter;

import com.pwittchen.search.twitter.BuildConfig;
import java.util.List;
import rx.Observable;
import rx.Subscriber;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public final class TwitterApiProvider implements TwitterApi {
  private static final int MAX_TWEETS_PER_REQUEST = 100;
  private static final int API_RATE_LIMIT_EXCEEDED_ERROR_CODE = 88;
  private final Twitter twitterInstance;

  public TwitterApiProvider() {
    final Configuration configuration = createConfiguration();
    final TwitterFactory twitterFactory = new TwitterFactory(configuration);
    twitterInstance = twitterFactory.getInstance();
  }

  private Configuration createConfiguration() {
    final ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
    configurationBuilder.setDebugEnabled(true)
        .setOAuthConsumerKey(BuildConfig.TWITTER_CONSUMER_KEY)
        .setOAuthConsumerSecret(BuildConfig.TWITTER_CONSUMER_SECRET)
        .setOAuthAccessToken(BuildConfig.TWITTER_ACCESS_TOKEN)
        .setOAuthAccessTokenSecret(BuildConfig.TWITTER_ACCESS_TOKEN_SECRET);

    return configurationBuilder.build();
  }

  @Override public Observable<List<Status>> searchTweets(final String keyword) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          final Query query = new Query(keyword).count(MAX_TWEETS_PER_REQUEST);
          final QueryResult result = twitterInstance.search(query);
          subscriber.onNext(result.getTweets());
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    });
  }

  @Override
  public Observable<List<Status>> searchTweets(final String keyword, final long maxTweetId) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          final Query query = new Query(keyword).maxId(maxTweetId).count(MAX_TWEETS_PER_REQUEST);
          final QueryResult result = twitterInstance.search(query);
          subscriber.onNext(result.getTweets());
          subscriber.onCompleted();
        } catch (TwitterException e) {
          subscriber.onError(e);
        }
      }
    });
  }

  @Override public int getApiRateLimitExceededErrorCode() {
    return API_RATE_LIMIT_EXCEEDED_ERROR_CODE;
  }

  @Override public int getMaxTweetsPerRequest() {
    return MAX_TWEETS_PER_REQUEST;
  }

  @Override public boolean canSearchTweets(final String keyword) {
    if (keyword.trim().isEmpty()) {
      return false;
    }
    return true;
  }
}
