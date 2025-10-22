package jp.oist.abcvlib.pidqrtransmitter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;


import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataSubscriber;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
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
public class MainActivity extends AbcvlibActivity implements SerialReadyListener,
        BatteryDataSubscriber {

    private BalancePIDController balancePIDController;
    private QRCode qrCode;
    // Create your data publisher objects
    PublisherManager publisherManager = new PublisherManager();
    private OrientationData orientationData;
    private WheelData wheelData;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Double> pidData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        pidData.put("setPoint", 5.9);
        pidData.put("p_tilt", -0.205);
        pidData.put("d_tilt", 0.0115);
    }

    protected void onStart() {
        super.onStart();
        handler.post(checkControllerRunnable);
    }

    private final Runnable checkControllerRunnable = new Runnable() {
        @Override
        public void run() {
            if (balancePIDController != null) {
                qrCode = new QRCode(getSupportFragmentManager(), R.id.qrFragmentView);
            } else {
                handler.postDelayed(this, 100); // Check again in 100ms
            }
        }
    };

    public void buttonClick(View view) {
        Button button = (Button) view;
        if (button.getText().equals("Start")) {
            // Sets initial values rather than wait for slider change
            if (qrCode != null) {
                JSONObject jsonObject = new JSONObject(pidData);
                String jsonString = jsonObject.toString();
                qrCode.generate(jsonString);
                try {
                    balancePIDController.setPID(pidData.get("p_tilt"), 0.0, pidData.get("d_tilt"), pidData.get("setPoint"), 0.0, 0.0, 15.0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                button.setText("Stop");
                balancePIDController.startController();
            }
        } else {
            button.setText("Start");
            balancePIDController.stopController();
        }
    }

    @Override
    public void onSerialReady(UsbSerial usbSerial) {
        orientationData = new OrientationData
                .Builder(this, publisherManager).build();
        BatteryData batteryData = new BatteryData.Builder(this, publisherManager).build();
        batteryData.addSubscriber(this);
        wheelData = new WheelData.Builder(this, publisherManager).build();
        SerialCommManager serialCommManager = new SerialCommManager(usbSerial, batteryData, wheelData);
        setSerialCommManager(serialCommManager);
        super.onSerialReady(usbSerial);
    }

    @Override
    public void onOutputsReady() {
        publisherManager.initializePublishers();
        publisherManager.startPublishers();

        // Create your controller/subscriber
        balancePIDController = (BalancePIDController) new BalancePIDController(outputs).setInitDelay(0)
                .setName("BalancePIDController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(5)
                .setTimeUnit(TimeUnit.MILLISECONDS);
        // Attach the controller/subscriber to the publishers
        orientationData.addSubscriber(balancePIDController);
        wheelData.addSubscriber(balancePIDController);
    }

    // Main loop for any application extending AbcvlibActivity. This is where you will put your main code
    @Override
    protected void abcvlibMainLoop() {
        balancePIDController.run();
    }

    @Override
    public void onBatteryVoltageUpdate(long timestamp, double voltage) {

    }

    @Override
    public void onChargerVoltageUpdate(long timestamp, double chargerVoltage, double coilVoltage) {

    }
}
