package com.pwittchen.search.twitter.di;

import com.pwittchen.search.twitter.di.module.NetworkModule;
import com.pwittchen.search.twitter.di.module.TwitterModule;
import com.pwittchen.search.twitter.ui.MainActivity;
import dagger.Component;
import javax.inject.Singleton;

@Singleton @Component(modules = { TwitterModule.class, NetworkModule.class })
public interface ApplicationComponent {
  void inject(MainActivity mainActivity);
}
