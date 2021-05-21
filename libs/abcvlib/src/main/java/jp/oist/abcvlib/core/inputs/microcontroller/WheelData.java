package jp.oist.abcvlib.core.inputs.microcontroller;

import android.util.Log;

import jp.oist.abcvlib.core.AbcvlibActivity;
import jp.oist.abcvlib.core.learning.gatherers.TimeStepDataBuffer;

public class WheelData {

    private final TimeStepDataBuffer timeStepDataBuffer;
    private boolean isRecording = false;

    private final int windowLength = 5;
    private int quadCount = windowLength;
    private double dt_sample = 0;
    private double expWeight = 0.1;

    //----------------------------------- Wheel speed metrics --------------------------------------
    /**
     * Compounding negative wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private double encoderCountRightWheel = 0;
    /**
     * Compounding positive wheel count set in AbcvlibLooper. Right is negative while left is
     * positive since wheels are mirrored on physical body and thus one runs cw and the other ccw.
     */
    private double encoderCountLeftWheel = 0;
    /**
     * Right Wheel Speed in quadrature encoder counts per second.
     */
    private double speedRightWheel = 0;
    private double speedRightWheelLP = 0;

    /**
     * Left Wheel Speed in quadrature encoder counts per second.
     */
    private double speedLeftWheel = 0;
    private double speedLeftWheelLP = 0;

    /**
     * distance in mm that the right wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceR = 0;
    /**
     * distance in mm that the left wheel has traveled from start point
     * This assumes no slippage/lifting/etc. Use with a grain of salt.
     */
    private double distanceL = 0;

    private double distanceLPrevious = 0;
    private double distanceRPrevious = 0;
    private double distanceLLP = 0;
    private double distanceRLP = 0;

    private final long[] timeStamps = new long[windowLength];

    public WheelData(AbcvlibActivity abcvlibActivity){
        this.timeStepDataBuffer = abcvlibActivity.getTimeStepDataAssembler().getTimeStepDataBuffer();
    }

    /**
     * Sets the encoder count for the right wheel. This shouldn't normally be used by the user. This
     * method exists for the AbcvlibLooper class to get/set encoder counts as the AbcvlibLooper
     * class is responsible for constantly reading the encoder values from the IOIOBoard.
     */
    public void onWheelDataUpdate(long timestamp, double countLeft, double countRight) {
        if (isRecording){
            timeStepDataBuffer.getWriteData().getWheelCounts().put(timestamp, countLeft, countRight);
        }
        int indexCurrent = (quadCount) % windowLength;
        int indexPrevious = (quadCount - 1) % windowLength;

        encoderCountLeftWheel = countLeft;
        encoderCountRightWheel = countRight;
        timeStamps[indexCurrent] = timestamp;
        dt_sample = (timeStamps[indexCurrent] - timeStamps[indexPrevious]) / 1000000000f;
        setDistanceL();
        setDistanceR();
        setWheelSpeedL();
        setWheelSpeedR();

        quadCount++;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    // Todo change all R/L methods to a single method returning a two field object instead
    /**
     * @return Current encoder count for the right wheel
     */
    public double getWheelCountR(){ return encoderCountRightWheel; }

    /**
     * @return Current encoder count for the left wheel
     */
    public double getWheelCountL(){ return encoderCountLeftWheel; }

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceL in mm
     */
    public double getDistanceL(){return distanceL;}

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    public double getDistanceR(){return distanceR;}

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedL() {return speedLeftWheel;}

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    public double getWheelSpeedR() {return speedRightWheel;}

    /**
     * @return Current speed of left wheel in encoder counts per second with a Low Pass filter.
     * May want to convert to rotations per second if the encoder resolution (counts per revolution)
     * is known.
     */
    public double getWheelSpeedL_LP() {return speedLeftWheelLP;}

    /**
     * @return Current speed of left wheel in encoder counts per second with a Low Pass filter.
     * May want to convert to rotations per second if the encoder resolution (counts per revolution)
     * is known.
     */
    public double getWheelSpeedR_LP() { return speedRightWheelLP;}

    /**
     * Get distances traveled by left wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceL in mm
     */
    private void setDistanceL(){
        double mmPerCount = (2 * Math.PI * 30) / 128;
        distanceLPrevious = distanceL;
        distanceL = encoderCountLeftWheel * mmPerCount;
        distanceLLP = exponentialAvg(distanceL, distanceLLP, 1);

    }

    /**
     * Get distances traveled by right wheel from start point.
     * This does not account for slippage/lifting/etc. so
     * use with a grain of salt
     * @return distanceR in mm
     */
    private void setDistanceR(){
        double mmPerCount = (2 * Math.PI * 30) / 128;
        distanceRPrevious = distanceR;
        distanceR = encoderCountRightWheel * mmPerCount;
        distanceRLP = exponentialAvg(distanceR, distanceRLP, 1);
    }

    /**
     * @return Current speed of left wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedL() {
        if (dt_sample != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedLeftWheel = (distanceL - distanceLPrevious) / dt_sample;
            speedLeftWheelLP = exponentialAvg(speedLeftWheel, speedLeftWheelLP, expWeight);
        }
        else{
            Log.i("sensorDebugging", "dt_sample == 0");
        }
    }

    /**
     * @return Current speed of right wheel in encoder counts per second. May want to convert to
     * rotations per second if the encoder resolution (counts per revolution) is known.
     */
    private void setWheelSpeedR() {
        if (dt_sample != 0) {
            // Calculate the speed of each wheel in mm/s.
            speedRightWheel = (distanceR - distanceRPrevious) / dt_sample;
            speedRightWheelLP = exponentialAvg(speedRightWheel, speedRightWheelLP, expWeight);
        }
        else{
            Log.i("sensorDebugging", "dt_sample == 0");
        }
    }

    private double calcDistance(int count){
        double distance;
        double mmPerCount = (2 * Math.PI * 30) / 128;
        distance = count * mmPerCount;
        return distance;
    }

    public double getExpWeight(){
        return expWeight;
    }

    public void setExpWeight(double weight){
        expWeight = weight;
    }

    private double exponentialAvg(double sample, double expAvg, double weighting){
        expAvg = (1.0 - weighting) * expAvg + (weighting * sample);
        return expAvg;
    }
}
