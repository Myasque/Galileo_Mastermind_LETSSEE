//
//import android.content.Context;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;

package com.example.galileomastermindboilerplate;

import android.app.Activity;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;


import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LogFragment extends MainActivity implements MeasurementListener {

    public Activity activity;

    private List<String> mData;
    private List<Integer>mIcon;
    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;
   public LogFragment( Activity _activity){

        this.activity = _activity;
       mData = new ArrayList<>();
       mIcon = new ArrayList<>();

    }


    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {

    }


    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

        StringBuilder builder = new StringBuilder("");

//        mData = new ArrayList<>();
//        mIcon = new ArrayList<>();

        activity.runOnUiThread(() -> {

            // Find RecyclerView by ID
            recyclerView = activity.findViewById(R.id.main_list);

//                mData = new ArrayList<>();
//                mIcon = new ArrayList<>();
            // Set layout manager
            adapter = new RecyclerViewAdapter(mData, mIcon);
            recyclerView.setAdapter(adapter);


            for (GnssMeasurement measurement : event.getMeasurements()) {
                String tempString = toStringMeasurement(measurement);
                builder.append(tempString);
                builder.append("\n");


                mData.add(builder.toString());
                mIcon.add(R.drawable.rawmeas);

                adapter.notifyDataSetChanged();


                String jsonString = convertFormattedArrayToJson(tempString);
                OkHttpClient client = new OkHttpClient();
                MediaType JSON = MediaType.get("application/json; charset=utf-8");

                // create RequestBody
                RequestBody body = RequestBody.create(jsonString, JSON);

                // create request
                Request request = new Request.Builder()
                        .url("http://172.20.10.12:5000/gnss_data")
                        .post(body)
                        .build();
                Log.d("Request","sending request: " + jsonString);


                // Send request asynchronously
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        e.printStackTrace();
                        Log.d("Lost","failed to send: ");

                    }

                    @Override
                    public void onResponse(okhttp3.Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            // 在这里处理响应
                            String responseData = response.body().string();
                            Log.d("Success","send and receive: " + responseData);
                            System.out.println(responseData);
                        }
                    }
                });

            }
            //String myJsonString = convertFormattedArrayToJson(mData);
            recyclerView.scrollToPosition(adapter.getItemCount() -1);

        });





    }


    public String convertFormattedArrayToJson(String data) {

       JSONObject jsonObject = new JSONObject();
        String[] lines = data.split("\n");

       for (String myLine : lines){
           myLine = myLine.trim();
           String[] parts = myLine.split("=", 2);
           if (parts.length == 2) {
               String key = parts[0].trim();  // 清理键的空白符
               String value = parts[1].trim(); // 清理值的空白符
               try {
                   jsonObject.put(key, value);
               } catch (JSONException e) {
                   throw new RuntimeException(e);
               }
           }
       }

        String TAG = "Convert";
        Log.d(TAG, "Converted jsonObject: " + jsonObject);

        // 将JSONObject转换为String并返回
        return jsonObject.toString();
    }


    private String toStringMeasurement(GnssMeasurement measurement) {
        final String format = "   %-4s = %s\n";
        StringBuilder builder = new StringBuilder("GnssMeasurement:\n");
        DecimalFormat numberFormat = new DecimalFormat("#0.000");
        DecimalFormat numberFormat1 = new DecimalFormat("#0.000E00");
        builder.append(String.format(format, "Svid", measurement.getSvid()));
        builder.append(String.format(format, "ConstellationType", measurement.getConstellationType()));
        builder.append(String.format(format, "TimeOffsetNanos", measurement.getTimeOffsetNanos()));

        builder.append(String.format(format, "State", measurement.getState()));

        builder.append(
                String.format(format, "ReceivedSvTimeNanos", measurement.getReceivedSvTimeNanos()));
        builder.append(
                String.format(
                        format,
                        "ReceivedSvTimeUncertaintyNanos",
                        measurement.getReceivedSvTimeUncertaintyNanos()));

        builder.append(String.format(format, "Cn0DbHz", numberFormat.format(measurement.getCn0DbHz())));

        builder.append(
                String.format(
                        format,
                        "PseudorangeRateMetersPerSecond",
                        numberFormat.format(measurement.getPseudorangeRateMetersPerSecond())));
        builder.append(
                String.format(
                        format,
                        "PseudorangeRateUncertaintyMetersPerSeconds",
                        numberFormat.format(measurement.getPseudorangeRateUncertaintyMetersPerSecond())));

        if (measurement.getAccumulatedDeltaRangeState() != GnssMeasurement.ADR_STATE_UNKNOWN) {
            builder.append(
                    String.format(
                            format, "AccumulatedDeltaRangeState", measurement.getAccumulatedDeltaRangeState()));

            builder.append(
                    String.format(
                            format,
                            "AccumulatedDeltaRangeMeters",
                            numberFormat.format(measurement.getAccumulatedDeltaRangeMeters())));
            builder.append(
                    String.format(
                            format,
                            "AccumulatedDeltaRangeUncertaintyMeters",
                            numberFormat1.format(measurement.getAccumulatedDeltaRangeUncertaintyMeters())));
        }

        if (measurement.hasCarrierFrequencyHz()) {
            builder.append(
                    String.format(format, "CarrierFrequencyHz", measurement.getCarrierFrequencyHz()));
        }

        if (measurement.hasCarrierCycles()) {
            builder.append(String.format(format, "CarrierCycles", measurement.getCarrierCycles()));
        }

        if (measurement.hasCarrierPhase()) {
            builder.append(String.format(format, "CarrierPhase", measurement.getCarrierPhase()));
        }

        if (measurement.hasCarrierPhaseUncertainty()) {
            builder.append(
                    String.format(
                            format, "CarrierPhaseUncertainty", measurement.getCarrierPhaseUncertainty()));
        }

        builder.append(
                String.format(format, "MultipathIndicator", measurement.getMultipathIndicator()));

        if (measurement.hasSnrInDb()) {
            builder.append(String.format(format, "SnrInDb", measurement.getSnrInDb()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(measurement.hasFullInterSignalBiasNanos()){
                builder.append(
                        String.format(format, "FullInterSignalBiasNanos", measurement.getFullInterSignalBiasNanos()));

            }
            if(measurement.hasFullInterSignalBiasUncertaintyNanos()){
                builder.append(
                        String.format(format, "FullInterSignalBiasUncertaintyNanos", measurement.getFullInterSignalBiasUncertaintyNanos()));

            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (measurement.hasAutomaticGainControlLevelDb()) {
                builder.append(
                        String.format(format, "AgcDb", measurement.getAutomaticGainControlLevelDb()));
            }
            if (measurement.hasCarrierFrequencyHz()) {
                builder.append(String.format(format, "CarrierFreqHz", measurement.getCarrierFrequencyHz()));
            }
        }

        return builder.toString();
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {

    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {


        // here
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {

    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {

    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {

    }

    @Override
    public void onNmeaReceived(long l, String s) {

    }

    @Override
    public void onTTFFReceived(long l) {

    }

}


