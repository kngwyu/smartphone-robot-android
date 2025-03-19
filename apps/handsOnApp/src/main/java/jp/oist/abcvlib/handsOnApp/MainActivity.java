package jp.oist.abcvlib.handsOnApp;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.vision.detector.Detection;

import java.util.List;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryData;
import jp.oist.abcvlib.core.inputs.microcontroller.BatteryDataSubscriber;
import jp.oist.abcvlib.core.inputs.PublisherManager;
import jp.oist.abcvlib.core.inputs.phone.ObjectDetectorData;
import jp.oist.abcvlib.core.inputs.phone.ObjectDetectorDataSubscriber;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelData;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataSubscriber;
import jp.oist.abcvlib.util.SerialCommManager;
import jp.oist.abcvlib.util.SerialReadyListener;
import jp.oist.abcvlib.util.UsbSerial;

/**
 * Demo app for Handson
 *
 * @author Yuji Kanagawa https://github.com/kngwyu
 */
public class MainActivity extends AbcvlibActivity implements BatteryDataSubscriber, SerialReadyListener, WheelDataSubscriber, ObjectDetectorDataSubscriber {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DebugInfoViewer debugInfo;
    private PublisherManager publisherManager;
    private int countL = 0;
    private int countR = 0;
    private int nLoopCalled = 0;
    private int a = 0;
    private String imageLabel = "Nothing";


    @Override
    protected void abcvlibMainLoop() {
        // int a = 0;
        nLoopCalled += 1;
        // Example code for controlling robots
        // Set wheel output
        // Stop when detected something
        if (imageLabel.equals("keyboard")){
            outputs.setWheelOutput(0.0f, 0.0f, false, false);
        } else if ((!imageLabel.equals("Nothing")) && (!imageLabel.equals("keyboard")))  {
            if (countL== a) {
                outputs.setWheelOutput(-1.0f, -1.0f, false, false);
            } else if ((a < countL) && (countL< (a+10))) {
                outputs.setWheelOutput(-1.0f, -1.0f, false, false);

            } else{
            outputs.setWheelOutput(0.0f, 1.0f, false, false);
            }
        }  else {
                outputs.setWheelOutput(1.0f, 1.0f, false, false);
  //      if (0 < nLoopCalled && nLoopCalled < 20) {
//            outputs.setWheelOutput(0.0f, 0.0f, false, false);
//        } else if (20 < nLoopCalled && nLoopCalled < 120) {
//            outputs.setWheelOutput(1.0f, 1.0f, false, false);
//        } else if (120 < nLoopCalled && nLoopCalled < 160) {
//            outputs.setWheelOutput(0.0f, 1.0f, false, false);
//        } else if (160 < nLoopCalled && nLoopCalled < 200) {
//            outputs.setWheelOutput(-1.0f, -1.0f, false, false);
//        } else {
//            outputs.setWheelOutput(0.0f, 0.0f, false, false);
        }
        a = countL;
        debugInfo.text1 = String.format("MainLoopCount: %d", nLoopCalled);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup Android GUI. Point this method to your main activity xml file or corresponding int
        // ID within the R class
        setContentView(R.layout.activity_main);

        final TextView textView1 = (TextView) findViewById(R.id.description);
        final TextView textView2 = (TextView) findViewById(R.id.description2);
        final TextView textView3 = (TextView) findViewById(R.id.description3);
        final ImageView imageView = (ImageView) findViewById(R.id.demoImage);
        // Set up debugInfo
        debugInfo = new DebugInfoViewer(this, textView1, textView2, textView3, imageView);
        Runnable updateTask = new Runnable() {
            @Override
            public void run() {
                handler.post(debugInfo);
                handler.postDelayed(this, 100); // Schedule the next update
            }
        };
        handler.post(updateTask);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSerialReady(UsbSerial usbSerial) {
        publisherManager = new PublisherManager();

        WheelData wheelData = new WheelData.Builder(this, publisherManager)
                .setBufferLength(50)
                .setExpWeight(0.01).build();
        wheelData.addSubscriber(this);
        BatteryData batteryData = new BatteryData.Builder(this, publisherManager).build();
        batteryData.addSubscriber(this);
        ObjectDetectorData detectorData = new ObjectDetectorData.Builder(this, publisherManager, this)
                .setModel("efficientdet-lite1.tflite")
                .build();
        detectorData.addSubscriber(this);
        setSerialCommManager(new SerialCommManager(usbSerial, batteryData, wheelData));
        super.onSerialReady(usbSerial);
    }

    @Override
    public void onOutputsReady() {
        publisherManager.initializePublishers();
        publisherManager.startPublishers();
    }

    @Override
    public void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR, double wheelDistanceL, double wheelDistanceR, double wheelSpeedInstantL, double wheelSpeedInstantR, double wheelSpeedBufferedL, double wheelSpeedBufferedR, double wheelSpeedExpAvgL, double wheelSpeedExpAvgR) {
        countL = wheelCountL;
        // TODO(kngwyu) why negated?
        countR = -wheelCountR;
        debugInfo.text2 = String.format("WheelCount: %d %d", countL, countR);
    }

    @Override
    public void onBatteryVoltageUpdate(long timestamp, double voltage) {
    }

    @Override
    public void onChargerVoltageUpdate(long timestamp, double chargerVoltage, double coilVoltage) {
    }

    @Override
    public void onObjectsDetected(Bitmap bitmap, TensorImage tensorImage, List<Detection> results, long inferenceTime, int height, int width) {
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            debugInfo.image = Bitmap.createScaledBitmap(rotatedBitmap, rotatedBitmap.getWidth() * 4, rotatedBitmap.getHeight() * 4, true);
            Category category = results.get(0).getCategories().get(0);
            imageLabel = category.getLabel();
            debugInfo.text3 = String.format("Label: %s (score: %.2f)", imageLabel, category.getScore());
        } catch (IndexOutOfBoundsException e) {
            imageLabel = "Nothing";
            debugInfo.text3 = "No object detected";
        }
    }
}