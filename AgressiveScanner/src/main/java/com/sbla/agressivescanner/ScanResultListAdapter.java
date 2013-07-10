package com.sbla.agressivescanner;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by henrik on 7/10/13.
 */
public class ScanResultListAdapter extends ArrayAdapter<ScanResult> {
  private final List<ScanResult> list;

  public ScanResultListAdapter(Context context, List<ScanResult> list) {
    super(context, R.layout.scanrow, list);
    this.list = list;
  }

  static class ViewHolder {
    protected TextView ssid;
    protected TextView level;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      LayoutInflater inflator = LayoutInflater.from(getContext());
      view = inflator.inflate(R.layout.scanrow, null);
      final ViewHolder viewHolder = new ViewHolder();
      viewHolder.ssid = (TextView) view.findViewById(R.id.ssid);
      viewHolder.level = (TextView) view.findViewById(R.id.level);

      view.setTag(viewHolder);
    }
    ViewHolder holder = (ViewHolder) view.getTag();
    holder.ssid.setText(list.get(position).SSID);
    holder.level.setText(String.valueOf(list.get(position).level));
    return view;
  }
}
