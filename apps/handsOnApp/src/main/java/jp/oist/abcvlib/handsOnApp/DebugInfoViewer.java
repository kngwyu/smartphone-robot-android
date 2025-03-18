package jp.oist.abcvlib.handsOnApp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

public class DebugInfoViewer implements Runnable {
    private final Activity activity;
    private final TextView textView1;
    private final TextView textView2;
    private final TextView textView3;
    private final ImageView imageView;
    volatile String text1;
    volatile String text2;
    volatile String text3;
    volatile Bitmap image;


    public DebugInfoViewer(
            Activity activityInit,
            TextView textViewInit1,
            TextView textViewInit2,
            TextView textViewInit3,
            ImageView imageViewInit
    ){
        activity = activityInit;
        textView1 = textViewInit1;
        textView2 = textViewInit2;
        textView3 = textViewInit3;
        imageView = imageViewInit;
        image = null;
    }

    @Override
    public void run() {
        activity.runOnUiThread(() -> {
            this.textView1.setText(text1);
            this.textView2.setText(text2);
            this.textView3.setText(text3);
            if (image != null) {
                imageView.setImageBitmap(image);
            }
        });
    }
}
