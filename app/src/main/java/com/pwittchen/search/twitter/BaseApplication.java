package com.pwittchen.search.twitter;

import android.app.Application;
import com.pwittchen.search.twitter.di.ApplicationComponent;
import com.pwittchen.search.twitter.di.DaggerApplicationComponent;
import com.pwittchen.search.twitter.di.module.NetworkModule;
import com.pwittchen.search.twitter.di.module.TwitterModule;
import timber.log.Timber;

public final class BaseApplication extends Application {
  private ApplicationComponent component;

  @Override public void onCreate() {
    super.onCreate();
    buildApplicationComponent();
    plantLoggingTree();
  }

  private void buildApplicationComponent() {
    component = DaggerApplicationComponent.builder()
        .twitterModule(new TwitterModule())
        .networkModule(new NetworkModule())
        .build();
  }

  private void plantLoggingTree() {
    if (BuildConfig.DEBUG) {
      Timber.plant(new Timber.DebugTree());
    } else {
      Timber.plant(new CrashReportingTree());
    }
  }

  public ApplicationComponent getComponent() {
    return component;
  }

  private static class CrashReportingTree extends Timber.Tree {
    @Override protected void log(int priority, String tag, String message, Throwable t) {
      // implement crash reporting with Crashlytics, Bugsnag or whatever if necessary
    }
  }
}
