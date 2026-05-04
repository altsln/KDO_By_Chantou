/**********************************************************************
 * Filename    : MainActivity.java
 * Description : Setup TCP socket and display received data on the screen
 * Author      : Alternatives Solutions
 * Modification: 2026/05/02
 **********************************************************************/

package com.kdobychantou.kdovirtualmiror;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kdobychantou.kdovirtualmiror.app.ImageReceiver;
import com.kdobychantou.kdovirtualmiror.app.MessageReceiver;
import com.kdobychantou.kdovirtualmiror.app.NetworkSettings;
import com.kdobychantou.kdovirtualmiror.app.VideoReceiver;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private ImageView cameraFrameView;
    private TextView sizeText;
    private VideoReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sizeText = findViewById(R.id.tv_img_size);
        cameraFrameView = findViewById(R.id.camera_frame_view);

        receiver = new VideoReceiver();
        receiver.startVideoListening(NetworkSettings.IP_ADDR,
                NetworkSettings.PORT_NUMBER,
                new VideoReceiver.OnVideoReceivedListener() {
                    @Override
                    public void onImgReceived(int ingSize, Bitmap bitmap) {
                        //sizeText.setText("img Size: " + ingSize);
                        // This must happen on the Main Thread
                        cameraFrameView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onStatusUpdate(String fps) {
                        //Log.d(TAG, fps);
                        sizeText.setText(fps);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "NETWORK - Error: " + message);
                    }
                });
        Log.d(TAG, "onCreate Done!");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiver.stop();
    }
}
