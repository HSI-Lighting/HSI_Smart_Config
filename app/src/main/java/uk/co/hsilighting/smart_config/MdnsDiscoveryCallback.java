package uk.co.hsilighting.smart_config;

import android.net.nsd.NsdServiceInfo;

public interface MdnsDiscoveryCallback {
    public void ServiceDiscovered(NsdServiceInfo serviceInfo);
}
