package com.pwittchen.search.twitter;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import com.pwittchen.search.twitter.ui.TweetsAdapter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import twitter4j.Status;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class) public class TweetsAdapterTest {

  @Test public void testAdapterShouldHaveGivenNumberOfTweets() {
    // given
    Context context = InstrumentationRegistry.getContext();
    List<Status> tweets = new ArrayList<>();
    Status status = Mockito.mock(Status.class);
    tweets.add(status);

    // when
    TweetsAdapter tweetsAdapter = new TweetsAdapter(context, tweets);

    // then
    assertThat(tweetsAdapter.getItemCount()).isEqualTo(tweets.size());
  }

  @Test public void testLastTweetIdShouldReturnCorrectValue() {
    // given
    Context context = InstrumentationRegistry.getContext();
    List<Status> tweets = new ArrayList<>();
    Status status = Mockito.mock(Status.class);
    final long givenLastTweetId = 123L;
    Mockito.when(status.getId()).thenReturn(givenLastTweetId);
    tweets.add(status);

    // when
    TweetsAdapter tweetsAdapter = new TweetsAdapter(context, tweets);

    // then
    assertThat(tweetsAdapter.getLastTweetId()).isEqualTo(givenLastTweetId);
  }

  @Test public void getTweetsMethodShouldReturnTheSameAmountOfTweetsAsPassedToAdapter() {
    // given
    Context context = InstrumentationRegistry.getContext();
    List<Status> tweets = new ArrayList<>();
    Status status = Mockito.mock(Status.class);
    tweets.add(status);
    tweets.add(status);

    // when
    TweetsAdapter tweetsAdapter = new TweetsAdapter(context, tweets);

    // then
    assertThat(tweetsAdapter.getTweets().size()).isEqualTo(tweets.size());
  }
}
