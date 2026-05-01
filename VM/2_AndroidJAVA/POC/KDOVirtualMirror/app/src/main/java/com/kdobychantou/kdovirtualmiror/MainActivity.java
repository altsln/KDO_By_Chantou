/**********************************************************************
 * Filename    : MainActivity.java
 * Description : Setup TCP socket
 * Auther      : Alternatives Solutions
 * Modification: 2026/04/29
 **********************************************************************/

package com.kdobychantou.kdovirtualmiror;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kdobychantou.kdovirtualmiror.app.CounterReceiver;
import com.kdobychantou.kdovirtualmiror.app.NetworkSettings;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private CounterReceiver receiver;

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


        receiver = new CounterReceiver();
        receiver.startListening(NetworkSettings.IP_ADDR, NetworkSettings.PORT_NUMBER);
        Log.d(TAG, "onCreate Done!");
    }
}