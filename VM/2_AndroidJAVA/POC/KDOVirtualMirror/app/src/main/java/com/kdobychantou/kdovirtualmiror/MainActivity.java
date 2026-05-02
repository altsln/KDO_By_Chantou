/**********************************************************************
 * Filename    : MainActivity.java
 * Description : Setup TCP socket and display received data on the screen
 * Author      : Alternatives Solutions
 * Modification: 2026/05/02
 **********************************************************************/

package com.kdobychantou.kdovirtualmiror;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kdobychantou.kdovirtualmiror.app.MessageReceiver;
import com.kdobychantou.kdovirtualmiror.app.NetworkSettings;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private MessageReceiver receiver;
    private TextView msgText;

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

        msgText = findViewById(R.id.tv_counter);
        receiver = new MessageReceiver();
        receiver.startListening(NetworkSettings.IP_ADDR,
                NetworkSettings.PORT_NUMBER,
                new MessageReceiver.OnMessageReceivedListener() {
                    @Override
                    public void onMsgReceived(String message) {
                        if (message.contains("STATUS_OK")) {
                            msgText.setTextColor(Color.GREEN);
                        } else if (message.contains("DATA_READY")) {
                            msgText.setTextColor(Color.BLUE);
                        } else {
                            msgText.setTextColor(Color.BLACK);
                        }
                        msgText.setText(message);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("NETWORK", "Error: " + message);
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
