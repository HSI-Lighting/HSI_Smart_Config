package uk.co.hsilighting.smart_config;

import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import uk.co.hsilighting.smart_config.databinding.ActivityProvisionBinding;
import com.espressif.iot.esptouch2.provision.EspProvisioner;
import com.espressif.iot.esptouch2.provision.EspProvisioningListener;
import com.espressif.iot.esptouch2.provision.EspProvisioningRequest;
import com.espressif.iot.esptouch2.provision.EspProvisioningResult;
import com.espressif.iot.esptouch2.provision.TouchNetUtil;

import java.util.ArrayList;
import java.util.List;

public class ProvisioningActivity extends AppCompatActivity {
    private static final String TAG = ProvisioningActivity.class.getSimpleName();

    public static final String KEY_PROVISION = "provision";
    public static final String KEY_PROVISION_REQUEST = "provision_request";
    public static final String KEY_DEVICE_COUNT = "device_count";

    private List<EspProvisioningResult> mStations;
    private StationAdapter mStationAdapter;

    private EspProvisioner mProvisioner;

    private WifiManager mWifiManager;

    private Observer<String> mBroadcastObserver;

    private ActivityProvisionBinding mBinding;

    private boolean mWifiFailed = false;

    private long mTime;

    private int mWillProvisioningCount = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityProvisionBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        EspProvisioningRequest request = getIntent().getParcelableExtra(KEY_PROVISION_REQUEST);
        mWillProvisioningCount = getIntent().getIntExtra(KEY_DEVICE_COUNT, -1);
        assert request != null;
        mProvisioner = new EspProvisioner(getApplicationContext());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, OrientationHelper.VERTICAL));
        mStations = new ArrayList<>();
        mStationAdapter = new StationAdapter();
        recyclerView.setAdapter(mStationAdapter);

        mBinding.stopBtn.setOnClickListener(v -> {
            v.setEnabled(false);
            mProvisioner.stopProvisioning();
        });

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mBroadcastObserver = action -> {
            boolean connected = TouchNetUtil.isWifiConnected(mWifiManager);
            if (!connected && mProvisioner.isProvisioning()) {
                mWifiFailed = true;
                mBinding.messageView.setText(getString(R.string.esptouch2_provisioning_wifi_disconnect));
                mProvisioner.stopProvisioning();
            }
        };
        HSI_Smart_Config_App.getInstance().observeBroadcastForever(mBroadcastObserver);

        mTime = System.currentTimeMillis();
        mProvisioner.startProvisioning(request, new ProvisionListener());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        HSI_Smart_Config_App.getInstance().removeBroadcastObserver(mBroadcastObserver);
        mProvisioner.stopProvisioning();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        mProvisioner.stopProvisioning();
        mProvisioner.close();
    }

    private class StationHolder extends RecyclerView.ViewHolder {
        TextView bssid_text;
        TextView ip_text;
        TextView device_type_text;

        String device_IP;

        StationHolder(@NonNull View itemView) {
            super(itemView);

            bssid_text = itemView.findViewById(R.id.bssid_text);
            bssid_text.setTextColor(Color.BLACK);
            ip_text = itemView.findViewById(R.id.ip_text);
            ip_text.setTextColor(Color.BLACK);
            device_type_text = itemView.findViewById(R.id.device_type_text);
            device_type_text.setTextColor(Color.BLACK);

            itemView.findViewById(R.id.config_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Create an Intent to start the WebViewActivity
                    Intent intent = new Intent(ProvisioningActivity.this, ConfigActivity.class);

                    // Put the URL as an extra
                    intent.putExtra("IP", device_IP);

                    // Start the activity
                    ProvisioningActivity.this.startActivity(intent);
                }
            });
        }
    }

    private class StationAdapter extends RecyclerView.Adapter<StationHolder> {

        @NonNull
        @Override
        public StationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.provision_result_item,
                    parent, false);
            return new StationHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull StationHolder holder, int position) {
            EspProvisioningResult station = mStations.get(position);

            holder.bssid_text.setText(getString(R.string.esptouch2_provisioning_result_bssid, station.bssid));
            holder.ip_text.setText(getString(R.string.esptouch2_provisioning_result_address,
                    station.address.getHostAddress()));
            holder.device_type_text.setText(getString(R.string.esptouch2_provisioning_result_device_type,
                    "checking..."));
            holder.device_IP = station.address.getHostAddress();
        }

        @Override
        public int getItemCount() {
            return mStations.size();
        }
    }


    private class ProvisionListener implements EspProvisioningListener {
        @Override
        public void onStart() {
            Log.d(TAG, "ProvisionListener onStart: ");
        }

        @Override
        public void onResponse(EspProvisioningResult result) {
            String mac = result.bssid;
            String host = result.address.getHostAddress();
            Log.d(TAG, "ProvisionListener onResponse: " + mac + " " + host);
            runOnUiThread(() -> {
                mStations.add(result);
                mStationAdapter.notifyItemInserted(mStations.size() - 1);

                if (mWillProvisioningCount > 0 && mStations.size() >= mWillProvisioningCount) {
                    mProvisioner.stopProvisioning();
                }
            });
        }

        @Override
        public void onStop() {
            Log.d(TAG, "ProvisionListener onStop: ");
            runOnUiThread(() -> {
                if (!mWifiFailed && mStations.isEmpty()) {
                    mBinding.messageView.setText(R.string.esptouch2_provisioning_result_none);
                }
                mBinding.stopBtn.setEnabled(false);
                mBinding.progressView.setVisibility(View.GONE);
            });
            mTime = System.currentTimeMillis() - mTime;
            Log.e(TAG, "Provisioning task cost " + mTime);
        }

        @Override
        public void onError(Exception e) {
            Log.w(TAG, "ProvisionListener onError: ", e);
            mProvisioner.stopProvisioning();
            runOnUiThread(() -> {
                String message = getString(R.string.esptouch2_provisioning_result_exception,
                        e.getLocalizedMessage());
                mBinding.messageView.setText(message);
            });
        }
    }
}
