package jp.oist.abcvlib.tests;

import android.util.Log;

import jp.oist.abcvlib.core.inputs.Publisher;
import jp.oist.abcvlib.core.inputs.microcontroller.WheelDataSubscriber;
import jp.oist.abcvlib.core.inputs.phone.OrientationData;
import jp.oist.abcvlib.core.inputs.phone.OrientationDataSubscriber;
import jp.oist.abcvlib.core.outputs.AbcvlibController;

public class BalancePIDController extends AbcvlibController implements WheelDataSubscriber, OrientationDataSubscriber {

    private final String TAG = this.getClass().getName();

    // Initialize all sensor reading variables
    private double p_tilt = -10;
    private double i_tilt = 0.0;
    private double d_tilt = 1.0;
    private double setPoint = 1.4;
    private double p_wheel = 0.0;
    private double e_t = 0; // e(t) of wikipedia
    private double int_e_t; // integral of e(t) from wikipedia. Discrete, so just a sum here.

    private double maxTiltFwd = 20.0; // in Degrees
    private double maxTiltBwd = 10.0; // in Degrees

    private double speedL;
    private double thetaDeg;
    private double angularVelocityDeg;

    private int bounceLoopCount = 0;

    public BalancePIDController(){
    }

    public void run() {
        int decision = 0;
        // If current tilt angle is over maxTiltFwd or under -maxTiltFwd --> Bounce U
        if (setPoint < thetaDeg - maxTiltFwd) {
            // Example 0 < 20 - 15
            // The robot is learning backward too much. Bounce forward first.
            bounce(false);
        } else if(setPoint > thetaDeg + maxTiltBwd) {
            // Example 0 > -20 + 15
            // The robot is learning backward too much. Bounce forward first.
            bounce(true);
            decision = 1;
        }else{
            bounceLoopCount = 0;
            linearController();
            decision = 2;
        }

        Log.v(TAG, "setPoint: " + setPoint + ", thetaDeg: " + thetaDeg + ", decision: " + decision);
    }

    public double getSpeedL() {
        return speedL;
    }

    public double getThetaDeg() {
        return thetaDeg;
    }

    /**
     * A means of changing the PID values while this controller is running
     * @param p_tilt_ proportional controller relative to the tilt angle of the phone
     * @param i_tilt_ integral controller relative to the tilt angle of the phone
     * @param d_tilt_ derivative controller relative to the tilt angle of the phone
     * @param setPoint_ the assumed angle where the robot would be balanced (ideally zero but realistically nearer to 3 or 4 deg)
     * @param p_wheel_ proportional controller relative to the wheel distance
     * @param maxTiltFwd_ max forward tilt angle (deg) at which the controller will switch between a linear and non-linear bounce controller.
     * @param maxTiltBwd_ max backward tilt angle (deg) at which the controller will switch between a linear and non-linear bounce controller.
     * @throws InterruptedException thrown if shutdown while trying to read/write to the IOIO board.
     */
    synchronized public void setPID(double p_tilt_, double i_tilt_, double d_tilt_, double setPoint_,
                                    double p_wheel_, double maxTiltFwd_, double maxTiltBwd_)
            throws InterruptedException {

        try {
                setPoint = setPoint_;
                p_tilt = p_tilt_;
                i_tilt = i_tilt_;
                d_tilt = d_tilt_;
                p_wheel = p_wheel_;
                maxTiltFwd = maxTiltFwd_;
                maxTiltBwd = maxTiltBwd_;
        } catch (NullPointerException e){
            Log.e(TAG,"Error", e);
            Thread.sleep(1000);
        }
    }

    // -------------- Actual Controllers ----------------------------

    private void bounce(boolean forward) {
        // loop steps between turning on and off wheels.
        final int bouncePulseWidth = 100;
        final float bounceSpeed = 0.6f;
        if (bounceLoopCount < bouncePulseWidth * 0.1){
            setOutput(0,0);
        }else if (bounceLoopCount < bouncePulseWidth * 1.1){
            if (forward){
                setOutput(bounceSpeed, bounceSpeed);
            }else{
                setOutput(-bounceSpeed,-bounceSpeed);
            }
        }else if (bounceLoopCount < bouncePulseWidth * 1.2){
            setOutput(0,0);
        }else if (bounceLoopCount < bouncePulseWidth * 2.2) {
            if (forward){
                setOutput(-bounceSpeed,-bounceSpeed);
            }else{
                setOutput(bounceSpeed, bounceSpeed);
            }
        }else {
            bounceLoopCount = 0;
        }
        bounceLoopCount++;
    }

    private void linearController() {
        // TODO this needs to account for length of time on each interval, or overall time length. Here this just assumes a width of 1 for all intervals.
        int_e_t = int_e_t + e_t;
        e_t = setPoint - thetaDeg;
        // error between actual and desired wheel speed (default 0)
        double e_w = 0.0 - speedL;
        Log.v(TAG, "speedL: " + speedL + ", deg: " + thetaDeg);

        double p_out = (p_tilt * e_t) + (p_wheel * e_w);
        double i_out = i_tilt * int_e_t;
        double d_out = d_tilt * angularVelocityDeg;

        setOutput((float)(p_out + i_out + d_out), (float)(p_out + i_out + d_out));
        Log.v(TAG, "P_tilt * e_t" + (p_tilt * e_t) + "p_wheel * e_w" +(p_wheel * e_w));
        Log.v(TAG, "PID: " + p_out + "," + i_out + "," + d_out);
    }

    // -------------- Input Data Listeners ----------------------------

    @Override
    public void onWheelDataUpdate(long timestamp, int wheelCountL, int wheelCountR,
                                  double wheelDistanceL, double wheelDistanceR,
                                  double wheelSpeedInstantL, double wheelSpeedInstantR,
                                  double wheelSpeedBufferedL, double wheelSpeedBufferedR,
                                  double wheelSpeedExpAvgL, double wheelSpeedExpAvgR) {
        speedL = wheelSpeedExpAvgL;
    }

    @Override
    public void onOrientationUpdate(long timestamp, double thetaRad, double angularVelocityRad) {
        thetaDeg = OrientationData.getThetaDeg(thetaRad);
        angularVelocityDeg = OrientationData.getAngularVelocityDeg(angularVelocityRad);
    }
}
