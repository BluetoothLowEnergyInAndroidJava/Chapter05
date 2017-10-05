package tonyg.example.com.exampleblescan;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import tonyg.example.com.exampleblescan.ble.BleCommManager;
import tonyg.example.com.exampleblescan.ble.BlePeripheral;

/**
 * Connect to a BLE Device, list its GATT services
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */
public class ConnectActivity extends AppCompatActivity {
    /** Constants **/
    private static final String TAG = ConnectActivity.class.getSimpleName();
    public static final String PERIPHERAL_MAC_ADDRESS_KEY = "com.example.com.exampleble.PERIPHERAL_MAC_ADDRESS";

    /** Bluetooth Stuff **/
    private BleCommManager mBleCommManager;
    private BlePeripheral mBlePeripheral;

    /** Functional stuff **/
    private String mPeripheralMacAddress;
    private String mBlePeripheralName;

    /** Activity State **/
    private boolean mBleConnected = false;
    private boolean mLeaveActivity = false;

    /** UI Stuff **/
    private MenuItem mProgressSpinner;
    private MenuItem mConnectItem, mDisconnectItem;
    private TextView mPeripheralBroadcastNameTV, mPeripheralAddressTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // grab information passed to the savedInstanceState,
        // from when the user clicked on the list in MainActivty
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mPeripheralMacAddress = extras.getString(PERIPHERAL_MAC_ADDRESS_KEY);
            }
        } else {
            mPeripheralMacAddress = savedInstanceState.getString(PERIPHERAL_MAC_ADDRESS_KEY);
        }


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loadUI();

        mBlePeripheral = new BlePeripheral();

    }


    /**
     * Prepare the UI elements
     */
    public void loadUI() {
        mPeripheralBroadcastNameTV = (TextView)findViewById(R.id.broadcast_name);
        mPeripheralAddressTV = (TextView)findViewById(R.id.mac_address);
    }




    @Override
    public void onResume() {
        super.onResume();
        // connect the bluetooth device
        initializeBluetooth();

    }

    @Override
    public void onPause() {
        super.onPause();
        disconnect();
    }


    /**
     * Create the menu
     *
     * @param menu
     * @return <b>true</b> if successful
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);

        mConnectItem = menu.findItem(R.id.action_connect);
        mDisconnectItem =  menu.findItem(R.id.action_disconnect);
        mProgressSpinner = menu.findItem(R.id.scan_progress_item);

        initializeBluetooth();
        connect();

        return true;
    }


    /**
     * User clicked a menu button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_connect:
                // User chose the "Scan" item
                connect();
                return true;

            case R.id.action_disconnect:
                // User chose the "Stop" item
                mLeaveActivity = true;
                quitActivity();
                return true;

            default:
                // The user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    /**
     * Turn on Bluetooth radio
     */
    public void initializeBluetooth() {
        try {
            mBleCommManager = new BleCommManager(this);
        } catch (Exception e) {
            Toast.makeText(this, "Could not initialize bluetooth", Toast.LENGTH_SHORT).show();
            Log.e(TAG, e.getMessage());
            finish();
        }
    }


    /**
     * Connect to Peripheral
     */
    public void connect() {
        // grab the Peripheral Device address and attempt to connect
        BluetoothDevice bluetoothDevice = mBleCommManager.getBluetoothAdapter().getRemoteDevice(mPeripheralMacAddress);
        mProgressSpinner.setVisible(true);
        try {
            mBlePeripheral.connect(bluetoothDevice, mGattCallback, getApplicationContext());
        } catch (Exception e) {
            mProgressSpinner.setVisible(false);
            Log.e(TAG, "Error connecting to peripheral");
        }
    }

    /**
     * Disconnect from Peripheral
     */
    public void disconnect() {
        // disconnect from the Peripheral.
        mProgressSpinner.setVisible(true);
        mBlePeripheral.disconnect();
    }

    /**
     * Bluetooth Peripheral connected.  Update UI
     */
    public void onBleConnected() {
        // update UI to reflect a connection
        BluetoothDevice bluetoothDevice = mBlePeripheral.getBluetoothDevice();
        mBlePeripheralName = bluetoothDevice.getName();
        mPeripheralBroadcastNameTV.setText(bluetoothDevice.getName());
        mPeripheralAddressTV.setText(bluetoothDevice.getAddress());
        mConnectItem.setVisible(false);
        mDisconnectItem.setVisible(true);
        mProgressSpinner.setVisible(false);

    }

    /**
     *  Quit the activity if the Peripheral is disconnected.  Otherwise disconnect and try again
     */
    public void quitActivity() {
        if (!mBleConnected) {
            finish();
        } else {
            disconnect();
        }
    }

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /**
         * Characteristic value changed
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        }

        /**
         * Peripheral connected or disconnected
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // There has been a connection or a disconnection with a Peripheral.
            // If this is a connection, update the UI to reflect the change
            // and discover the GATT profile of the connected Peripheral
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "Connected to peripheral");
                mBleConnected = true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBleConnected();
                    }
                });

                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBlePeripheral.close();
                mBleConnected = false;
                if (mLeaveActivity) quitActivity();
            }
        }

        /**
         * Gatt Profile discovered
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
        }
    };
}
