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
import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import com.github.pwittchen.infinitescroll.library.InfiniteScrollListener;
import com.github.pwittchen.reactivenetwork.library.ConnectivityStatus;
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
  private String lastKeyword = "";
  private LinearLayoutManager layoutManager;

  private Subscription subDelayedSearch;
  private Subscription subSearchTweets;
  private Subscription subLoadMoreTweets;

  @Inject protected TwitterApi twitterApi;
  @Inject protected NetworkApi networkApi;

  @Bind(R.id.recycler_view_tweets) public RecyclerView recyclerViewTweets;
  @Bind(R.id.toolbar) public Toolbar toolbar;
  @Bind(R.id.search_view) public MaterialSearchView searchView;
  @Bind(R.id.message_container) public LinearLayout messageContainerLayout;
  @Bind(R.id.iv_message_container_image) public ImageView imageViewMessage;
  @Bind(R.id.tv_message_container_text) public TextView textViewMessage;
  @Bind(R.id.pb_loading_more_tweets) public ProgressBar progressLoadingMoreTweets;

  @BindString(R.string.no_internet_connection) public String msgNoInternetConnection;
  @BindString(R.string.cannot_load_more_tweets) public String msgCannotLoadMoreTweets;
  @BindString(R.string.no_tweets) public String msgNoTweets;
  @BindString(R.string.no_tweets_formatted) public String msgNoTweetsFormatted;
  @BindString(R.string.searched_formatted) public String msgSearchedFormatted;
  @BindString(R.string.api_rate_limit_exceeded) public String msgApiRateLimitExceeded;
  @BindString(R.string.error_during_search) public String msgErrorDuringSearch;

  @Override protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initInjections();
    initRecyclerView();
    setSupportActionBar(toolbar);
    initSearchView();
    setErrorMessage();
  }

  @Override public boolean onCreateOptionsMenu(final Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    final MenuItem item = menu.findItem(R.id.action_search);
    searchView.setMenuItem(item);
    return true;
  }

  @Override protected void onResume() {
    super.onResume();
    setMessageOnConnectivityChange();
  }

  private void setMessageOnConnectivityChange() {
    networkApi.observeConnectivity(getApplicationContext())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<ConnectivityStatus>() {
          @Override public void call(ConnectivityStatus status) {
            if (messageContainerLayout.getVisibility() == View.VISIBLE) {
              setErrorMessage();
            }
          }
        });
  }

  private void initInjections() {
    ButterKnife.bind(this);
    ((BaseApplication) getApplication()).getComponent().inject(this);
  }

  private void initRecyclerView() {
    recyclerViewTweets.setHasFixedSize(true);
    recyclerViewTweets.setAdapter(new TweetsAdapter(this, new LinkedList<Status>()));
    layoutManager = new LinearLayoutManager(this);
    recyclerViewTweets.setLayoutManager(layoutManager);
    recyclerViewTweets.addOnScrollListener(createInfiniteScrollListener());
  }

  @NonNull private InfiniteScrollListener createInfiniteScrollListener() {
    return new InfiniteScrollListener(twitterApi.getMaxTweetsPerRequest(), layoutManager) {
      @Override public void onScrolledToEnd(final int firstVisibleItemPosition) {
        if (subLoadMoreTweets != null && !subLoadMoreTweets.isUnsubscribed()) {
          return;
        }

        final long lastTweetId = ((TweetsAdapter) recyclerViewTweets.getAdapter()).getLastTweetId();

        subLoadMoreTweets = twitterApi.searchTweets(lastKeyword, lastTweetId)
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
                  showSnackBar(msgNoInternetConnection);
                } else {
                  showSnackBar(msgCannotLoadMoreTweets);
                }
                progressLoadingMoreTweets.setVisibility(View.GONE);
              }

              @Override public void onNext(List<Status> newTweets) {
                final TweetsAdapter newAdapter = createNewTweetsAdapter(newTweets);
                refreshView(recyclerViewTweets, newAdapter, firstVisibleItemPosition);
                progressLoadingMoreTweets.setVisibility(View.GONE);
                unsubscribe();
              }
            });
      }
    };
  }

  @NonNull private TweetsAdapter createNewTweetsAdapter(List<Status> newTweets) {
    final TweetsAdapter adapter = (TweetsAdapter) recyclerViewTweets.getAdapter();
    final List<Status> oldTweets = adapter.getTweets();
    final List<Status> tweets = new LinkedList<>();
    tweets.addAll(oldTweets);
    tweets.addAll(newTweets);
    return new TweetsAdapter(MainActivity.this, tweets);
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

  private void setErrorMessage() {
    if (networkApi.isConnectedToInternet(this)) {
      showErrorMessageContainer(msgNoTweets, R.drawable.no_tweets);
    } else {
      showErrorMessageContainer(msgNoInternetConnection, R.drawable.error);
    }
  }

  private void searchTweetsWithDelay(final String keyword) {
    safelyUnsubscribe(subDelayedSearch);

    if (!twitterApi.canSearchTweets(keyword)) {
      return;
    }

    // we are creating this delay to let user provide keyword
    // and omit not necessary requests
    subDelayedSearch = Observable.timer(1, TimeUnit.SECONDS)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<Long>() {
          @Override public void call(Long milliseconds) {
            searchTweets(keyword);
          }
        });
  }

  private void searchTweets(final String keyword) {
    safelyUnsubscribe(subDelayedSearch, subLoadMoreTweets, subSearchTweets);
    lastKeyword = keyword;

    if (!networkApi.isConnectedToInternet(this)) {
      showSnackBar(msgNoInternetConnection);
      return;
    }

    if (!twitterApi.canSearchTweets(keyword)) {
      return;
    }

    subSearchTweets = twitterApi.searchTweets(keyword)
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
      return msgApiRateLimitExceeded;
    }
    return msgErrorDuringSearch;
  }

  private void handleSearchResults(final List<Status> tweets, final String keyword) {
    if (tweets.isEmpty()) {
      final String message = String.format(msgNoTweetsFormatted, keyword);
      showSnackBar(message);
      showErrorMessageContainer(message, R.drawable.no_tweets);
      return;
    }

    final TweetsAdapter adapter = new TweetsAdapter(MainActivity.this, tweets);
    recyclerViewTweets.setAdapter(adapter);
    recyclerViewTweets.invalidate();
    recyclerViewTweets.setVisibility(View.VISIBLE);
    messageContainerLayout.setVisibility(View.GONE);
    final String message = String.format(msgSearchedFormatted, keyword);
    showSnackBar(message);
  }

  private void showSnackBar(final String message) {
    final View containerId = findViewById(R.id.container);
    Snackbar.make(containerId, message, Snackbar.LENGTH_LONG).show();
  }

  @Override protected void onPause() {
    super.onPause();
    safelyUnsubscribe(subDelayedSearch, subSearchTweets, subLoadMoreTweets);
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