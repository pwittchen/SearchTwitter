package com.pwittchen.search.twitter.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.pwittchen.search.twitter.BaseApplication;
import com.pwittchen.search.twitter.R;
import com.pwittchen.search.twitter.network.NetworkApi;
import com.pwittchen.search.twitter.twitter.TwitterApi;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import twitter4j.Status;
import twitter4j.TwitterException;

public final class MainActivity extends AppCompatActivity {
  private static final String EMPTY_STRING = "";
  private String lastKeyword = EMPTY_STRING;
  private LinearLayoutManager recyclerViewLayoutManager;
  private Subscription delayedSearchSubscription;
  private Subscription searchTweetsSubscription;
  private Subscription loadMoreTweetsSubscription;
  @Inject protected TwitterApi twitterApi;
  @Inject protected NetworkApi networkApi;
  @InjectView(R.id.recycler_view_tweets) public RecyclerView recyclerViewTweets;
  @InjectView(R.id.toolbar) public Toolbar toolbar;
  @InjectView(R.id.search_view) public MaterialSearchView searchView;
  @InjectView(R.id.message_container) public LinearLayout messageContainerLayout;
  @InjectView(R.id.iv_message_container_image) public ImageView imageViewMessage;
  @InjectView(R.id.tv_message_container_text) public TextView textViewMessage;
  @InjectView(R.id.pb_loading_more_tweets) public ProgressBar progressLoadingMoreTweets;

  @Override protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initInjections();
    initRecyclerView();
    setSupportActionBar(toolbar);
    initSearchView();
    initMessageContainer();
  }

  @Override public boolean onCreateOptionsMenu(final Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    final MenuItem item = menu.findItem(R.id.action_search);
    searchView.setMenuItem(item);
    return true;
  }

  private void initInjections() {
    ButterKnife.inject(this);
    ((BaseApplication) getApplication()).getComponent().inject(this);
  }

  private void initRecyclerView() {
    recyclerViewTweets.setHasFixedSize(true);
    recyclerViewTweets.setAdapter(new TweetsAdapter(this, new LinkedList<Status>()));
    recyclerViewLayoutManager = new LinearLayoutManager(this);
    recyclerViewTweets.setLayoutManager(recyclerViewLayoutManager);
    setInfiniteScrollListener();
  }

  @SuppressWarnings("deprecation") private void setInfiniteScrollListener() {
    recyclerViewTweets.setOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy) {
        super.onScrolled(recyclerView, dx, dy);
        final int maxTweetsPerRequest = twitterApi.getMaxTweetsPerRequest();
        if (twitterApi.canLoadMoreTweets(recyclerViewLayoutManager, maxTweetsPerRequest)) {
          loadMoreTweets();
        }
      }
    });
  }

  private void loadMoreTweets() {
    if (loadMoreTweetsSubscription != null && !loadMoreTweetsSubscription.isUnsubscribed()) {
      return;
    }

    final long lastTweetId = ((TweetsAdapter) recyclerViewTweets.getAdapter()).getLastTweetId();

    loadMoreTweetsSubscription = twitterApi.searchTweets(lastKeyword, lastTweetId)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<Status>>() {
          @Override public void onStart() {
            progressLoadingMoreTweets.setVisibility(View.VISIBLE);
          }

          @Override public void onCompleted() {
            // we don't have to implement this
          }

          @Override public void onError(Throwable e) {
            if (!networkApi.isConnectedToInternet(MainActivity.this)) {
              showSnackBar(getString(R.string.no_internet_connection));
            } else {
              showSnackBar(getString(R.string.cannot_load_more_tweets));
            }
            progressLoadingMoreTweets.setVisibility(View.GONE);
          }

          @Override public void onNext(List<Status> newTweets) {
            handleLoadMoreTweets(newTweets, (TweetsAdapter) recyclerViewTweets.getAdapter());
            progressLoadingMoreTweets.setVisibility(View.GONE);
            unsubscribe();
          }
        });
  }

  private void handleLoadMoreTweets(final List<Status> newTweets, final TweetsAdapter adapter) {
    final List<Status> oldTweets = adapter.getTweets();
    final List<Status> tweets = new LinkedList<>();
    tweets.addAll(oldTweets);
    tweets.addAll(newTweets);
    final TweetsAdapter newAdapter = new TweetsAdapter(MainActivity.this, tweets);
    final int lastPosition = recyclerViewLayoutManager.findFirstVisibleItemPosition();
    recyclerViewTweets.setAdapter(newAdapter);
    recyclerViewTweets.invalidate();
    recyclerViewTweets.scrollToPosition(lastPosition);
  }

  private void initSearchView() {
    searchView.setVoiceSearch(false);
    searchView.setCursorDrawable(R.drawable.search_view_cursor);
    searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
      @Override public boolean onQueryTextSubmit(final String query) {
        searchTweets(query);
        return false;
      }

      @Override public boolean onQueryTextChange(String newText) {
        searchTweetsWithDelay(newText);
        return false;
      }
    });
  }

  private void initMessageContainer() {
    if (networkApi.isConnectedToInternet(this)) {
      showErrorMessageContainer(getString(R.string.no_tweets), R.drawable.no_tweets);
    } else {
      showErrorMessageContainer(getString(R.string.no_internet_connection), R.drawable.error);
    }
  }

  private void searchTweetsWithDelay(final String keyword) {
    safelyUnsubscribe(delayedSearchSubscription);

    if (!twitterApi.canSearchTweets(keyword)) {
      return;
    }

    // we are creating this delay to let user provide keyword
    // and omit not necessary requests
    delayedSearchSubscription = Observable.timer(1, TimeUnit.SECONDS)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Long>() {
          @Override public void call(Long milliseconds) {
            searchTweets(keyword);
          }
        });
  }

  private void searchTweets(final String keyword) {
    safelyUnsubscribe(delayedSearchSubscription, loadMoreTweetsSubscription,
        searchTweetsSubscription);
    lastKeyword = keyword;

    if (!networkApi.isConnectedToInternet(this)) {
      showSnackBar(getString(R.string.no_internet_connection));
      return;
    }

    if (!twitterApi.canSearchTweets(keyword)) {
      return;
    }

    searchTweetsSubscription = twitterApi.searchTweets(keyword)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<Status>>() {
          @Override public void onCompleted() {
            // we don't have to implement this method
          }

          @Override public void onError(final Throwable e) {
            final String message = getErrorMessage((TwitterException) e);
            showSnackBar(message);
            showErrorMessageContainer(message, R.drawable.no_tweets);
          }

          @Override public void onNext(final List<Status> tweets) {
            handleSearchResults(tweets, keyword);
          }
        });
  }

  @NonNull private String getErrorMessage(final TwitterException e) {
    if (e.getErrorCode() == twitterApi.getApiRateLimitExceededErrorCode()) {
      return getString(R.string.api_rate_limit_exceeded);
    }
    return getString(R.string.error_during_search);
  }

  private void handleSearchResults(final List<Status> tweets, final String keyword) {
    if (tweets.isEmpty()) {
      final String message = String.format(getString(R.string.no_tweets_formatted), keyword);
      showSnackBar(message);
      showErrorMessageContainer(message, R.drawable.no_tweets);
      return;
    }

    final TweetsAdapter adapter = new TweetsAdapter(MainActivity.this, tweets);
    recyclerViewTweets.setAdapter(adapter);
    recyclerViewTweets.invalidate();
    recyclerViewTweets.setVisibility(View.VISIBLE);
    messageContainerLayout.setVisibility(View.GONE);
    final String message = String.format(getString(R.string.searched_formatted), keyword);
    showSnackBar(message);
  }

  private void showSnackBar(final String message) {
    final View containerId = findViewById(R.id.container);
    Snackbar.make(containerId, message, Snackbar.LENGTH_LONG).show();
  }

  @Override protected void onPause() {
    super.onPause();
    safelyUnsubscribe(delayedSearchSubscription, searchTweetsSubscription,
        loadMoreTweetsSubscription);
  }

  private void safelyUnsubscribe(final Subscription... subscriptions) {
    for (Subscription subscription : subscriptions) {
      if (subscription != null && !subscription.isUnsubscribed()) {
        subscription.unsubscribe();
      }
    }
  }

  private void showErrorMessageContainer(final String message, final int imageResourceId) {
    recyclerViewTweets.setVisibility(View.GONE);
    messageContainerLayout.setVisibility(View.VISIBLE);
    imageViewMessage.setImageResource(imageResourceId);
    textViewMessage.setText(message);
  }

  @Override public void onBackPressed() {
    if (searchView.isSearchOpen()) {
      searchView.closeSearch();
    } else {
      super.onBackPressed();
    }
  }
}