package com.sbla.agressivescanner;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Created by henrik on 7/10/13.
 */
public class ScannerService extends Service {

  private final int NOTIFICATION_ID = 12332;

  private List<ScanResult> results = Collections.emptyList();
  private final Handler handler = new Handler();


  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    registerReceiver(wifiScanResultReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    registerReceiver(wifiConnectionResultReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    showNotification("Service Started!", "", false);
    handler.post(scanner);

    return Service.START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  final BroadcastReceiver wifiScanResultReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context c, Intent intent) {
      synchronized (results) {
        results = getWifi().getScanResults();

        for (ScanResult res : results) {
          Log.e("Got Result:", res.SSID + " " + res.capabilities + " " + res.level);
        }

        Iterator<ScanResult> it = results.iterator();

        while (it.hasNext()) {
          ScanResult sr = it.next();
          if (sr.capabilities.contains("WPA") || sr.capabilities.contains("WEP")) {
            it.remove();
          }
        }
      }

      if (results.size() > 0) {
        handler.post(connector);
      } else {
        handler.removeCallbacks(scanner);
        handler.postDelayed(scanner, 1000 * 10);
      }
    }
  };

  final BroadcastReceiver wifiConnectionResultReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context c, Intent intent) {
      WifiManager wifi = getWifi();
      showNotification(wifi.getConnectionInfo().getSupplicantState().name(), "", false);

      if (wifi.getConnectionInfo().getSupplicantState().equals(SupplicantState.COMPLETED)) {
        handler.removeCallbacks(scanner);
        showNotification("Connected!", getWifiInfoString(wifi), true);
        testConnection();
      } else {
        handler.post(scanner);
      }

    }
  };

  private String getWifiInfoString(WifiManager wifi) {
    return wifi.getConnectionInfo().getSSID() + "@" + wifi.getConnectionInfo().getLinkSpeed() + WifiInfo.LINK_SPEED_UNITS;
  }

  private final static String mWalledGardenUrl = "http://clients3.google.com/generate_204";

  private void testConnection() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        HttpURLConnection urlConnection = null;
        try {
          URL url = new URL(mWalledGardenUrl); // "http://clients3.google.com/generate_204"
          urlConnection = (HttpURLConnection) url.openConnection();
          urlConnection.setInstanceFollowRedirects(false);
          urlConnection.setConnectTimeout(15 * 1000);
          urlConnection.setReadTimeout(10 * 1000);
          urlConnection.setUseCaches(false);
          urlConnection.getInputStream();

          if (urlConnection.getResponseCode() != 204) {
            vibrate(100);
          } else {
            vibrate(1000);
          }

          Log.e("Connection result:", "Connection result: " + urlConnection.getResponseCode());
        } catch (IOException e) {
          //Test connection failed...
          Log.e("Connection result:", "Failed to connect!");
        } finally {
          if (urlConnection != null) {
            urlConnection.disconnect();
          }
        }
      }
    }).start();

  }

  private void vibrate(final long millis) {
    handler.post(new Runnable() {
      @Override
      public void run() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(millis);
      }
    });
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(wifiScanResultReceiver);
    unregisterReceiver(wifiConnectionResultReceiver);
    handler.removeCallbacks(scanner);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancelAll();
    super.onDestroy();
  }

  final Runnable scanner = new Runnable() {
    public void run() {
      if (!getWifi().getConnectionInfo().getSupplicantState().equals(SupplicantState.COMPLETED)) {
        getWifi().startScan();
        showNotification("Scan Started!", "", false);
      }
      handler.postDelayed(this, 15 * 1000);
    }
  };

  final Runnable connector = new Runnable() {
    public void run() {
      if (!getWifi().getConnectionInfo().getSupplicantState().equals(SupplicantState.COMPLETED)) {
        synchronized (results) {
          Collections.sort(results, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult s1, ScanResult s2) {
              return Integer.valueOf(s1.level).compareTo(s2.level);
            }
          });

          showNotification(results);

          if (results.size() > 0) {
            ScanResult best = results.get(0);
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + best.SSID + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            getWifi().addNetwork(config);

            List<WifiConfiguration> list = getWifi().getConfiguredNetworks();
            for (WifiConfiguration conf : list) {
              if (conf.SSID != null && conf.SSID.contains(best.SSID)) {
                getWifi().enableNetwork(conf.networkId, true);
              }
            }
          }

        }
      }
    }
  };

  public void showNotification(String message, String submessage, boolean greenflash) {
    NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(message)
                    .setContentText(submessage)
                    .setLights(Color.YELLOW, 100, 100)
                    .setOngoing(true);

    if (greenflash) {
      mBuilder.setLights(Color.GREEN, 1000, 10);
    }

    Intent resultIntent = new Intent(this, MainActivity.class);
    PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, resultIntent, 0);

    mBuilder.setContentIntent(resultPendingIntent);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancelAll();
    manager.notify(NOTIFICATION_ID, mBuilder.build());
  }

  public void showNotification(List<ScanResult> list) {
    NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("Got Scan Results")
                    .setContentText(list.size() + " results...")
                    .setLights(Color.YELLOW, 100, 100)
                    .setOngoing(true);


    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
    inboxStyle.setBigContentTitle("Scan Results:");
    for (ScanResult res : list) {

      inboxStyle.addLine(res.SSID + " " + res.level);
    }
    mBuilder.setStyle(inboxStyle);


    Intent resultIntent = new Intent(this, MainActivity.class);
    PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, resultIntent, 0);

    mBuilder.setContentIntent(resultPendingIntent);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    manager.cancelAll();
    manager.notify(NOTIFICATION_ID, mBuilder.build());
  }

  public WifiManager getWifi() {
    return (WifiManager) getSystemService(Context.WIFI_SERVICE);
  }
}
