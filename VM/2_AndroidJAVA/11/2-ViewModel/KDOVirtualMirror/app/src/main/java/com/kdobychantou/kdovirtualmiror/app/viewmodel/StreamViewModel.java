/**********************************************************************
 * Filename    : StreamViewModel.java
 * Description : Setup VideoReceiver to live configuration changes
 * Author      : Alternatives Solutions
 * Modification: 2026/05/05
 **********************************************************************/
package com.kdobychantou.kdovirtualmiror.app.viewmodel;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kdobychantou.kdovirtualmiror.app.VideoReceiver;


public class StreamViewModel extends ViewModel {
    private static String TAG = StreamViewModel.class.toString();
    // LiveData allows the UI to "watch" for new frames
    private final MutableLiveData<Bitmap> currentFrame = new MutableLiveData<>();
    private final MutableLiveData<String> fpsText = new MutableLiveData<>();

    private VideoReceiver videoReceiver;

    public LiveData<Bitmap> getFrame() { return currentFrame; }
    public LiveData<String> getFps() { return fpsText; }

    public void startStreaming(String ip, int port) {
        // Only start if we aren't already streaming
        Log.d(TAG, "Attempting to start stream at " + ip);
        if (null == videoReceiver) {
            videoReceiver = new VideoReceiver();
            videoReceiver.startVideoListening(ip, port, new VideoReceiver.OnVideoReceivedListener() {
                @Override
                public void onImgReceived(int size, Bitmap bitmap) {
                    // postValue sends data from the background thread to the UI thread
                    currentFrame.postValue(bitmap);
                }

                @Override
                public void onStatusUpdate(String fps) {
                    fpsText.postValue(fps);
                }

                @Override
                public void onError(String message) {

                }
            });
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // This is called when the app is actually closed (not just rotated)
        if (videoReceiver != null) {
            videoReceiver.stop();
        }
    }
}