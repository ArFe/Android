package br.com.ideiageni.uct;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by ariel on 31/10/2016.
 */

public class BTCommClass {

    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    //private TextView isSerial;
    private TextView mConnectionState;
    private TextView mDataField;
    private Button mSendButton;
    private String mDeviceName;
    private String mDeviceAddress;
    //  private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    private int waitTime = 10;
    private byte[] readData;
    private Activity mMainActivity;


    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public BTCommClass(Activity mainActivity, String deviceName, String deviceAddress) {
        mMainActivity = mainActivity;
        mDeviceName = deviceName;
        mDeviceAddress = deviceAddress;


        // Sets up UI references.
        ((TextView) mMainActivity.findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) mMainActivity.findViewById(R.id.connection_state);
        // is serial present?
        //isSerial = (TextView) mMainActivity.findViewById(R.id.isSerial);

        mDataField = (TextView) mMainActivity.findViewById(R.id.data_value);
        mSendButton= (Button) mMainActivity.findViewById(R.id.button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessage(new byte[]{0x01,0x03,0x00,0x10,0x00,0x10,0x45,-0x3D});
            }
        });

        if(mMainActivity.getActionBar() != null) {
            mMainActivity.getActionBar().setTitle(mDeviceName);
            mMainActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent gattServiceIntent = new Intent(mMainActivity.getApplicationContext(), BluetoothLeService.class);
        mMainActivity.bindService(gattServiceIntent, mServiceConnection, MainActivity.BIND_AUTO_CREATE);
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //finish();
            } else {
                // Automatically connects to the device upon successful start-up initialization.
                mBluetoothLeService.connect(mDeviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                mMainActivity.invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                mMainActivity.invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                read(intent.getByteArrayExtra(BluetoothLeService.EXTRA_BYTES));
            }
        }
    };

    private void clearUI() {
        mDataField.setText("");
    }


    public void onResume() {
        mMainActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    public void onPause() {
        mMainActivity.unregisterReceiver(mGattUpdateReceiver);
    }

    public void onDestroy() {
        mMainActivity.unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    private void updateConnectionState(final int resourceId) {
        mMainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {

        if (data != null) {
            final String text = mDataField.getText() + data;
            mDataField.setText(text);
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = mMainActivity.getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
            //if(SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") { isSerial.setText("Yes, serial :-)"); } else {  isSerial.setText("No, serial :-("); }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void read (byte[] data) {
        byte[] c = readData;
        if (data != null) {
            if(readData == null){
                c = data;
            } else {
                Log.d("Modbus Master", "readData " + readData.length + " data " + data.length);
                c = new byte[readData.length + data.length];
                System.arraycopy(readData, 0, c, 0, readData.length);
                System.arraycopy(data, 0, c, readData.length, data.length);
            }
        }
        readData = c;
        Log.d("Modbus Master", "iAvailable " + readData.length);
    }

    public void SendMessage(byte[] data) {
        mDataField.setText("");
        if(mConnected) {
            characteristicTX.setValue(data);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
        }
    }

    public byte[] ReceiveMessage(int iAvailable){
        int i;
        byte[] readTemp;

        readTemp = new byte[iAvailable];
        for (i = 0; i < iAvailable; i++) {
            readTemp[i] = readData[i];
         }
        readData = null;
        return readTemp;
    }

    public int getAvailable(){
        if(readData == null) return 0;
        else return readData.length;
    }

    public boolean isConnected() {return mConnected;}

    public void connect() {
        if (mBluetoothLeService.initialize()) {
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }
    }

}
