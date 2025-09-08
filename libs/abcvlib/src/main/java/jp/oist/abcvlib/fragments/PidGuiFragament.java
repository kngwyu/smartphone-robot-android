package jp.oist.abcvlib.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jp.oist.abcvlib.core.R;
import jp.oist.abcvlib.tests.BalancePIDController;
import jp.oist.abcvlib.util.ErrorHandler;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PidGuiFragament#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PidGuiFragament extends Fragment{

    Slider setPoint_;
    Slider p_tilt_;
    Slider d_tilt_;
    Slider p_wheel_;
    Slider maxTiltFwd_;
    Slider maxTiltBwd_;

    private final String TAG = this.getClass().toString();
    private BalancePIDController balancePIDController;
    private final Slider.OnChangeListener sliderChangeListener = (slider, value, fromUser) -> updatePID();

    Map<String, Slider> controls = new HashMap<String, Slider>();

    public PidGuiFragament() {
        // Required empty public constructor
    }

    public PidGuiFragament(BalancePIDController balancePIDController) {
        this.balancePIDController = balancePIDController;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void updatePID() {
        try {
            balancePIDController.setPID(p_tilt_.getValue(),
                    0,
                    d_tilt_.getValue(),
                    setPoint_.getValue(),
                    p_wheel_.getValue(),
                    maxTiltFwd_.getValue(),
                    maxTiltBwd_.getValue());
        } catch (InterruptedException e) {
            ErrorHandler.eLog(TAG, "Error when getting slider gui values", e, true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.pid_gui, container, false);
        controls.put("sp", setPoint_ = rootView.findViewById(R.id.seekBarSetPoint));
        controls.put("pt", p_tilt_ = rootView.findViewById(R.id.seekBarTiltP));
        controls.put("dt", d_tilt_ = rootView.findViewById(R.id.seekBarTiltD));
        controls.put("pw", p_wheel_ = rootView.findViewById(R.id.seekBarWheelSpeedP));
        controls.put("mtf", maxTiltFwd_ = rootView.findViewById(R.id.seekBarMaxTiltFwd));
        controls.put("mtb", maxTiltBwd_ = rootView.findViewById(R.id.seekBarMaxTiltBwd));

        // Format the label for some small-valued sliders
        p_wheel_.setLabelFormatter(value -> String.format(Locale.US, "%.5f", value));
        p_tilt_.setLabelFormatter(value -> String.format(Locale.US, "%.3f", value));
        d_tilt_.setLabelFormatter(value -> String.format(Locale.US, "%.4f", value));

        for (Map.Entry<String, Slider> entry : controls.entrySet()) {
            entry.getValue().addOnChangeListener(sliderChangeListener);
        }
        return rootView;
    }

    public String getControls() {

        JSONObject controlValues = new JSONObject();
        Map<String, Double> controls = new HashMap<String, Double>();

        controls.put("sp", 2.8);
        controls.put("pt", -24.0);
        controls.put("dt", 1.0);
        controls.put("pw", 0.4);
        controls.put("ew", 0.25);
        controls.put("mt", 6.5);

        // Take the value from each slider and store it in a new HashMap
        for (Map.Entry<String, Double> entry : controls.entrySet()) {
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            String value = df.format(entry.getValue());
            try {
                controlValues.put(entry.getKey(), value);
            } catch (JSONException e) {
                ErrorHandler.eLog(TAG, "Error when processing hashmap for controls", e, true);
            }
        }
        return controlValues.toString();
    };
}