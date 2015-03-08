package wakeup.timmui.me.wakeup;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.scanner.ScanActivity;

import java.io.IOException;
import java.security.Timestamp;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    private final int DELAY = 5000;
    private Context appContext;
    private MediaPlayer mPlayer;
    private Vibrator vibrator;

    private DeviceListener mListener = new AbstractDeviceListener() {
        private long startTimestamp = 0; // Timestamp since last movement
        private double alertDelay = 1; // Time to alert (in minutes)
        private double [] prevOrient = {0,0,0,0};
        private int state = 1;
        private int active = 1;

        @Override
        public void onConnect (Myo myo, long timestamp){
            startTimestamp = timestamp;
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation){
            float values[] = {0,0,0,0};

            double orientChange = Math.abs(prevOrient [3] - rotation.w());

            Log.d("Accel",String.format("%.3f Change: %.3f", rotation.w(),orientChange));
            Log.d("Time",String.format("%d State: %d",(timestamp-startTimestamp),state));

            if ( orientChange <= 0.002){
                if (state == 1 && Math.abs(timestamp-startTimestamp) >= 10000 && Math.abs(timestamp-startTimestamp) < 20000){
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    vibrator.vibrate(500);

                    Toast.makeText(getApplicationContext(), "Wake UP!", Toast.LENGTH_LONG).show();
                    state ++;
                }
                else if (state == 2 && (Math.abs(timestamp-startTimestamp) >= 20000 && Math.abs(timestamp-startTimestamp) < 30000)){
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    vibrator.vibrate(2000);

                    state ++;
                }
                else if (state == 3 && (Math.abs(timestamp-startTimestamp) >= 30000 && Math.abs(timestamp-startTimestamp) < 40000)){
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    vibrator.vibrate(new long[]{0,1000,500,1000,500,1000,500},-1);
                    state ++;

                    try{
                        mPlayer.prepare();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }

                }
                else if (state == 4 && (Math.abs(timestamp-startTimestamp) >= 40000 && Math.abs(timestamp-startTimestamp) < 50000))
                {
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    myo.vibrate(Myo.VibrationType.LONG);
                    vibrator.vibrate(new long[]{0,1000,500,1000,500,1000,500},-1);

                    //TODO: Audio (Justin Bieber)

                    mPlayer.start();

                    state = 1;
                    startTimestamp = timestamp;
                }
            }
            else {
                startTimestamp = timestamp;
                state = 1;
                mPlayer.stop();

            }

            // Setting Current data to previous data
            prevOrient [0] = rotation.x();
            prevOrient [1] = rotation.y();
            prevOrient [2] = rotation.z();
            prevOrient [3] = rotation.w();
        }
        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose){
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    //mTextView.setText(getString(R.string.hello_world));
                    break;
                case REST:
                case DOUBLE_TAP:
                    int restTextId = R.string.hello_world;
                    switch (myo.getArm()) {
                        case LEFT:
                            //restTextId = R.string.arm_left;
                            break;
                        case RIGHT:
                            //restTextId = R.string.arm_right;
                            break;
                    }
                    //mTextView.setText(getString(restTextId));
                    break;
                case FIST:
                    //mTextView.setText(getString(R.string.pose_fist));
                    break;
                case WAVE_IN:
                    //mTextView.setText(getString(R.string.pose_wavein));
                    break;
                case WAVE_OUT:
                    // mTextView.setText(getString(R.string.pose_waveout));
                    break;
                case FINGERS_SPREAD:
                    //mTextView.setText(getString(R.string.pose_fingersspread));
                    break;
            }

            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);

                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext = getApplicationContext();

        mPlayer = MediaPlayer.create(appContext, R.raw.alert);
        vibrator = (Vibrator) appContext.getSystemService(VIBRATOR_SERVICE);

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
