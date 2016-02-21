package com.pwittchen.search.twitter.network;

import android.content.Context;
import com.github.pwittchen.reactivenetwork.library.ConnectivityStatus;
import rx.Observable;

public interface NetworkApi {
  boolean isConnectedToInternet(Context context);

  Observable<ConnectivityStatus> observeConnectivity(final Context context);
}
