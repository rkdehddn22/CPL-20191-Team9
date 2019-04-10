package cdp.t9;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceActivity extends AppCompatActivity {
    private final static String TAG = DeviceActivity.class.getSimpleName();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    BluetoothDevice device;
    BluetoothLeService bleService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> gattCharacteristics = new ArrayList<>();

    String deviceName;

    AlertDialog progress;

    TextView tvwInfo;
    TextView tvwStatus;
    TextView tvwData;
    TextView tvwText;
    EditText edtSend;
    Button btnSend;

    boolean connected;

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                updateConnectionState(R.string.device_status_connected);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                updateConnectionState(R.string.device_status_disconnected);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(bleService.getSupportedGattServices());

                //getDeviceSetting();

                Log.d(TAG, "======= Init Setting Data ");
                updateCommandState("Init Data");


                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // notification enable
                        try {
                            final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(3).get(1);
                            //final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(1).get(1);
                            bleService.setCharacteristicNotification(characteristic, true);
                        } catch (Exception e) {
                            e.printStackTrace();
                            bleService.disconnect();
                            Toast.makeText(DeviceActivity.this, R.string.device_status_invalid_device, Toast.LENGTH_SHORT).show();
                        }
                    }

                }, 1000);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                byte[] sendByte = intent.getByteArrayExtra("init");

                if ((sendByte[0] == 0x55) && (sendByte[1] == 0x33)) {
                    Log.d(TAG, "======= Init Setting Data ");
                    updateCommandState("Init Data");

                    Handler mHandler = new Handler();
                    mHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            // notification enable
                            try {
                                final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(3).get(1);
                                //final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(1).get(1);
                                bleService.setCharacteristicNotification(characteristic, true);
                            } catch (Exception e) {
                                e.printStackTrace();
                                bleService.disconnect();
                                Toast.makeText(DeviceActivity.this, R.string.device_status_invalid_device, Toast.LENGTH_SHORT).show();
                            }
                        }

                    }, 1000);
                }

                if ((sendByte[0] == 0x55) && (sendByte[1] == 0x03)) {
                    Log.d(TAG, "======= SPP READ NOTIFY ");
                    updateCommandState("SPP READ");

                    byte notifyValue = sendByte[2];

                    String s = "";
                    s = tvwText.getText().toString();
                    s += "Rx: " + Util.unHex(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    tvwText.setMovementMethod(new ScrollingMovementMethod());
                    tvwText.setText(s + "\r\n");

                    int scrollamout = tvwText.getLayout().getLineTop(tvwText.getLineCount()) - tvwText.getHeight();
                    if (scrollamout > 0)
                        tvwText.scrollTo(0, scrollamout);
                }

                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bleService.connect(device.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleService = null;
        }
    };

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

        deviceName = device.getName() == null ? getString(R.string.main_null_device_name) : device.getName();

        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(deviceName);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCancelable(false);
        b.setView(R.layout.layout_connecting);
        progress = b.create();

        tvwInfo = findViewById(R.id.tvw_device_info);
        tvwStatus = findViewById(R.id.tvw_device_status);
        tvwData = findViewById(R.id.tvw_device_data);
        tvwText = findViewById(R.id.tvw_device_text);
        edtSend = findViewById(R.id.edt_device_send);
        btnSend = findViewById(R.id.btn_device_send);

        tvwInfo.setText(String.format(getString(R.string.device_info), deviceName, device.getAddress()));
        updateConnectionState(R.string.device_disconnected);
        displayData(null);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected) return;

                if (edtSend.length() < 1) {
                    Toast.makeText(DeviceActivity.this, R.string.device_send_no_message, Toast.LENGTH_SHORT).show();
                    return;
                } else if (edtSend.length() > 20) {
                    Toast.makeText(DeviceActivity.this, R.string.device_send_too_long_message, Toast.LENGTH_SHORT).show();
                    return;
                }

                sendData();

                StringBuilder sb = new StringBuilder();
                sb.append(tvwText.getText().toString())
                        .append("Tx: ")
                        .append(edtSend.getText().toString())
                        .append("\r\n");
                tvwText.setText(sb);
            }
        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        Log.d("BLEApp", "DeviceActivity Ready");
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bleService != null) {
            final boolean result = bleService.connect(device.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bleService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvwStatus.setText(resourceId);
            }
        });
    }

    private void updateCommandState(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvwStatus.setText(String.format(getString(R.string.device_status), str));
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            tvwData.setText(String.format(getString(R.string.device_data), data));
        } else {
            tvwData.setText(String.format(getString(R.string.device_data), getString(R.string.device_data_null)));
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        gattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            Log.d(TAG, "service uuid : " + uuid);

            currentServiceData.put(LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> svcGattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : svcGattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.d(TAG, "gattCharacteristic uuid : " + uuid);

                currentCharaData.put(LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            gattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

       /* SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter); */


        Log.d(TAG, "service read ok ");
    }


    private void sendData(){
        if(gattCharacteristics != null) {
            try {
                final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(3).get(0);
                bleService.writeCharacteristics(characteristic, edtSend.getText().toString());
                updateCommandState("Write Data");
                displayData(edtSend.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
                bleService.disconnect();
                Toast.makeText(DeviceActivity.this, R.string.device_status_invalid_device, Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }
}
