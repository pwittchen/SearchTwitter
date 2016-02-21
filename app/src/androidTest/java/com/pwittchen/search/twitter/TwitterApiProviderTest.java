package com.pwittchen.search.twitter;

import android.support.test.runner.AndroidJUnit4;
import com.pwittchen.search.twitter.twitter.TwitterApiProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class) public class TwitterApiProviderTest {

  @Test public void testCanSearchTweetsShouldBeTrue() {
    // given
    TwitterApiProvider twitterApiProvider = new TwitterApiProvider();
    String sampleKeyword = "sampleKeyword";

    // when
    boolean canSearchTweets = twitterApiProvider.canSearchTweets(sampleKeyword);

    // then
    assertThat(canSearchTweets).isTrue();
  }

  @Test public void testCanSearchTweetsShouldBeFalse() {
    // given
    TwitterApiProvider twitterApiProvider = new TwitterApiProvider();
    String emptyString = "";

    // when
    boolean canSearchTweets = twitterApiProvider.canSearchTweets(emptyString);

    // then
    assertThat(canSearchTweets).isFalse();
  }
}
