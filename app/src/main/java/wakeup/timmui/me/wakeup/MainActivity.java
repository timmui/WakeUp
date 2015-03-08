package wakeup.timmui.me.wakeup;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.scanner.ScanActivity;
import java.io.IOException;



public class MainActivity extends ActionBarActivity {

    private Context appContext;
    private MediaPlayer mPlayer;
    private Vibrator vibrator;
    private TextView tv1;
    private boolean run = false;

    private DeviceListener mListener = new AbstractDeviceListener() {
        private long startTimestamp = 0; // Timestamp since last movement
        private double[] prevOrient = {0, 0, 0, 0};
        private int state = 1;

        @Override
        public void onConnect(Myo myo, long timestamp) {
            startTimestamp = timestamp;
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            double orientChange = Math.abs(prevOrient[3] - rotation.w());

            Log.d("Accel", String.format("%.3f Change: %.3f", rotation.w(), orientChange));
            Log.d("Time", String.format("%d State: %d", (timestamp - startTimestamp), state));

            tv1.setText(String.format("Current State: %d \nTime: %d", state, timestamp - startTimestamp));

            if (!run){
                startTimestamp = timestamp;
                state = 1;
            }

            if (orientChange <= 0.01 && run ) {
                if (state == 1 && Math.abs(timestamp - startTimestamp) >= 10000 && Math.abs(timestamp - startTimestamp) < 20000) {
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    vibrator.vibrate(500);

                    Toast.makeText(getApplicationContext(), "Wake UP! 1st Warning", Toast.LENGTH_LONG).show();
                    state++;
                } else if (state == 2 && (Math.abs(timestamp - startTimestamp) >= 20000 && Math.abs(timestamp - startTimestamp) < 30000)) {
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    vibrator.vibrate(2000);

                    Toast.makeText(getApplicationContext(), "Wake UP! 2nd Warning", Toast.LENGTH_LONG).show();
                    state++;
                } else if (state == 3 && (Math.abs(timestamp - startTimestamp) >= 30000 && Math.abs(timestamp - startTimestamp) < 40000)) {
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    vibrator.vibrate(new long[]{0, 1000, 500, 1000, 500, 1000, 500}, -1);
                    state++;

                    Toast.makeText(getApplicationContext(), "Wake UP! LAST Warning", Toast.LENGTH_LONG).show();

                } else if (state == 4 && (Math.abs(timestamp - startTimestamp) >= 40000 && Math.abs(timestamp - startTimestamp) < 50000)) {
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    vibrator.vibrate(new long[]{0, 1000, 500, 1000, 500, 1000, 500}, -1);

                    Toast.makeText(getApplicationContext(), "Wake UP!!!! ", Toast.LENGTH_LONG).show();

                    // Max Volume
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

                    // Media Player
                    mPlayer = MediaPlayer.create(
                            appContext,
                           R.raw.alert);
                    mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                        }
                    });
                    try {
                        mPlayer.prepareAsync();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }

                    // Text
                    SmsManager smsManager = SmsManager.getDefault();
                    //smsManager.sendTextMessage("5197295683",null,"Wake me up please!",null,null);
                    smsManager.sendTextMessage("6478028459",null,"Wake me up please!",null,null);

                    state=1;
                    //startTimestamp = timestamp;
                }
                } else if (orientChange > 0.01  && Math.abs(timestamp - startTimestamp) >= 800 && run) {
                startTimestamp = timestamp;
                state = 1;

                try{
                    mPlayer.stop();
                }
                catch (NullPointerException e){
                    e.printStackTrace();
                }


            }

            // Setting Current data to previous data
            prevOrient[0] = rotation.x();
            prevOrient[1] = rotation.y();
            prevOrient[2] = rotation.z();
            prevOrient[3] = rotation.w();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        appContext = getApplicationContext();

        vibrator = (Vibrator) getApplicationContext().getSystemService(VIBRATOR_SERVICE);

        tv1 = (TextView) findViewById(R.id.tv1); // stuff
        tv1.setText("No Myo Connected");

        // Create Start button
        final ImageButton startButton = (ImageButton) (findViewById(R.id.bStart));
        final ImageButton stopButton = (ImageButton) (findViewById(R.id.bStop));

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                run = true;
                startButton.setVisibility(View.INVISIBLE);
                startButton.setEnabled(false);
                stopButton.setVisibility(View.VISIBLE);
                stopButton.setEnabled(true);
            }
        });

        // Create Start button
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                run = false;
                stopButton.setVisibility(View.INVISIBLE);
                stopButton.setEnabled(false);
                startButton.setVisibility(View.VISIBLE);
                startButton.setEnabled(true);
            }
        });



        // Myo Hub
        Hub hub = Hub.getInstance();

        if (!hub.init(this)) {
            //Log.e(TAG, "Could not initialize the Hub.");
            finish();
            return;
        }
        hub.addListener(mListener);

    }

    @Override
    public void onDestroy (){
        mPlayer.release();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.action_scan == id) {
            onScanActionSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }
}
