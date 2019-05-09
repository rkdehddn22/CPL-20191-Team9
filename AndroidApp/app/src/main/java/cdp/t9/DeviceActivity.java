package cdp.t9;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

    AlertDialog adProgress;

    TextView tvwInfo;
    TextView tvwStatus;
    TextView tvwData;

    TextView tvwEmpty;

    ListView lvwValues;
    TrackingValueAdapter adapter;
    ArrayList<TrackingValue> trackingValues;

    boolean connected;

    Handler handler;
    Runnable rRefresher;
    final int refreshRate = 200;

    private class MessageData {
        byte timestamp;
        byte reserved1;     // 3?
        byte msgId;
        byte msgSize;
        byte[] msg;
    }

    private SparseArray<MessageData> messages;
    private AlertDialog adAddValue;
    private EditText edtAddName;
    private RadioButton rdoAddTypeDigital;
    private RadioButton rdoAddTypeAnalog;
    private LinearLayout llAddAnalog;
    private TextView tvwAddLength;
    private Button btnAddLengthAdd;
    private Button btnAddLengthSub;
    private EditText edtAddResolution;
    private Spinner spnAddPosId;
    private TextView tvwAddPosPos;
    private Button btnAddPosPosAdd;
    private Button btnAddPosPosSub;
    private TextView tvwAddPreview;

    private int addValLength = 1;
    private int addBytePos = 0;

    private ArrayList<String> messageIds;
    private ArrayAdapter<String> sapiAdapter;

    private Runnable rUpdateAVPreview;
    private Runnable rTrackValues;
    private final int trackRate = 100;

    private boolean isAddValueValid = false;
    private boolean isAddValueDialogShown = false;
    private int addValueDialogTarget = -1;

    private float scale;

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

                adProgress.cancel();

                handler.postDelayed(rRefresher, refreshRate);

                Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        // notification enable
                        try {
                            final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(2).get(0);
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
                // not called?
                byte[] sendByte = intent.getByteArrayExtra("init");

                if ((sendByte[0] == 0x55) && (sendByte[1] == 0x33)) {
                    Log.d(TAG, "======= Init Setting Data ");
                    updateCommandState("Init Data");

                    Log.d("asdf", "asdfasdgghrjrj");

                    Handler mHandler = new Handler();
                    mHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            // notification enable
                            try {
                                final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(2).get(0);
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

                    /*
                    String s = "";
                    s = tvwText.getText().toString();
                    s += "Rx: " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                    tvwText.setMovementMethod(new ScrollingMovementMethod());
                    tvwText.setText(s + "\r\n");

                    int scrollamout = tvwText.getLayout().getLineTop(tvwText.getLineCount()) - tvwText.getHeight();
                    if (scrollamout > 0)
                        tvwText.scrollTo(0, scrollamout);
                    */

                    byte[] bData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_BYTES);

                    MessageData newMsg = new MessageData();
                    newMsg.timestamp = bData[0];
                    newMsg.reserved1 = bData[1];
                    newMsg.msgId = bData[2];
                    newMsg.msgSize = bData[3];
                    newMsg.msg = new byte[bData[3]];
                    for (int i = 4; i < bData.length; i++) {
                        newMsg.msg[i - 4] = bData[i];
                    }

                    processMessage(newMsg);
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

        handler = new Handler();
        rRefresher = new Runnable() {
            @Override
            public void run() {
                sendData(":STT");
                handler.postDelayed(rRefresher, refreshRate);
            }
        };

        scale = getResources().getDisplayMetrics().density;

        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(deviceName);

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setCancelable(true);
        b.setView(R.layout.layout_connecting);
        b.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!connected) finish();
            }
        });
        adProgress = b.create();

        tvwInfo = findViewById(R.id.tvw_device_info);
        tvwStatus = findViewById(R.id.tvw_device_status);
        tvwData = findViewById(R.id.tvw_device_data);
        tvwEmpty = findViewById(R.id.tvw_device_empty);

        tvwInfo.setText(String.format(getString(R.string.device_info), deviceName, device.getAddress()));
        updateConnectionState(R.string.device_disconnected);
        displayData(null);

        lvwValues = findViewById(R.id.lvw_device_values);
        trackingValues = new ArrayList<>();
        adapter = new TrackingValueAdapter(trackingValues);
        lvwValues.setAdapter(adapter);

        lvwValues.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                addValueDialogTarget = position;
                openAddValueDialog();
            }
        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        adProgress.show();

        AlertDialog.Builder ba = new AlertDialog.Builder(this);
        ba.setTitle(R.string.add_title_new)
        .setView(R.layout.layout_add_value)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(android.R.string.cancel, null);

        adAddValue = ba.create();

        rUpdateAVPreview = new Runnable() {
            @Override
            public void run() {
                try {
                    MessageData md = messages.valueAt(spnAddPosId.getSelectedItemPosition());
                    int v = 0;
                    if (rdoAddTypeDigital.isChecked()) {    // digital
                        v = (int)(md.msg[addBytePos] & 0xff);
                    } else {                                // analog
                        int p = addBytePos;
                        int s = addValLength;
                        while (true) {
                            v = v + (int)(md.msg[p + (s - 1)] & 0xff);
                            s--;
                            if (s == 0) break;
                            v = v * 0x100;
                        }
                    }
                    isAddValueValid = true;
                    tvwAddPreview.setText(String.valueOf(v));
                } catch (Exception e) {
                    isAddValueValid = false;
                    tvwAddPreview.setText(R.string.add_error_invalid_value);
                }
                if (adAddValue.isShowing()) handler.postDelayed(rUpdateAVPreview, 250);
            }
        };

        rTrackValues = new Runnable() {
            @Override
            public void run() {
                if (trackingValues.size() > 0) updateAllTrackingValue();
                handler.postDelayed(rTrackValues, trackRate);
            }
        };

        handler.postDelayed(rTrackValues, trackRate);

        messageIds = new ArrayList<>();
        sapiAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, messageIds);

        messages = new SparseArray<>();

        Log.d("BLEApp", "DeviceActivity Ready");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connected) handler.removeCallbacks(rRefresher);
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (connected) handler.postDelayed(rRefresher, refreshRate);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_device_add:
                addValueDialogTarget = -1;
                openAddValueDialog();
                return true;
            case R.id.action_device_load_profile:
                ArrayList<TrackingValue> newTvList = new ArrayList<>();
                String loadedData;
                try {
                    FileInputStream fis = openFileInput("value_profile");
                    BufferedReader br = new BufferedReader( new InputStreamReader(fis, Charset.forName("UTF-8")));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().length() == 0) continue;
                        TrackingValue newTv = new TrackingValue();
                        newTv.loadFromString(line);
                        newTvList.add(newTv);
                    }
                    fis.close();
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, R.string.device_error_nothing_to_load, Toast.LENGTH_SHORT).show();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.device_error_load_io_fail, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (newTvList.size() > 0) {
                    trackingValues.clear();
                    trackingValues.addAll(newTvList);
                    tvwEmpty.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.device_load_completed, Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetInvalidated();
                } else {
                    Toast.makeText(this, R.string.device_error_nothing_to_load, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_device_save_profile:
                if (trackingValues.size() == 0) {
                    Toast.makeText(this, R.string.device_error_nothing_to_save, Toast.LENGTH_SHORT).show();
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (TrackingValue tv : trackingValues) {
                    sb.append(tv.saveToString()).append("\n");
                }
                sb.setLength(sb.length() - 1);
                try {
                    FileOutputStream fos = openFileOutput("value_profile", MODE_PRIVATE);
                    fos.write(sb.toString().getBytes(Charset.forName("UTF-8")));
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.device_error_save_io_fail, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Toast.makeText(this, R.string.device_save_completed, Toast.LENGTH_SHORT).show();
                return true;
        }
        return false;
    }

    private void openAddValueDialog() {
        adAddValue.show();
        if (!isAddValueDialogShown) {
            edtAddName = adAddValue.findViewById(R.id.edt_add_name);
            rdoAddTypeDigital = adAddValue.findViewById(R.id.rdo_add_type_digital);
            rdoAddTypeAnalog = adAddValue.findViewById(R.id.rdo_add_type_analog);
            llAddAnalog = adAddValue.findViewById(R.id.ll_add_analog);
            tvwAddLength = adAddValue.findViewById(R.id.tvw_add_length);
            btnAddLengthAdd = adAddValue.findViewById(R.id.btn_add_length_add);
            btnAddLengthSub = adAddValue.findViewById(R.id.btn_add_length_sub);
            edtAddResolution = adAddValue.findViewById(R.id.edt_add_resolution);
            spnAddPosId = adAddValue.findViewById(R.id.spn_add_pos_id);
            tvwAddPosPos = adAddValue.findViewById(R.id.tvw_add_pos_pos);
            btnAddPosPosAdd = adAddValue.findViewById(R.id.btn_add_pos_pos_add);
            btnAddPosPosSub = adAddValue.findViewById(R.id.btn_add_pos_pos_sub);
            tvwAddPreview = adAddValue.findViewById(R.id.tvw_add_preview);

            rdoAddTypeDigital.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    llAddAnalog.setVisibility(isChecked ? View.GONE : View.VISIBLE);
                    if (isChecked) {
                        addValLength = 1;
                        tvwAddLength.setText(String.valueOf(addValLength));
                    }
                }
            });

            btnAddLengthAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addValLength++;
                    tvwAddLength.setText(String.valueOf(addValLength));
                }
            });

            btnAddLengthSub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addValLength--; if (addValLength < 1) addValLength = 1;
                    tvwAddLength.setText(String.valueOf(addValLength));
                }
            });

            spnAddPosId.setAdapter(sapiAdapter);

            btnAddPosPosAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addBytePos++;
                    tvwAddPosPos.setText(String.valueOf(addBytePos));
                }
            });

            btnAddPosPosSub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addBytePos--; if (addBytePos < 0) addBytePos = 0;
                    tvwAddPosPos.setText(String.valueOf(addBytePos));
                }
            });

            adAddValue.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (edtAddName.length() == 0) {
                        Toast.makeText(DeviceActivity.this, R.string.add_error_name_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!Util.tryParseInt(edtAddResolution.getText().toString())) {
                        Toast.makeText(DeviceActivity.this, R.string.add_error_invalid_resolution, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!isAddValueValid) {
                        Toast.makeText(DeviceActivity.this, R.string.add_error_invalid_value, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (addValueDialogTarget == -1) {
                        trackingValues.add(createNewTrackingValue(edtAddName.getText().toString(),
                                (rdoAddTypeDigital.isChecked() ? TrackingValueType.DIGITAL : TrackingValueType.ANALOG),
                                messages.keyAt(spnAddPosId.getSelectedItemPosition()),
                                Integer.parseInt(edtAddResolution.getText().toString()),
                                addValLength,
                                addBytePos,
                                rdoAddTypeAnalog.isChecked()));
                    } else {
                        TrackingValue newTv = createNewTrackingValue(edtAddName.getText().toString(),
                                (rdoAddTypeDigital.isChecked() ? TrackingValueType.DIGITAL : TrackingValueType.ANALOG),
                                messages.keyAt(spnAddPosId.getSelectedItemPosition()),
                                Integer.parseInt(edtAddResolution.getText().toString()),
                                addValLength,
                                addBytePos,
                                rdoAddTypeAnalog.isChecked());
                        trackingValues.remove(addValueDialogTarget);
                        trackingValues.add(addValueDialogTarget, newTv);
                    }
                    adapter.notifyDataSetChanged();
                    tvwEmpty.setVisibility(View.GONE);
                    adAddValue.dismiss();
                }
            });

            isAddValueDialogShown = true;
        }
        if (addValueDialogTarget == -1) {
            addValLength = 1;
            addBytePos = 0;
            edtAddName.setText("");
            rdoAddTypeDigital.setChecked(true);
            rdoAddTypeAnalog.setChecked(false);
            llAddAnalog.setVisibility(View.GONE);
            tvwAddLength.setText("1");
            edtAddResolution.setText("1");
            spnAddPosId.setSelection(0);
            tvwAddPosPos.setText("0");
        } else {
            TrackingValue tv = trackingValues.get(addValueDialogTarget);
            addValLength = tv.size;
            addBytePos = tv.bytePos;
            edtAddName.setText(tv.name);
            rdoAddTypeDigital.setChecked(tv.type == TrackingValueType.DIGITAL);
            rdoAddTypeAnalog.setChecked(tv.type == TrackingValueType.ANALOG);
            llAddAnalog.setVisibility((tv.type == TrackingValueType.ANALOG) ? View.VISIBLE : View.GONE);
            tvwAddLength.setText(String.valueOf(addValLength));
            edtAddResolution.setText(String.valueOf(tv.max));
            spnAddPosId.setSelection(messageIds.indexOf(Util.bytesToHex((byte)tv.msgId)));
            tvwAddPosPos.setText(String.valueOf(addBytePos));
        }
        handler.postDelayed(rUpdateAVPreview, 250);
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

    /*
    private void sendData(){
        if(gattCharacteristics != null) {
            try {
                final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(2).get(0);
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
    */

    private void sendData(String message){
        if(gattCharacteristics != null) {
            try {
                final BluetoothGattCharacteristic characteristic = gattCharacteristics.get(2).get(0);
                bleService.writeCharacteristics(characteristic, message);
                updateCommandState("Write Data");
                displayData(message);
            } catch (Exception e) {
                e.printStackTrace();
                bleService.disconnect();
                Toast.makeText(DeviceActivity.this, R.string.device_status_invalid_device, Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    private int dpToPx(int dp) {
        return (int)(dp * scale + 0.5f);
    }

    private void processMessage(MessageData msg) {
        if (messages.get(msg.msgId) == null) {
            messages.put(msg.msgId, msg);
            messageIds.clear();
            for (int i = 0; i < messages.size(); i++) {
                messageIds.add(Util.bytesToHex(messages.valueAt(i).msgId));
            }
            sapiAdapter.notifyDataSetChanged();
        } else {
            messages.put(msg.msgId, msg);
        }
    }

    private void updateAllTrackingValue() {
        for (TrackingValue tv : trackingValues) {
            if (tv.type == TrackingValueType.DIGITAL) {
                tv.value = messages.get(tv.msgId).msg[tv.bytePos];
                if (tv.digitalBits != null) {
                    // TODO flickering when scrolled?
                    for (int i = 0; i < 8; i++) {
                        tv.digitalBits[i].setText(((tv.value / (int)(Math.pow(2, i)) & 1) == 0) ? "○" : "●");
                    }
                }
            } else {    // analog
                int v = 0;
                int p = tv.bytePos;
                int s = tv.size;
                while (true) {
                    v = v + (int)(messages.get(tv.msgId).msg[p + (s - 1)] & 0xff);
                    s--;
                    if (s == 0) break;
                    v = v * 0x100;
                }
                tv.value = v;
                tv.analogGraphLastX += 0.1d;
                tv.analogData.appendData(new DataPoint(tv.analogGraphLastX, tv.value), true, 60);
                if (tv.oobIndicator != null) tv.oobIndicator.setVisibility((tv.value > tv.max) ? View.VISIBLE : View.GONE);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private TrackingValue createNewTrackingValue(String name, TrackingValueType type, int msgId, int max, int valLength, int bytePos, boolean isAnalog) {
        TrackingValue newTv = new TrackingValue();
        newTv.name = name;
        newTv.type = type;
        newTv.msgId = msgId;
        if (isAnalog) {
            newTv.size = valLength;
            newTv.max = max;

            newTv.analogData = new LineGraphSeries<>();
            newTv.analogData.setDrawDataPoints(false);
            newTv.analogData.setDataPointsRadius(10);
        }
        newTv.bytePos = bytePos;
        //trackingValues.add(newTv);
        return newTv;
    }

    private static enum TrackingValueType {
        DIGITAL,
        ANALOG
    }

    private class TrackingValue {
        String name;
        TrackingValueType type;
        int msgId;
        int bytePos;
        int size;
        int max;
        int value;

        // analog things
        TextView oobIndicator;
        LineGraphSeries<DataPoint> analogData;
        double analogGraphLastX = 0d;

        // digital things
        TextView[] digitalBits;

        public String saveToString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("\n")
              .append((type == TrackingValueType.ANALOG) ? "analog" : "digital").append("\n")
              .append(msgId).append("\n")
              .append(bytePos).append("\n")
              .append(size).append("\n")
              .append(max).append("\n");
            byte[] d = sb.toString().getBytes(Charset.forName("UTF-8"));
            return Base64.encodeToString(d, Base64.DEFAULT);
        }

        public void loadFromString(String savedString) {
            byte[] d = Base64.decode(savedString, Base64.DEFAULT);
            String data[] = new String(d, Charset.forName("UTF-8")).split("\n");
            name = data[0];
            type = (data[1].equalsIgnoreCase("analog") ? TrackingValueType.ANALOG : TrackingValueType.DIGITAL);
            msgId = Integer.parseInt(data[2]);
            bytePos = Integer.parseInt(data[3]);
            size = Integer.parseInt(data[4]);
            max = Integer.parseInt(data[5]);

            if (type == TrackingValueType.ANALOG) {
                analogData = new LineGraphSeries<>();
                analogData.setDrawDataPoints(false);
                analogData.setDataPointsRadius(10);
            }
        }
    }

    private class TrackingValueAdapter extends BaseAdapter {
        ArrayList<TrackingValue> tvalue;

        public TrackingValueAdapter(ArrayList<TrackingValue> tvalue) {
            this.tvalue = tvalue;
        }

        @Override
        public int getCount() {
            return tvalue.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListItemViewHolder holder;

            if (convertView == null || ((ListItemViewHolder)convertView.getTag()).id != tvalue.get(position).hashCode()) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                convertView = inflater.inflate(R.layout.list_tvalue, parent, false);

                holder = new ListItemViewHolder();
                holder.tvwName = convertView.findViewById(R.id.tvw_tvalue_name);
                holder.tvwType = convertView.findViewById(R.id.tvw_tvalue_type);
                holder.tvwAnalogValue = convertView.findViewById(R.id.tvw_tvalue_analog_value);
                holder.tvwAnalogOob = convertView.findViewById(R.id.tvw_tvalue_analog_oob);
                holder.llAnalog = convertView.findViewById(R.id.ll_tvalue_analog);
                holder.llAnalogParent = convertView.findViewById(R.id.ll_tvalue_analog_parent);
                holder.llDigital = convertView.findViewById(R.id.ll_tvalue_digital);

                if (tvalue.get(position).type == TrackingValueType.ANALOG) {
                    holder.tvwBits = null;
                    holder.llDigital.setVisibility(View.GONE);
                    holder.gvGraph = new GraphView(DeviceActivity.this);
                    holder.gvGraph.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(100)));
                    holder.gvGraph.getViewport().setXAxisBoundsManual(true);
                    holder.gvGraph.getViewport().setMinX(0);
                    holder.gvGraph.getViewport().setMaxX(6);
                    holder.gvGraph.getViewport().setYAxisBoundsManual(true);
                    holder.gvGraph.getViewport().setMinY(0);
                    holder.gvGraph.getViewport().setMaxY(tvalue.get(position).max);
                    holder.gvGraph.getViewport().setDrawBorder(false);
                    holder.gvGraph.getGridLabelRenderer().setHighlightZeroLines(false);
                    holder.gvGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
                    holder.gvGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
                    holder.gvGraph.addSeries(tvalue.get(position).analogData);
                    holder.llAnalog.addView(holder.gvGraph, 0);
                } else {
                    holder.gvGraph = null;
                    holder.llAnalogParent.setVisibility(View.GONE);
                    holder.tvwBits = new TextView[8];
                    holder.tvwBits[0] = convertView.findViewById(R.id.tvw_tvalue_bit_0);
                    holder.tvwBits[1] = convertView.findViewById(R.id.tvw_tvalue_bit_1);
                    holder.tvwBits[2] = convertView.findViewById(R.id.tvw_tvalue_bit_2);
                    holder.tvwBits[3] = convertView.findViewById(R.id.tvw_tvalue_bit_3);
                    holder.tvwBits[4] = convertView.findViewById(R.id.tvw_tvalue_bit_4);
                    holder.tvwBits[5] = convertView.findViewById(R.id.tvw_tvalue_bit_5);
                    holder.tvwBits[6] = convertView.findViewById(R.id.tvw_tvalue_bit_6);
                    holder.tvwBits[7] = convertView.findViewById(R.id.tvw_tvalue_bit_7);
                    tvalue.get(position).digitalBits = holder.tvwBits;
                }

                tvalue.get(position).oobIndicator = holder.tvwAnalogOob;

                holder.id = tvalue.get(position).hashCode();

                convertView.setTag(holder);
            } else {
                holder = (ListItemViewHolder)convertView.getTag();
            }

            holder.tvwName.setText(tvalue.get(position).name);
            holder.tvwType.setText(tvalue.get(position).type == TrackingValueType.DIGITAL ? "디지털" : "아날로그");
            holder.tvwAnalogValue.setText(String.valueOf(tvalue.get(position).value));

            return convertView;
        }

        private class ListItemViewHolder {
            int id;
            TextView tvwName;
            TextView tvwType;
            TextView tvwAnalogValue;
            TextView tvwAnalogOob;
            LinearLayout llAnalog;
            LinearLayout llAnalogParent;
            LinearLayout llDigital;
            GraphView gvGraph;
            TextView[] tvwBits;
        }
    }
}
