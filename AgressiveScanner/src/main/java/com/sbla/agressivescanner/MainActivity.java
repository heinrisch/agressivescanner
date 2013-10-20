package com.sbla.agressivescanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.startservice).setOnClickListener(getOnStartServiceClickListener());
    findViewById(R.id.stopservice).setOnClickListener(getOnStopServiceClickListener());
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

}
