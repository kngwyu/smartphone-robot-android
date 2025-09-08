package jp.oist.abcvlib.pidbalancer;

import android.app.Activity;
import android.widget.TextView;

import java.util.Locale;

import jp.oist.abcvlib.tests.BalancePIDController;

public class SpeedDegViewer implements Runnable {
    private final Activity activity;
    private final TextView speedView;
    private final TextView degView;
    private final BalancePIDController controler;


    public SpeedDegViewer(
            Activity activityInit,
            TextView speedViewInit,
            TextView degViewInit,
            BalancePIDController balancePIDController
    ) {
        activity = activityInit;
        speedView = speedViewInit;
        degView = degViewInit;
        controler = balancePIDController;
    }

    @Override
    public void run() {
        activity.runOnUiThread(() -> {
            this.speedView.setText(String.format(Locale.US, "Speed: %.2f", controler.getSpeedL()));
            this.degView.setText(String.format(Locale.US, "Deg: %.2f", controler.getThetaDeg()));
        });
    }
}

