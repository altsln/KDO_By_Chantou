/**********************************************************************
 * Filename    : MainActivity.java
 * Description : Setup TCP socket and display received data on the screen
 * The connection process is done now via mDNS. Connection is now
 * triggered after user clicks on the connect button. Update UI
 * Author      : Alternatives Solutions
 * Modification: 2026/05/05
 **********************************************************************/

package com.kdobychantou.kdovirtualmiror;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private ImageView cameraFrameView;
    private TextView sizeText;
    private Button btnConnect;
//    private VideoReceiver receiver;
    private StreamViewModel viewModel;
    private DiscoveryManager discoveryManager;

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

        btnConnect = findViewById(R.id.btn_connect);
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

                // Call this when the user clicks a "Scan" or "Connect" button
                Log.d(TAG, "Start discovery");
                discoveryManager.startDiscovery(getApplicationContext());
            }
        });

        Log.d(TAG, "onCreate Done!");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        receiver.stop();
    }
}
