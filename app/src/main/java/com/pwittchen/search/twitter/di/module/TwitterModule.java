package com.pwittchen.search.twitter.di.module;

import com.pwittchen.search.twitter.twitter.TwitterApi;
import com.pwittchen.search.twitter.twitter.TwitterApiProvider;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module public final class TwitterModule {
  @Provides @Singleton public TwitterApi provideTwitterApi() {
    TwitterApi twitterApi = new TwitterApiProvider();
    return twitterApi;
  }
}
