package com.pwittchen.search.twitter.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public final class NetworkApiProvider implements NetworkApi {
  @Override public boolean isConnectedToInternet(Context context) {
    final String service = Context.CONNECTIVITY_SERVICE;
    final ConnectivityManager manager = (ConnectivityManager) context.getSystemService(service);
    final NetworkInfo networkInfo = manager.getActiveNetworkInfo();

    if (networkInfo == null) {
      return false;
    }

    if (networkInfo.getType() == manager.TYPE_WIFI && networkInfo.isConnected()) {
      return true;
    } else if (networkInfo.getType() == manager.TYPE_MOBILE) {
      return true;
    }

    return false;
  }
}
