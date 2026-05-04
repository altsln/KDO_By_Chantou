/**********************************************************************
 * Filename    : MainActivity.java
 * Description : Setup TCP socket and display received data on the screen
 * The connection process is done now via mDNS
 * Author      : Alternatives Solutions
 * Modification: 2026/05/04
 **********************************************************************/

package com.kdobychantou.kdovirtualmiror;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kdobychantou.kdovirtualmiror.app.ImageReceiver;
import com.kdobychantou.kdovirtualmiror.app.MessageReceiver;
import com.kdobychantou.kdovirtualmiror.app.NetworkSettings;
import com.kdobychantou.kdovirtualmiror.app.VideoReceiver;
import com.kdobychantou.kdovirtualmiror.app.mdns.DiscoveryManager;

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
        // Set the listener
        VideoReceiver.OnVideoReceivedListener videoListener = new VideoReceiver.OnVideoReceivedListener() {
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
        };


        DiscoveryManager discoveryManager =
                new DiscoveryManager(this, new DiscoveryManager.OnDeviceFoundListener() {
                    @Override
                    public void onDeviceFound(String ip, int port) {
                        // Now you have the dynamic IP! Start the video
                        Log.d(TAG, "ipAdr= " + ip);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Found ESP32 at " + ip, Toast.LENGTH_SHORT).show();
                            receiver.startVideoListening(ip, port, videoListener);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("Discovery", message);
                    }
                });

        Log.d(TAG, "Start discovery");
        discoveryManager.startDiscovery(getApplicationContext());

        Log.d(TAG, "onCreate Done!");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        receiver.stop();
    }
}
