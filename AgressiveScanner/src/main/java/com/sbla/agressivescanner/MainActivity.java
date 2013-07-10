package com.sbla.agressivescanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity {

  BroadcastReceiver wifiScanResultReceiver;

  List<ScanResult> results = Collections.emptyList();

  ScanResultListAdapter adapter;
  ListView scanResultsListView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.scan).setOnClickListener(getOnScanClickListener());

    findViewById(R.id.startservice).setOnClickListener(getOnStartServiceClickListener());
    findViewById(R.id.stopservice).setOnClickListener(getOnStopServiceClickListener());

    scanResultsListView = (ListView) findViewById(R.id.accesspointlist);

  }

  private View.OnClickListener getOnStopServiceClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Context context = MainActivity.this;
        Intent service = new Intent(context, ScannerService.class);
        context.stopService(service);
      }
    };
  }

  private View.OnClickListener getOnStartServiceClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Context context = MainActivity.this;
        Intent service = new Intent(context, ScannerService.class);
        context.startService(service);
      }
    };
  }

  private void registerWifiScanResultReceiver() {
    wifiScanResultReceiver = new BroadcastReceiver() {
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

          scanResultsListView.setAdapter(new ScanResultListAdapter(MainActivity.this, results));
        }
      }
    };

    registerReceiver(wifiScanResultReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
  }

  @Override
  protected void onStart() {
    super.onStart();
    registerWifiScanResultReceiver();
  }

  @Override
  protected void onPause() {
    super.onPause();
    unregisterReceiver(wifiScanResultReceiver);
  }

  private View.OnClickListener getOnScanClickListener() {
    return new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getWifi().startScan();
      }
    };
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  public WifiManager getWifi() {
    return (WifiManager) getSystemService(Context.WIFI_SERVICE);
  }

}
