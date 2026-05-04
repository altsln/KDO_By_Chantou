/**********************************************************************
 * Filename    : CounterReceiver.java
 * Description : Reading String from ESP32 and send it back to the
 * main Thread
 * Author      : Alternatives Solutions
 * Modification: 2026/05/01
 **********************************************************************/
package com.kdobychantou.kdovirtualmiror.app;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MessageReceiver {
    private static String TAG = MessageReceiver.class.toString();
    private boolean isRunning = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    // Interface to send data back to your Activity
    public interface OnMessageReceivedListener {
        void onMsgReceived(String count);
        void onError(String message);
    }

    public void startListening(String ip, int port, OnMessageReceivedListener listener) {
        isRunning = true;
        // Start a background thread (Replaces CoroutineScope)
        new Thread(() -> {
            try (Socket socket = new Socket(ip, port);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String line;
                while (isRunning) {
                    //Next portion of the code goes here
                    line = reader.readLine();
                    if (null != line) {
                        final String message = line;
                        //log data
                        Log.d(TAG, "MSG = " + message);     //log data
                        // Switch to Main Thread to update UI
                        mHandler.post(() -> {
                            if (listener != null) listener.onMsgReceived(message);
                        });
                    }
                }
            } catch (Exception e) {
                mHandler.post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
    }
}
