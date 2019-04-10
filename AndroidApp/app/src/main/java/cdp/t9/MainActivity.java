package cdp.t9;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_PERM = 2;

    private final int ADAPTER_NOTIFY_INTERVAL = 100;

    private Handler handler;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothAdapter.LeScanCallback scanCallback;
    private Runnable rStopScan;
    private Runnable rAdapterNotify;

    private ListView lvwDevices;
    private DeviceListAdapter adapter;

    private boolean isScanning = false;

    private MenuItem scanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rStopScan = new Runnable() {
            @Override
            public void run() {
                stopBleSearch();
            }
        };

        rAdapterNotify = new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                if (isScanning) handler.postDelayed(rAdapterNotify, ADAPTER_NOTIFY_INTERVAL);
            }
        };

        handler = new Handler();

        lvwDevices = findViewById(R.id.lvw_main_devices);
        adapter = new DeviceListAdapter(this);
        lvwDevices.setAdapter(adapter);

        lvwDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = adapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
                intent.putExtra("btdevice", device);
                stopBleSearch();
                startActivity(intent);
            }
        });

        // this callback will be called when a new BLE device is found
        scanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                int deviceIdx = getDeviceIdxInList(adapter.devices, device);
                if (deviceIdx == -1) {
                    adapter.devices.add(device);
                } else {
                    adapter.devices.set(deviceIdx, device);
                }
                //adapter.notifyDataSetChanged();
            }
        };

        // initialize bluetooth things
        bluetoothManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            // this device does not support bluetooth?
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (isScanning) {
            menu.findItem(R.id.action_main_search).setIcon(null);
            menu.findItem(R.id.action_main_search).setTitle(R.string.main_action_search_stop);
        } else {
            menu.findItem(R.id.action_main_search).setIcon(R.drawable.ic_action_refresh);
            menu.findItem(R.id.action_main_search).setTitle(R.string.main_action_search);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_main_search:
                if (isScanning) stopBleSearch();
                else startBleSearch();
                return true;
            case R.id.action_main_settings:
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBleSearch();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBleSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_PERM);
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                startBleSearch();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    startBleSearch();
                }
            } else {
                finish();
            }
        }
    }

    private int getDeviceIdxInList(List<BluetoothDevice> list, BluetoothDevice device) {
        return list.indexOf(device);
    }

    private void startBleSearch() {
        if (isScanning) return;

        adapter.devices.clear();
        adapter.notifyDataSetChanged();

        // search for 10 seconds and stop
        handler.postDelayed(rStopScan, 10000);
        handler.postDelayed(rAdapterNotify, ADAPTER_NOTIFY_INTERVAL);
        bluetoothAdapter.startLeScan(scanCallback);
        isScanning = true;

        invalidateOptionsMenu();
    }

    private void stopBleSearch() {
        if (!isScanning) return;

        handler.removeCallbacks(rStopScan);
        handler.removeCallbacks(rAdapterNotify);
        adapter.notifyDataSetChanged();
        bluetoothAdapter.stopLeScan(scanCallback);
        isScanning = false;

        invalidateOptionsMenu();
    }

    private class DeviceListAdapter extends BaseAdapter {
        private Context context;
        public List<BluetoothDevice> devices;

        public DeviceListAdapter(Context context) {
            this.context = context;
            devices = new ArrayList<BluetoothDevice>();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return devices.get(position);
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
                convertView = inflater.inflate(R.layout.list_devices, parent, false);

                holder = new ListItemViewHolder();
                holder.tvwName = convertView.findViewById(R.id.tvw_list_device_name);
                holder.tvwDesc = convertView.findViewById(R.id.tvw_list_device_details);

                convertView.setTag(holder);
            } else {
                holder = (ListItemViewHolder)convertView.getTag();
            }

            holder.tvwName.setText((devices.get(position).getName() == null) ? getString(R.string.main_null_device_name) : devices.get(position).getName());
            holder.tvwDesc.setText(devices.get(position).getAddress());

            return convertView;
        }

        private class ListItemViewHolder {
            TextView tvwName;
            TextView tvwDesc;
        }
    }
}
