package wakeup.timmui.me.wakeup;

import android.content.Intent;
import android.graphics.Color;
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

import java.security.Timestamp;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity {

    private final int DELAY = 5000;
    private DeviceListener mListener = new AbstractDeviceListener() {
        private long startTimestamp = 0; // Timestamp since last movement
        private int alertDelay = 10/60; // Time to alert (in minutes)
        private double [] prevOrient = {0,0,0,0};

        @Override
        public void onAccelerometerData (Myo myo, long timestamp, Vector3 accel) {
            float [] values = {0,0,0};
            values [0] = (float) accel.x();
            values [1] = (float) accel.y();
            values [2] = (float) accel.z();

            double magnitude = Math.sqrt(Math.pow(values[0],2) + Math.pow(values[1],2) + Math.pow(values[2],2));

            Log.d("Accel",String.format("X: %.3f Y: %.3f Z: %.3f \n %.3f", values[0],values[1],values[2],magnitude));

            if ( magnitude <= 0.1){
                if ((timestamp-startTimestamp) >= alertDelay*60000){
                    //myo.vibrate(Myo.VibrationType.MEDIUM);
                }
            }
            else {
                startTimestamp = timestamp;
            }

        }
        @Override
        public void onConnect (Myo myo, long timestamp){
            startTimestamp = timestamp;
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation){
            float values[] = {0,0,0,0};
            Log.d("Accel",String.format("%.3f", rotation.w()));

            double orientChange = Math.abs(prevOrient [3] - rotation.w());

            if ( orientChange <= 0.1){
                if ((timestamp-startTimestamp) >= alertDelay*60000){
                    myo.vibrate(Myo.VibrationType.MEDIUM);
                    Toast.makeText(getApplicationContext(), "Wake UP!", Toast.LENGTH_LONG);
                }
            }
            else {
                startTimestamp = timestamp;
            }

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

        // Myo Hub
        Hub hub = Hub.getInstance();

        if (!hub.init(this)) {
            //Log.e(TAG, "Could not initialize the Hub.");
            finish();
            return;
        }
        hub.addListener(mListener);

        //Instantiates a new timer object
        Timer timer = new Timer();
        final Runnable runnable = new Runnable(){
            public void run(){

            }
        };
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(runnable);
            }
        },DELAY);

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
