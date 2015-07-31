package com.scd.samtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by sdickson on 4/20/2015.
 */
public class TrackReceiver extends BroadcastReceiver
{
    public static final int REQUEST_CODE = 12345;

    public void onReceive(Context context, Intent intent) {
        Log.d("samtrack", "Received tick. Getting location...");
        Intent i = new Intent(context, TrackService.class);
        context.startService(i);
    }
}
