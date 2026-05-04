/**********************************************************************
 * Filename    : ImageReceiver.java
 * Description : Reading image size and image data from ESP32 and
 * send it back to the main Thread
 * Author      : Alternatives Solutions
 * Modification: 2026/05/03
 **********************************************************************/
package com.kdobychantou.kdovirtualmiror.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.DataInputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageReceiver {
    private static String TAG = ImageReceiver.class.toString();
    private boolean isRunning = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    // Interface to send data back to your Activity
    public interface OnImageReceivedListener {
        void onImgReceived(int ingSize, Bitmap bitmap);
        void onError(String message);
    }

    public void startListening(String ip, int port, OnImageReceivedListener  listener) {
        isRunning = true;
        // Start a background thread (Replaces CoroutineScope)
        new Thread(() -> {
            try (Socket socket = new Socket(ip, port);
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                Log.d(TAG, "Connected to ESP32 Image Server");

                while (isRunning) {
                    // 1. Read Image Size first (4 bytes, Little Endian)
                    byte[] sizeBuffer = new byte[4];
                    dis.readFully(sizeBuffer);
                    int imgSize = ByteBuffer.wrap(sizeBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    // Sanity check: JPEG frames for ESP32 are usually < 100KB.
                    // This prevents crashes if we get a garbage number.
                    if (imgSize > 0 && imgSize < 1000000) {
                        // 2. Read the actual Image Bytes
                        byte[] imgBytes = new byte[imgSize];
                        dis.readFully(imgBytes);
                        Log.d(TAG, "image size: " + imgSize);

                        // 3. Convert to Bitmap and send to UI
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                        mHandler.post(() -> {
                            if (bitmap != null) listener.onImgReceived(imgSize, bitmap);
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
