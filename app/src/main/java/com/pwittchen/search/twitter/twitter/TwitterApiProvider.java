package com.pwittchen.search.twitter.twitter;

import android.support.v7.widget.LinearLayoutManager;
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
  private Twitter twitterInstance;

  public TwitterApiProvider() {
    Configuration configuration = createConfiguration();
    TwitterFactory twitterFactory = new TwitterFactory(configuration);
    twitterInstance = twitterFactory.getInstance();
  }

  private Configuration createConfiguration() {
    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
    configurationBuilder.setDebugEnabled(true)
        .setOAuthConsumerKey(TwitterOAuthConfig.CONSUMER_KEY)
        .setOAuthConsumerSecret(TwitterOAuthConfig.CONSUMER_SECRET)
        .setOAuthAccessToken(TwitterOAuthConfig.ACCESS_TOKEN)
        .setOAuthAccessTokenSecret(TwitterOAuthConfig.ACCESS_TOKEN_SECRET);

    return configurationBuilder.build();
  }

  @Override public Observable<List<Status>> searchTweets(final String keyword) {
    return Observable.create(new Observable.OnSubscribe<List<Status>>() {
      @Override public void call(Subscriber<? super List<Status>> subscriber) {
        try {
          Query query = new Query(keyword).count(MAX_TWEETS_PER_REQUEST);
          QueryResult result = twitterInstance.search(query);
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
          Query query = new Query(keyword).maxId(maxTweetId).count(MAX_TWEETS_PER_REQUEST);
          QueryResult result = twitterInstance.search(query);
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

  @Override
  public boolean canLoadMoreTweets(LinearLayoutManager layoutManager, int tweetsPerRequest) {
    int visibleItemsCount = layoutManager.getChildCount();
    int totalItemsCount = layoutManager.getItemCount();
    int pastVisibleItemsCount = layoutManager.findFirstVisibleItemPosition();
    boolean lastItemShown = visibleItemsCount + pastVisibleItemsCount >= totalItemsCount;
    return lastItemShown && totalItemsCount >= tweetsPerRequest;
  }

  @Override public boolean canSearchTweets(String keyword) {
    if (keyword.trim().isEmpty()) {
      return false;
    }
    return true;
  }
}
