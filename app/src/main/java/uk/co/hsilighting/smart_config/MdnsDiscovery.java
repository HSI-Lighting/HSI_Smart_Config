package uk.co.hsilighting.smart_config;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;

public class MdnsDiscovery {
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;

    public void startDiscovery(Context context, MdnsDiscoveryCallback callback) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("mDNS", "Discovery failed: " + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("mDNS", "Stop discovery failed: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.i("mDNS", "Discovery started");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i("mDNS", "Discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.i("mDNS", "Service found: " + serviceInfo.getServiceName());

                // Resolve the service to get connection details
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e("mDNS", "Resolve failed: " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        Log.i("mDNS", "Resolved service: " + serviceInfo);
                        Log.i("mDNS", "Host: " + serviceInfo.getHost());
                        Log.i("mDNS", "Port: " + serviceInfo.getPort());

                        callback.ServiceDiscovered(serviceInfo);
                        // Now you can connect to the device
                        // connectToDevice(serviceInfo.getHost(), serviceInfo.getPort());
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.i("mDNS", "Service lost: " + serviceInfo);
            }
        };

        // Start discovery for ESP32 services
        nsdManager.discoverServices("_hsi._udp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        if (nsdManager != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}