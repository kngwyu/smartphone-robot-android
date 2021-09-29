package jp.oist.abcvlib.compoundcontroller;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.concurrent.TimeUnit;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.AbcvlibLooper;
import jp.oist.abcvlib.core.IOReadyListener;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.tests.BalancePIDController;
import jp.oist.abcvlib.fragments.PidGuiFragament;

/**
 * Android application showing connection to IOIOBoard, Hubee Wheels, and Android Sensors
 * Initializes socket connection with external python server
 * Runs PID controller locally on Android, but takes PID parameters from python GUI
 * Shows how to setup custom controller in conjunction with the the PID balance controller.
 * @author Christopher Buckley https://github.com/topherbuckley
 */
public class MainActivity extends AbcvlibActivity implements IOReadyListener {

    private BalancePIDController balancePIDController;
    private PidGuiFragament pidGuiFragament;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        // Informs AbcvlibActivity that this is the class it should call when IO is ready.
        setIoReadyListener(this);

        // Passes Android App information up to parent classes for various usages. Do not modify
        super.onCreate(savedInstanceState);
    }

    public void displayPID_GUI(){
        pidGuiFragament = PidGuiFragament.newInstance(balancePIDController);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.main_fragment, pidGuiFragament).commit();
    }

    public void buttonClick(View view) {
        Button button = (Button) view;
        if (button.getText().equals("Start")){
            // Sets initial values rather than wait for slider change
            pidGuiFragament.updatePID();
            button.setText("Stop");
            balancePIDController.startController();

        }else{
            button.setText("Start");
            balancePIDController.stopController();
        }
    }

    @Override
    public void onIOReady(AbcvlibLooper abcvlibLooper) {
        // Create your data publisher objects
        PublisherManager publisherManager = new PublisherManager();
        OrientationData orientationData = new OrientationData
                .Builder(this, publisherManager).build();
        WheelData wheelData = new WheelData
                .Builder(this, publisherManager, abcvlibLooper).build();
        // Initialize all publishers (i.e. start their threads and data streams)
        publisherManager.initializePublishers();

        // Create your controllers/subscribers
        balancePIDController = (BalancePIDController) new BalancePIDController().setInitDelay(0)
                .setName("BalancePIDController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(5)
                .setTimeUnit(TimeUnit.MILLISECONDS);
        CustomController customController = (CustomController) new CustomController().setInitDelay(0)
                .setName("CustomController").setThreadCount(1)
                .setThreadPriority(Thread.NORM_PRIORITY).setTimestep(1000)
                .setTimeUnit(TimeUnit.MILLISECONDS);

        // Attach the controller/subscriber to the publishers
        orientationData.addSubscriber(balancePIDController);
        wheelData.addSubscriber(balancePIDController);
        wheelData.addSubscriber(customController);;

        // Start passing data from publishers to subscribers
        publisherManager.startPublishers();

        // Starting and never stopping the customController to see difference between this and adding the PID controller to it via the GUI button.
        customController.startController();

        // Adds your custom controller to the compounding master controller.
        getOutputs().getMasterController().addController(balancePIDController);
        getOutputs().getMasterController().addController(customController);
        // Start the master controller after adding and starting any customer controllers.
        getOutputs().startMasterController();

        runOnUiThread(this::displayPID_GUI);
    }
}
