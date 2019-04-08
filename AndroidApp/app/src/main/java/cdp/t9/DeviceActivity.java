package cdp.t9;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class DeviceActivity extends AppCompatActivity {
    BluetoothDevice device;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCallback callback;

    ArrayList<BluetoothGattService> services;

    AlertDialog progress;
    Handler handler;
    Runnable rRefreshServices;

    ListView lvwServices;
    ServiceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        device = getIntent().getParcelableExtra("btdevice");
        if (device == null) {
            Toast.makeText(this, getString(R.string.device_invalid_launch), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(device.getName() == null ? getString(R.string.main_null_device_name) : device.getName());

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCancelable(false);
        b.setView(R.layout.layout_connecting);
        progress = b.create();

        services = new ArrayList<>();
        handler = new Handler();
        rRefreshServices = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetInvalidated();
                handler.postDelayed(rRefreshServices, 2000);
            }
        };

        lvwServices = findViewById(R.id.lvw_device_services);
        adapter = new ServiceAdapter(services);
        lvwServices.setAdapter(adapter);

        callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("BLEApp", "Device Connected");

                        bluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        progress.dismiss();
                        Toast.makeText(DeviceActivity.this, R.string.device_disconnected, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("BLEApp", "Error!");
                    progress.dismiss();
                    finish();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLEApp", "Service Discovery Finished");
                    services.addAll(bluetoothGatt.getServices());
                    adapter.notifyDataSetInvalidated();
                    progress.dismiss();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();

        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        progress.show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = device.connectGatt(this, false, callback, BluetoothDevice.TRANSPORT_LE);
        } else {
            bluetoothGatt = device.connectGatt(this, false, callback);
        }
    }

    private class ServiceAdapter extends BaseAdapter {
        ArrayList<SvcChrPack> scmap;

        public ServiceAdapter(ArrayList<BluetoothGattService> services) {
            scmap = new ArrayList<>();
            for (BluetoothGattService s : services) {
                SvcChrPack scp = new SvcChrPack();
                scp.svc = s;
                scp.chr = new ArrayList<>();
                scp.chr.addAll(s.getCharacteristics());
                scmap.add(scp);
            }
        }

        @Override
        public int getCount() {
            return services.size();
        }

        @Override
        public BluetoothGattService getItem(int position) {
            return services.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListItemViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                convertView = inflater.inflate(R.layout.list_services, parent, false);

                holder = new ListItemViewHolder();
                holder.tvwSvcName = convertView.findViewById(R.id.tvw_service_name);
                holder.tvwDetails = convertView.findViewById(R.id.tvw_service_temp);

                convertView.setTag(holder);
            } else {
                holder = (ListItemViewHolder)convertView.getTag();
            }

            holder.tvwSvcName.setText(scmap.get(position).svc.getUuid().toString());

            StringBuilder sb = new StringBuilder();
            for (BluetoothGattCharacteristic c : scmap.get(position).chr) {
                sb.append("Characteristic ").append(c.getUuid().toString()).append("\n");
                if ((c.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) sb.append("Readable ");
                if ((c.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) sb.append("Writable ");
                if ((c.getProperties() & (BluetoothGattCharacteristic.PROPERTY_BROADCAST)) > 0) sb.append("Broadcastable ");
                sb.append("\n");
                sb.append("Reporting value: ").append(Util.bytesToHex(c.getValue())).append("\n\n");
            }

            holder.tvwDetails.setText(sb.toString());

            return convertView;
        }

        private class SvcChrPack {
            BluetoothGattService svc;
            ArrayList<BluetoothGattCharacteristic> chr;
        }

        private class ListItemViewHolder {
            TextView tvwSvcName;
            TextView tvwDetails;
        }
    }
}
