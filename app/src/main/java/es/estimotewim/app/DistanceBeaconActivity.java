package es.estimotewim.app;

import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.util.Date;
import java.util.List;


public class DistanceBeaconActivity extends ActionBarActivity {

    private static final String TAG = DistanceBeaconActivity.class.getSimpleName();
    private static final String MAC_ADDRESS = "FF:D0:D7:45:0C:AB";
    private BeaconManager beaconManager;
    private Beacon beacon;
    private Region region;

    // Y positions are relative to height of bg_distance image.
    private static final double RELATIVE_START_POS = 320.0 / 1110.0;
    private static final double RELATIVE_STOP_POS = 885.0 / 1110.0;

    private View dotView;

    private int startY = 1;
    private int segmentLength = 35;
    private long time;
    private TextView distanceTextView;

    private static final double MAX_WIDE_RANGE_METER = 30.0;
    private static final long REFRESH_DATA_TIME_MILlI_SECONDS = 1000;
    private static final int MAX_METER_ERROR_DISCARD = 6;
    private double lastDistance = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.distance_view);

        ImageView imagePerson = (ImageView) findViewById(R.id.imagePerson);
        dotView = findViewById(R.id.dot);
        distanceTextView = (TextView)findViewById(R.id.distance);

        startY = imagePerson.getLayoutParams().height + 10;

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        segmentLength = displaymetrics.heightPixels;

//        beacon = getIntent().getParcelableExtra(MainActivity.EXTRAS_BEACON);
//        region = new Region("regionid", beacon.getProximityUUID(), beacon.getMajor(), beacon.getMinor());
//        if (beacon == null) {
//            Toast.makeText(this, "Beacon not found in intent extras", Toast.LENGTH_LONG).show();
//            finish();
//        }

        beaconManager = new BeaconManager(this);
        time = new Date().getTime();

        time += REFRESH_DATA_TIME_MILlI_SECONDS;

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> rangedBeacons) {
                dotView.setVisibility(View.VISIBLE);
                // Note that results are not delivered on UI thread.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Just in case if there are multiple beacons with the same uuid, major, minor.
                        Beacon foundBeacon = null;
                        for (Beacon rangedBeacon : rangedBeacons) {
//                            final String macAddress = beacon.getMacAddress();
                            final String macAddress = MAC_ADDRESS;
                            if (rangedBeacon.getMacAddress().equals(macAddress)) {
                                foundBeacon = rangedBeacon;
                            }
                        }
                        long now = new Date().getTime();
                        if (foundBeacon != null && now > time) {
                            time = now + REFRESH_DATA_TIME_MILlI_SECONDS;
                            updateDistanceView(foundBeacon);
                        }
                    }
                });
            }
        });

//        final View view = findViewById(R.id.root);
//        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//
//                startY = (int) (RELATIVE_START_POS * view.getMeasuredHeight());
//                int stopY = (int) (RELATIVE_STOP_POS * view.getMeasuredHeight());
//                segmentLength = stopY - startY;
//
//                dotView.setVisibility(View.VISIBLE);
//                dotView.setTranslationY(computeDotPosY(beacon));
//            }
//        });
    }

    private void updateDistanceView(Beacon foundBeacon) {
        if (segmentLength == -1) {
            return;
        }

        distanceTextView.setText(Double.toString(Utils.computeAccuracy(foundBeacon)));
        dotView.animate().translationY(computeDotPosY(foundBeacon)).start();
    }

    private int computeDotPosY(Beacon beacon) {
        // Let's put dot at the end of the scale when it's further than 6m.

        double distance = Utils.computeAccuracy(beacon);

        if (lastDistance > 0 && Math.abs(lastDistance - distance) > MAX_METER_ERROR_DISCARD) {
            distance = lastDistance;
        } else {
            lastDistance = distance;
        }

        Log.w(TAG, "    Distance: " + distance);
        final int newPosition = (int)Math.ceil(startY + (segmentLength * (distance / MAX_WIDE_RANGE_METER)));
        Log.w(TAG, "Last Position: " + lastDistance);
        Log.w(TAG, "New Position: " + newPosition);






        return newPosition;
    }

    @Override
    protected void onStart() {
        super.onStart();

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(MainActivity.ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    Toast.makeText(DistanceBeaconActivity.this, "Cannot start ranging, something terrible happened",
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Cannot start ranging", e);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        beaconManager.disconnect();

        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_distance_beacon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
