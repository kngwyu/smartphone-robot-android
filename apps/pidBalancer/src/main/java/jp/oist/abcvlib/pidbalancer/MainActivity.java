package jp.oist.abcvlib.pidbalancer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataSubscriber;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.fragments.PidGuiFragament;
import jp.oist.abcvlib.tests.BalancePIDController;
import jp.oist.abcvlib.util.SerialCommManager;
import jp.oist.abcvlib.util.SerialReadyListener;
import jp.oist.abcvlib.util.UsbSerial;

/**
 * Android application to demonstrate PID control of the robot
 * @author Christopher Buckley <a href="https://github.com/topherbuckley">...</a>
 * @author Yuji Kanagawa <a href="https://github.com/kngwyu">...</a>
 */
public class MainActivity extends AbcvlibActivity implements BatteryDataSubscriber, SerialReadyListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BalancePIDController balancePIDController;
    private PublisherManager publisherManager;
    private PidGuiFragament pidGuiFragment;
    private SpeedDegViewer sdViewer;
    private final String TAG = this.getClass().toString();
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        // Set up PidInfoViewer
        balancePIDController = (BalancePIDController) new BalancePIDController().setInitDelay(0)
                .setName("BalancePIDController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(5)
                .setTimeUnit(TimeUnit.MILLISECONDS);
        this.runOnUiThread(this::createPIDFragment);

        // UI update
        final TextView speedView = (TextView) findViewById(R.id.speed_box);
        final TextView degView = (TextView) findViewById(R.id.deg_box);
        sdViewer = new SpeedDegViewer(this, speedView, degView, balancePIDController);
        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                handler.post(sdViewer);
                handler.postDelayed(this, 100); // Schedule the next update
            }
        };
        handler.post(updateTask);
    }


    /**
     * Handles the logic for the Start/Stop button click.
     * This method should be called from the Activity's onClick handler.
     * @param view The button view that was clicked.
     */
    public void buttonClick(View view) {
        Button button = (Button) view;
        if (button.getText().equals("Start")){
            Log.d(TAG, "Start button clicked.");
            if (pidGuiFragment != null) {
                pidGuiFragment.updatePID();
                button.setText("Stop");
                balancePIDController.startController();
            }
        } else {
            Log.d(TAG, "Stop button clicked.");
            button.setText("Start");
            balancePIDController.stopController();
        }
    }

    @Override
    public void onSerialReady(UsbSerial usbSerial) {
        publisherManager = new PublisherManager();
        OrientationData orientationData = new OrientationData
                .Builder(this, publisherManager).build();
        orientationData.addSubscriber(this.balancePIDController);
        WheelData wheelData = new WheelData.Builder(this, publisherManager).build();
        wheelData.addSubscriber(this.balancePIDController);
        BatteryData batteryData = new BatteryData.Builder(this, publisherManager).build();
        batteryData.addSubscriber(this);
        setSerialCommManager(new SerialCommManager(usbSerial, batteryData, wheelData));
        super.onSerialReady(usbSerial);
    }

    private void createPIDFragment() {
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        pidGuiFragment = new PidGuiFragament(this.balancePIDController);
        // Begin a transaction to place the fragment in the UI
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment, pidGuiFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onOutputsReady() {
        publisherManager.initializePublishers();
        publisherManager.startPublishers();
        getOutputs().getMasterController().addController(balancePIDController);
        // Start the master controller after adding and starting any customer controllers.
        getOutputs().startMasterController();
    }
    // Main loop is empty but left here for easy-extension (kngwyu)
    @Override
    protected void abcvlibMainLoop() {}

    @Override
    public void onBatteryVoltageUpdate(long timestamp, double voltage) {}

    @Override
    public void onChargerVoltageUpdate(long timestamp, double chargerVoltage, double coilVoltage) {}
}
