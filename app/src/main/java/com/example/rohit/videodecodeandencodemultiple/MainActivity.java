package com.example.rohit.videodecodeandencodemultiple;

import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final VideoDecodeAndEncoder videoDecodeAndEncoder = new VideoDecodeAndEncoder(Environment.getExternalStorageDirectory().toString()+ File.separator+"inputvideo.mp4",Environment.getExternalStorageDirectory().toString()+ File.separator+"outputvideo.mp4",2);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    videoDecodeAndEncoder.extractDecodeEditEncodeMux();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}



/*





 */