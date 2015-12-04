package com.pwittchen.search.twitter;

import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.LinearLayoutManager;
import com.pwittchen.search.twitter.twitter.TwitterApiProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class) public class TwitterApiProviderTest {

  @Test public void testCanLoadMoreTweetsLogicIsCorrect() {
    // given
    TwitterApiProvider twitterApiProvider = new TwitterApiProvider();
    LinearLayoutManager manager = Mockito.mock(LinearLayoutManager.class);
    int visibleItemsCount = 10;
    int totalItemsCount = 150;
    int pastVisibleItemsCount = 20;
    when(manager.getChildCount()).thenReturn(visibleItemsCount);
    when(manager.getItemCount()).thenReturn(totalItemsCount);
    when(manager.findFirstVisibleItemPosition()).thenReturn(pastVisibleItemsCount);
    boolean lastItemShown = visibleItemsCount + pastVisibleItemsCount > totalItemsCount;
    int tweetsPerRequest = 100;
    boolean canLoadMoreTweetsExpected = lastItemShown && totalItemsCount >= tweetsPerRequest;

    // when
    boolean canLoadMoreTweets = twitterApiProvider.canLoadMoreTweets(manager, tweetsPerRequest);

    // then
    assertThat(canLoadMoreTweets).isEqualTo(canLoadMoreTweetsExpected);
  }

  @Test public void testCanLoadMoreTweetsShouldBeTrue() {
    // given
    TwitterApiProvider twitterApiProvider = new TwitterApiProvider();
    LinearLayoutManager manager = Mockito.mock(LinearLayoutManager.class);
    int visibleItemsCount = 10;
    int totalItemsCount = 20;
    int pastVisibleItemsCount = 15;
    when(manager.getChildCount()).thenReturn(visibleItemsCount);
    when(manager.getItemCount()).thenReturn(totalItemsCount);
    when(manager.findFirstVisibleItemPosition()).thenReturn(pastVisibleItemsCount);
    int tweetsPerRequest = 10;

    // when
    boolean canLoadMoreTweets = twitterApiProvider.canLoadMoreTweets(manager, tweetsPerRequest);

    // then
    assertThat(canLoadMoreTweets).isTrue();
  }

  @Test public void testCanLoadMoreTweetsShouldBeFalse() {
    // given
    TwitterApiProvider twitterApiProvider = new TwitterApiProvider();
    LinearLayoutManager manager = Mockito.mock(LinearLayoutManager.class);
    int visibleItemsCount = 10;
    int totalItemsCount = 30;
    int pastVisibleItemsCount = 15;
    when(manager.getChildCount()).thenReturn(visibleItemsCount);
    when(manager.getItemCount()).thenReturn(totalItemsCount);
    when(manager.findFirstVisibleItemPosition()).thenReturn(pastVisibleItemsCount);
    int tweetsPerRequest = 100;

    // when
    boolean canLoadMoreTweets = twitterApiProvider.canLoadMoreTweets(manager, tweetsPerRequest);

    // then
    assertThat(canLoadMoreTweets).isFalse();
  }

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
