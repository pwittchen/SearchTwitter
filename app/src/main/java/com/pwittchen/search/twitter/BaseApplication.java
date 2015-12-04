package com.pwittchen.search.twitter;

import android.app.Application;
import com.pwittchen.search.twitter.di.ApplicationComponent;
import com.pwittchen.search.twitter.di.DaggerApplicationComponent;
import com.pwittchen.search.twitter.di.module.TwitterModule;

public final class BaseApplication extends Application {
  private ApplicationComponent component;

  @Override public void onCreate() {
    super.onCreate();
    buildApplicationComponent();
  }

  private void buildApplicationComponent() {
    component = DaggerApplicationComponent.builder().twitterModule(new TwitterModule()).build();
  }

  public ApplicationComponent getComponent() {
    return component;
  }
}
