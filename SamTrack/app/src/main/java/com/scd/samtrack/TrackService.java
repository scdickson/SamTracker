package com.scd.samtrack;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.CalendarContract;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by sdickson on 4/20/2015.
 */
public class TrackService extends Service implements LocationListener
{
    private LocationManager locationManager;
    private PowerManager.WakeLock wakeLock;
    private boolean currentlyProcessingLocation = false;

    public void onCreate()
    {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "LocationUpdaterService");
    }

    public void onDestroy()
    {
        super.onDestroy();

        if (this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
    }
    public IBinder onBind(Intent intent) {
        return null;
    }


    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (!currentlyProcessingLocation) {
            currentlyProcessingLocation = true;
            startTracking();
        }
        return START_NOT_STICKY;
    }

    public void startTracking()
    {
        //wakeLock.acquire();

        if (locationManager == null) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            String locationProvider = LocationManager.GPS_PROVIDER;
            if(!MainActivity.USE_GPS)
            {
                locationProvider = LocationManager.NETWORK_PROVIDER;
            }
            locationManager.requestLocationUpdates(locationProvider, 30 * 1000, 0, this);
        }
    }


    public void onStatusChanged(String provider, int status, Bundle extras) {}
    public void onProviderEnabled(String provider) {}
    public void onProviderDisabled(String provider) {}

    public void onLocationChanged(final Location location)
    {
        try
        {
            final Bundle bundle = new Bundle();
            final HashMap<String, Integer> cityLocations = new HashMap<String, Integer>();

            ParseQuery<ParseObject> query = ParseQuery.getQuery("PageView");
            query.findInBackground(new FindCallback<ParseObject>() {
                public void done(List<ParseObject> pageViewList, ParseException e) {
                    if (e == null) {
                        for (ParseObject obj : pageViewList) {
                            String cityName = obj.getString("CityName");

                            if (cityLocations.containsKey(cityName))
                            {
                                Integer i = cityLocations.get(cityName);
                                cityLocations.put(cityName, i+1);
                            }
                            else
                            {
                                cityLocations.put(cityName, 1);
                            }
                        }
                        bundle.putSerializable("city_locations", cityLocations);
                        bundle.putInt("num_views", pageViewList.size());
                        bundle.putString("message", "Sending location: " + location.getLatitude() + ", " + location.getLongitude() + "\n");

                        Message message = MainActivity.handler.obtainMessage();
                        message.setData(bundle);
                        MainActivity.handler.sendMessage(message);
                    } else {
                        e.printStackTrace();
                    }
                }
            });


            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(300);

            Log.d("scdtrack", location.toString());
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getDefault());
            Date date = new Date(location.getTime());
            ParseObject locObject = new ParseObject("LocationObject");
            locObject.put("time", dateFormat.format(date));
            locObject.put("lat", location.getLatitude());
            locObject.put("lng", location.getLongitude());
            locObject.put("speed", location.getSpeed());
            locObject.put("altitude", location.getAltitude());
            locObject.put("trip_num", 2);

            if(MainActivity.description != null)
            {
                locObject.put("description", MainActivity.description);
                MainActivity.description = null;
            }
            else
            {
                locObject.put("description", "");
            }

            if(MainActivity.photoData != null)
            {
                ParseFile img = new ParseFile(System.currentTimeMillis() + ".png", MainActivity.photoData);
                img.saveInBackground();
                Log.d("scdtrack", "Put new image data: " + img);
                locObject.put("image", img);
                MainActivity.photoData = null;
            }

            locObject.saveInBackground();
            locationManager.removeUpdates(this);
            stopSelf();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

}
