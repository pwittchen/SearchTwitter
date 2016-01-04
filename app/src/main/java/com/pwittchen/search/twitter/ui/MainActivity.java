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
  private LinearLayoutManager recycleViewLinearLayoutManager;
  private Subscription delayedSearchSubscription;
  private Subscription searchTweetsSubscription;
  private Subscription loadMoreTweetsSubscription;
  @Inject protected TwitterApi twitterApi;
  @Inject protected NetworkApi networkApi;
  @InjectView(R.id.recycle_view_tweets) public RecyclerView recyclerViewTweets;
  @InjectView(R.id.toolbar) public Toolbar toolbar;
  @InjectView(R.id.search_view) public MaterialSearchView searchView;
  @InjectView(R.id.message_container) public LinearLayout messageContainer;
  @InjectView(R.id.iv_message_container_image) public ImageView messageImage;
  @InjectView(R.id.tv_message_container_text) public TextView messageText;
  @InjectView(R.id.tv_loading_more_tweets) public TextView loadingMoreTweets;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initInjections();
    initRecycleView();
    setSupportActionBar(toolbar);
    initSearchView();
    initMessageContainer();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    MenuItem item = menu.findItem(R.id.action_search);
    searchView.setMenuItem(item);
    return true;
  }

  private void initInjections() {
    ButterKnife.inject(this);
    ((BaseApplication) getApplication()).getComponent().inject(this);
  }

  private void initRecycleView() {
    recyclerViewTweets.setHasFixedSize(true);
    recyclerViewTweets.setAdapter(new TweetsAdapter(this, new LinkedList<Status>()));
    recycleViewLinearLayoutManager = new LinearLayoutManager(this);
    recyclerViewTweets.setLayoutManager(recycleViewLinearLayoutManager);
    setInfiniteScrollListener();
  }

  @SuppressWarnings("deprecation") private void setInfiniteScrollListener() {
    recyclerViewTweets.setOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override public void onScrolled(final RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        int maxTweetsPerRequest = twitterApi.getMaxTweetsPerRequest();
        if (twitterApi.canLoadMoreTweets(recycleViewLinearLayoutManager, maxTweetsPerRequest)) {
          loadMoreTweets();
        }
      }
    });
  }

  private void loadMoreTweets() {
    if (loadMoreTweetsSubscription != null && !loadMoreTweetsSubscription.isUnsubscribed()) {
      return;
    }

    long lastTweetId = ((TweetsAdapter) recyclerViewTweets.getAdapter()).getLastTweetId();

    loadMoreTweetsSubscription = twitterApi.searchTweets(lastKeyword, lastTweetId)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<Status>>() {
          @Override public void onStart() {
            loadingMoreTweets.setVisibility(View.VISIBLE);
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
            loadingMoreTweets.setVisibility(View.GONE);
          }

          @Override public void onNext(List<Status> newTweets) {
            handleLoadMoreTweets(newTweets, (TweetsAdapter) recyclerViewTweets.getAdapter());
            loadingMoreTweets.setVisibility(View.GONE);
            unsubscribe();
          }
        });
  }

  private void handleLoadMoreTweets(List<Status> newTweets, TweetsAdapter tweetsAdapter) {
    List<Status> oldTweets = tweetsAdapter.getTweets();
    List<Status> tweets = new LinkedList<>();
    tweets.addAll(oldTweets);
    tweets.addAll(newTweets);
    TweetsAdapter adapter = new TweetsAdapter(MainActivity.this, tweets);
    int lastPosition = recycleViewLinearLayoutManager.findFirstVisibleItemPosition();
    recyclerViewTweets.setAdapter(adapter);
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
    unsubscribe(delayedSearchSubscription);

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
    unsubscribe(delayedSearchSubscription, loadMoreTweetsSubscription, searchTweetsSubscription);
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

          @Override public void onError(Throwable e) {
            String message = getErrorMessage((TwitterException) e);
            showSnackBar(message);
            showErrorMessageContainer(message, R.drawable.no_tweets);
          }

          @Override public void onNext(final List<Status> tweets) {
            handleSearchResults(tweets, keyword);
          }
        });
  }

  @NonNull private String getErrorMessage(TwitterException e) {
    String message = getString(R.string.error_during_search);
    if (e.getErrorCode() == twitterApi.getApiRateLimitExceededErrorCode()) {
      message = getString(R.string.api_rate_limit_exceeded);
    }
    return message;
  }

  private void handleSearchResults(final List<Status> tweets, final String keyword) {
    if (tweets.isEmpty()) {
      String message = String.format(getString(R.string.no_tweets_formatted), keyword);
      showSnackBar(message);
      showErrorMessageContainer(message, R.drawable.no_tweets);
      return;
    }

    TweetsAdapter adapter = new TweetsAdapter(MainActivity.this, tweets);
    recyclerViewTweets.setAdapter(adapter);
    recyclerViewTweets.invalidate();
    recyclerViewTweets.setVisibility(View.VISIBLE);
    messageContainer.setVisibility(View.GONE);
    String message = String.format(getString(R.string.searched_formatted), keyword);
    showSnackBar(message);
  }

  private void showSnackBar(String message) {
    View containerId = findViewById(R.id.container);
    Snackbar.make(containerId, message, Snackbar.LENGTH_LONG).show();
  }

  @Override protected void onPause() {
    super.onPause();
    unsubscribe(delayedSearchSubscription, searchTweetsSubscription, loadMoreTweetsSubscription);
  }

  private void unsubscribe(Subscription... subscriptions) {
    for (Subscription subscription : subscriptions) {
      if (subscription != null && !subscription.isUnsubscribed()) {
        subscription.unsubscribe();
        subscription = null;
      }
    }
  }

  private void showErrorMessageContainer(String message, int imageResourceId) {
    recyclerViewTweets.setVisibility(View.GONE);
    messageContainer.setVisibility(View.VISIBLE);
    messageImage.setImageResource(imageResourceId);
    messageText.setText(message);
  }

  @Override public void onBackPressed() {
    if (searchView.isSearchOpen()) {
      searchView.closeSearch();
    } else {
      super.onBackPressed();
    }
  }
}