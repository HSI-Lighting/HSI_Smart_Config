package uk.co.hsilighting.smart_config;

import android.net.nsd.NsdServiceInfo;

import com.espressif.iot.esptouch2.provision.EspProvisioningResult;

public class HSI_Provision_Result {
    public EspProvisioningResult espProvisioningResult;
    public NsdServiceInfo mdnsDiscoveryResult;

    public HSI_Provision_Result(EspProvisioningResult provRes){
        espProvisioningResult = provRes;
    }

    public HSI_Provision_Result(NsdServiceInfo discoveryResult){
        mdnsDiscoveryResult = discoveryResult;
    }
}
