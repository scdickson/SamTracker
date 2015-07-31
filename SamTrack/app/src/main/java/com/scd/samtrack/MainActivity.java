package com.scd.samtrack;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.Parse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;


public class MainActivity extends ActionBarActivity implements View.OnClickListener
{
    private static final int CAMERA_REQUEST = 1888;
    public static String description = null;
    public static byte[] photoData = null;
    public static boolean USE_GPS = true;

    static HashMap<String, Integer> cityLocations;
    boolean isTracking = false;
    Button stop, start;
    ImageView img, text;
    static EditText console;
    static TextView numRefreshes;
    public static Handler handler = new Handler(){
        public void handleMessage(Message msg)
        {
            Bundle bundle = msg.getData();
            String data = bundle.getString("message");
            int num_views = bundle.getInt("num_views");
            cityLocations = (HashMap<String, Integer>) bundle.getSerializable("city_locations");
            console.append(getDateTime() + "-> " + data);
            numRefreshes.setText(String.valueOf(num_views));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Parse.enableLocalDatastore(this);
        Parse.initialize(this, "yM55WWyesqIhfQ5ZkgXnPXwged6JLjob0QcQz58p", "nngduxwGT96iParUfClNjDNrc2N3ogE7JLyoHLil");

        start = (Button) findViewById(R.id.start);
        start.setOnClickListener(this);
        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(this);

        img = (ImageView) findViewById(R.id.add_img);
        img.setOnClickListener(this);
        text = (ImageView) findViewById(R.id.add_text);
        text.setOnClickListener(this);

        numRefreshes = (TextView) findViewById(R.id.num_refreshes);
        numRefreshes.setOnClickListener(this);

        console = (EditText) findViewById(R.id.console);
    }

    public static String getDateTime()
    {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getDefault());
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    public void stopTrack()
    {
        if(isTracking) {
            isTracking = false;
            console.append(getDateTime() + "-> " + "Stopped Tracking.\n");
            Intent intent = new Intent(getApplicationContext(), TrackReceiver.class);
            final PendingIntent pIntent = PendingIntent.getBroadcast(this, TrackReceiver.REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(pIntent);
        }
    }

    public void startTrack()
    {
        if(!isTracking) {
            isTracking = true;
            console.append(getDateTime() + "-> " + "Started Tracking...\n");
            Intent intent = new Intent(getApplicationContext(), TrackReceiver.class);
            final PendingIntent pIntent = PendingIntent.getBroadcast(this, TrackReceiver.REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            long firstMillis = System.currentTimeMillis(); // first run of alarm is immediate
            int intervalMillis = 1000 * 60 * 10; // 10 mins
            AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, intervalMillis, pIntent);
        }
    }

    public void onClick(View view)
    {
        if(view.equals(stop))
        {
            stop.setVisibility(View.GONE);
            start.setVisibility(View.VISIBLE);
            stopTrack();
        }
        else if(view.equals(start))
        {
            stop.setVisibility(View.VISIBLE);
            start.setVisibility(View.GONE);
            startTrack();
        }
        else if(view.equals(img))
        {
            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
        else if(view.equals(text))
        {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Add a description for the next location:");

            final EditText input = new EditText(this);
            input.setLines(10);
            alert.setView(input);

            alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    description = input.getText().toString();
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        }
        else if(view.equals(numRefreshes))
        {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Views by City:");

            ListView modeList = new ListView(this);
            String[] stringArray = new String[cityLocations.size()];
            int i = 0;
            for(String city : cityLocations.keySet())
            {
                stringArray[i++]=(city + ": " + cityLocations.get(city));
            }
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
            modeList.setAdapter(modeAdapter);

            builder.setView(modeList);
            final Dialog dialog = builder.create();

            dialog.show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.PNG, 100, out);
            photoData = out.toByteArray();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(item.isCheckable())
        {
            item.setChecked(true);
        }

        switch(item.getItemId())
        {
            case R.id.use_network:
                USE_GPS = false;
                break;
            case R.id.use_gps:
                USE_GPS = true;
                break;
        }

        return true;
    }
}
