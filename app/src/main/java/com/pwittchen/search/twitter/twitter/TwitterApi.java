package com.pwittchen.search.twitter.twitter;

import android.support.v7.widget.LinearLayoutManager;
import java.util.List;
import rx.Observable;
import twitter4j.Status;

public interface TwitterApi {
  Observable<List<Status>> searchTweets(final String keyword);

  Observable<List<Status>> searchTweets(final String keyword, final long maxTweetId);

  int getApiRateLimitExceededErrorCode();

  int getMaxTweetsPerRequest();

  boolean canLoadMoreTweets(final LinearLayoutManager manager, final int tweetsPerRequest);

  boolean canSearchTweets(final String keyword);
}
