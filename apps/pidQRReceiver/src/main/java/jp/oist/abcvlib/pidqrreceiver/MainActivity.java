package jp.oist.abcvlib.pidqrreceiver;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataSubscriber;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.QRCodeData;
import jp.oist.abcvlib.core.inputs.phone.QRCodeDataSubscriber;
import jp.oist.abcvlib.tests.BalancePIDController;
import jp.oist.abcvlib.util.QRCode;
import jp.oist.abcvlib.util.SerialCommManager;
import jp.oist.abcvlib.util.SerialReadyListener;
import jp.oist.abcvlib.util.UsbSerial;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 *
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements SerialReadyListener, QRCodeDataSubscriber, BatteryDataSubscriber {
    private final static String TAG = "MainActivity";
    private BalancePIDController balancePIDController;
    // Create your data publisher objects
    PublisherManager publisherManager = new PublisherManager();
    private OrientationData orientationData;
    private WheelData wheelData;
    private final Map<String, Double> pidData = new HashMap<>();
    private boolean pidvalueChanged = true;

    private final int PID_BEFORE = 0;
    private final int BOUNCE1 = 1;
    private final int BOUNCE2 = 2;
    private final int ROTATING = 3;
    private final int PID_AFTER = 4;
    private final int NOT_STARTED = 5;

    private int state = NOT_STARTED;
    private int stateCount = 0;

    private TextView statusTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);
        statusTextView = findViewById(R.id.statusText);
        if (statusTextView != null) {
            statusTextView.setText("Looking for QR Code...");
        }
        pidData.put("setPoint", 3.6);
        pidData.put("p_tilt", -0.2);
        pidData.put("d_tilt", 0.01);
    }

    public void buttonClick(View view) {
        Button button = (Button) view;
        if (button.getText().equals("Start")) {
            // Sets initial values rather than wait for slider change
            if (balancePIDController != null) {
                button.setText("Stop");
                balancePIDController.startController();
                state = PID_BEFORE;
            }
        } else {
            button.setText("Start");
            balancePIDController.stopController();
        }
    }

    @Override
    public void onSerialReady(UsbSerial usbSerial) {
        orientationData = new OrientationData.Builder(this, publisherManager).build();
        BatteryData batteryData = new BatteryData.Builder(this, publisherManager).build();
        batteryData.addSubscriber(this);
        wheelData = new WheelData.Builder(this, publisherManager).build();
        QRCodeData qrCodeData = new QRCodeData.Builder(this, publisherManager, this).build();
        qrCodeData.addSubscriber(this);
        SerialCommManager serialCommManager = new SerialCommManager(usbSerial, batteryData, wheelData);
        setSerialCommManager(serialCommManager);
        super.onSerialReady(usbSerial);
    }

    @Override
    public void onOutputsReady() {
        publisherManager.initializePublishers();
        publisherManager.startPublishers();

        // Create your controller/subscriber
        balancePIDController = (BalancePIDController) new BalancePIDController(outputs).setInitDelay(0).setName("BalancePIDController").setThreadCount(1).setThreadPriority(Thread.NORM_PRIORITY).setTimestep(5).setTimeUnit(TimeUnit.MILLISECONDS);
        // Attach the controller/subscriber to the publishers
        orientationData.addSubscriber(balancePIDController);
        wheelData.addSubscriber(balancePIDController);
    }

    // Main loop for any application extending AbcvlibActivity. This is where you will put your main code
    @Override
    protected void abcvlibMainLoop() {
        if (state == NOT_STARTED) {
            return;
        }
        if (pidvalueChanged) {
            try {
                balancePIDController.setPID(pidData.get("p_tilt"), 0.0, pidData.get("d_tilt"), pidData.get("setPoint"), 0.0, 0.0, 15.0);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            pidvalueChanged = false;
        }
        if (state == ROTATING) {
            outputs.setWheelOutput(0.6f, -0.6f, false, false);
        } else if(state == BOUNCE1) {
            outputs.setWheelOutput(0.6f, 0.6f, false, false);

        }else if(state == BOUNCE2) {
            outputs.setWheelOutput(-0.6f, -0.6f, false, false);

        }else {
            balancePIDController.run();
        }
        stateCount += 1;
        if (state == PID_BEFORE && stateCount > 50) {
            state = ROTATING;
            stateCount = 0;
        }else if (state == BOUNCE1 && stateCount > 20) {
            state = BOUNCE2;
            stateCount = 0;
        } else if (state == BOUNCE2 && stateCount > 10) {
            state = ROTATING;
            stateCount = 0;
        } else if (state == ROTATING && stateCount > 50) {
            state = PID_BEFORE;
            stateCount = 0;
        }
        Log.v(TAG, "State: " + state + "count: " + stateCount);
    }

    @Override
    public void onBatteryVoltageUpdate(long timestamp, double voltage) {

    }

    @Override
    public void onChargerVoltageUpdate(long timestamp, double chargerVoltage, double coilVoltage) {

    }

    public void onQRCodeDetected(String qrDataDecoded) {
        try {
            JSONObject jsonObject = new JSONObject(qrDataDecoded);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                // Check if the value is a number to avoid class cast exceptions
                if (jsonObject.get(key) instanceof Number) {
                    double value = jsonObject.getDouble(key);
                    pidData.put(key, value);
                    Log.d(TAG, "QR Decoded: Updated " + key + " to " + value);
                }
            }
            pidvalueChanged = true; // Signal that PID values have changed and need to be applied
            state = PID_AFTER;
            runOnUiThread(() -> {
                if (statusTextView != null) {
                    statusTextView.setText("QR code found!");
                }
            });
        } catch (JSONException e) {
            // If the qrDataDecoded is not a valid JSON string, a JSONException will be thrown.
            // In that case, we do nothing and ignore the QR code.
            Log.d(TAG, "Invalid QR code value");
        }
    }
}
