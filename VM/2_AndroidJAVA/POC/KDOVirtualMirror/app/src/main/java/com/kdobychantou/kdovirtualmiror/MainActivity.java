/**********************************************************************
 * Filename    : MainActivity.java
 * Description : Setup TCP socket and display received data on the screen
 * The connection process is done now via mDNS. Connection is now
 * triggered after user clicks on the connect button. Update UI,
 * and all buttons work to connect and disconnect. Now added sharedPrefs
 * to cache the ip address and the port number. scaleGestureDetector
 * added
 * Author      : Alternatives Solutions
 * Modification: 2026/05/07
 **********************************************************************/

package com.kdobychantou.kdovirtualmiror;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.kdobychantou.kdovirtualmiror.app.VideoReceiver;
import com.kdobychantou.kdovirtualmiror.app.mdns.DiscoveryManager;
import com.kdobychantou.kdovirtualmiror.app.viewmodel.StreamViewModel;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private static final String PREFS_NAME = "ESP32_CACHE";
    private ImageView cameraFrameView;
    private TextView sizeText;
    private Button btnConnect;
    private Button btnDisconnect;
//    private VideoReceiver receiver;
    private StreamViewModel viewModel;
    private DiscoveryManager discoveryManager;

    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            //v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // TURN OFF SLEEP MODE (Keep screen on)  WORKAROUND NEEDED IF SCREEN ROTATE
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        sizeText = findViewById(R.id.tv_img_size);
        cameraFrameView = findViewById(R.id.camera_frame_view);

        // Initialize the ViewModel
        viewModel = new ViewModelProvider(this).get(StreamViewModel.class);

        // 1. Observe the Video Frame
        viewModel.getFrame().observe(this, bitmap -> {
            cameraFrameView.setImageBitmap(bitmap);
        });

        // 2. Observe the FPS
        viewModel.getFps().observe(this, fps -> {
            sizeText.setText(fps);
        });

        // 3. Your Discovery Logic
        discoveryManager = new DiscoveryManager(this, new DiscoveryManager.OnDeviceFoundListener() {
            @Override
            public void onDeviceFound(String ip, int port) {

                // Save the discovery result for next time
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putString("last_ip", ip)
                        .putInt("last_port", port)
                        .apply();

                // Now you have the dynamic IP! Start the video
                Log.d(TAG, "ipAdr= " + ip);
                /*runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Found ESP32 at " + ip, Toast.LENGTH_SHORT).show();
                    receiver.startVideoListening(ip, port, videoListener);
                });*/
                viewModel.startStreaming(ip, port);
            }

            @Override
            public void onError(String message) {
                Log.e("Discovery", message);
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Your callback logic here
                Toast.makeText(MainActivity.this, "Discovery Started...", Toast.LENGTH_SHORT).show();
                // TURN OFF SLEEP MODE (Keep screen on)
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                // Call this when the user clicks a "Scan" or "Connect" button
                Log.d(TAG, "attempt to connect");
                attemptConnection();
            }
        });


        btnDisconnect.setOnClickListener(v -> {
            viewModel.stopStreaming();
            //TURN ON SLEEP MODE (Allow screen to dim)
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Toast.makeText(this, "Stream Stopped", Toast.LENGTH_SHORT).show();
        });


        //gesture management
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

        // Attach the detector to the ImageView's touch listener
        cameraFrameView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        Log.d(TAG, "onCreate Done!");
    }

    private void attemptConnection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cachedIp = prefs.getString("last_ip", null);
        int cachedPort = prefs.getInt("last_port", -1);

        if (cachedIp != null && cachedPort != -1) {
            Log.d(TAG, "Using cached IP: " + cachedIp);
            viewModel.startStreaming(cachedIp, cachedPort);
        } else {
            Log.d(TAG, "No cache, starting discovery.");
            discoveryManager.startDiscovery(getApplicationContext());
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        receiver.stop();
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Limit the zoom: 1x to 5x
            mScaleFactor = Math.max(1.0f, Math.min(mScaleFactor, 5.0f));

            cameraFrameView.setScaleX(mScaleFactor);
            cameraFrameView.setScaleY(mScaleFactor);

            Log.d(TAG, "Current Zoom: " + mScaleFactor);
            return true;
        }
    }
}
