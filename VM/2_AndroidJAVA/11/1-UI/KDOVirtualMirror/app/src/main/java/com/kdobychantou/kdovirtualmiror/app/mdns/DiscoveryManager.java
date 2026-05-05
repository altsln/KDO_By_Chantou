/**********************************************************************
 * Filename    : DiscoveryManager.java
 * Description : Discover the service annd the target for an automatic
 * connection to the Server. Hardcoded IP address is no longer needed.
 * Author      : Alternatives Solutions
 * Modification: 2026/05/04
 **********************************************************************/
package com.kdobychantou.kdovirtualmiror.app.mdns;


import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class DiscoveryManager {
    private static final String TAG = "DiscoveryManager";
    private static final String SERVICE_TYPE = "_arduino._tcp."; // Matches ESP32 MDNS.addService
    private static final String SERVICE_NAME_TARGET = "esp32-KDO";

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private OnDeviceFoundListener listener;

    public interface OnDeviceFoundListener {
        void onDeviceFound(String ip, int port);
        void onError(String message);
    }

    public DiscoveryManager(Context context, OnDeviceFoundListener listener) {
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.listener = listener;
    }

    public void startDiscovery(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wifi.createMulticastLock("esp32_mdns_lock");
        multicastLock.setReferenceCounted(true);
        multicastLock.acquire();

        // ... then call discoverServices ...
        startDiscovery();
    }

    private void startDiscovery() {
        stopDiscovery(); // Clean up any existing listeners first
        initializeDiscoveryListener();
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }


    private void initializeDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                if (listener != null) listener.onError("Discovery Start Failed");
                //stopDiscovery();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Stop discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onDiscoveryStopped(String s) {
                Log.d(TAG, "Service discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service found: " + serviceInfo.getServiceName());

                // Only resolve if it matches our ESP32's name
                if (serviceInfo.getServiceName().contains(SERVICE_NAME_TARGET)) {
                    nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed: " + errorCode);
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                            Log.d(TAG, "Resolve Succeeded. IP: " +
                                    resolvedServiceInfo.getHost().getHostAddress());

                            if (listener != null) {
                                listener.onDeviceFound(
                                        resolvedServiceInfo.getHost().getHostAddress(),
                                        resolvedServiceInfo.getPort()
                                );
                            }
                        }
                    });
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Service lost: " + serviceInfo);
            }
        };
    }

    public void stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping discovery", e);
            }
            discoveryListener = null;
        }
    }

}
