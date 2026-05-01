/**********************************************************************
 * Filename    : CounterReceiver.java
 * Description : Reading integer from ESP32
 * Auther      : Alternatives Solutions
 * Modification: 2026/05/01
 **********************************************************************/
package com.kdobychantou.kdovirtualmiror.app;

import android.util.Log;

import java.io.DataInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CounterReceiver {
    private static String TAG = CounterReceiver.class.toString();
    private boolean isRunning = false;

    public void startListening(String ip, int port) {
        isRunning = true;
        // Start a background thread (Replaces CoroutineScope)
        new Thread(() -> {
            try (Socket socket = new Socket(ip, port);
                 DataInputStream inputStream = new DataInputStream(socket.getInputStream())) {

                while (isRunning) {
                  //Next portion of the code goes here
                    // ESP32/C++ typically sends Little-Endian
                    byte[] bytes = new byte[4];
                    inputStream.readFully(bytes);
                    int counter = ByteBuffer.wrap(bytes)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .getInt();
                    //log data
                    Log.d(TAG, "data= " + counter);
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
    }
}
