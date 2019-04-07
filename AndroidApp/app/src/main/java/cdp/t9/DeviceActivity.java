package cdp.t9;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class DeviceActivity extends AppCompatActivity {
    BluetoothDevice device;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCallback callback;

    AlertDialog progress;

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

        callback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d("BLEApp", "Device Connected");

                        progress.dismiss();
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
                    for (BluetoothGattService a : bluetoothGatt.getServices()) {
                        Log.d("BLEApp", "Service: " + a.getUuid().toString());
                    }
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
}
