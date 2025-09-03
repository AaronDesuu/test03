/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.gattApp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final int STATE_INITIATE = -1;
    private static final int STATE_INITIALIZE = 0;
    private static final int STATE_DISCONNECTED = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_CONNECTED = 3;
    private static final int STATE_DISCOVERED = 4;

    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private String mBluetoothDeviceAddress = null;;
    private BluetoothGatt mBluetoothGatt = null;;
    private int mConnectionState = STATE_INITIATE;
    private BluetoothGattService mService  = null;;
    private BluetoothGattCharacteristic mCharacteristic;

    public final static String ACTION_GATT_ERROR =
            "com.fujielectricmeter.bluetooth.le.ACTION_GATT_ERROR";
    public final static String ACTION_GATT_CONNECTED =
            "com.fujielectricmeter.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.fujielectricmeter.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.fujielectricmeter.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.fujielectricmeter.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.fujielectricmeter.bluetooth.le.EXTRA_DATA";

    private final UUID uuid_service = UUID.fromString("b973f2e0-b19e-11e2-9e96-0800200c9a66");
    private final UUID uuid_read  = UUID.fromString("d973f2e1-b19e-11e2-9e96-0800200c9a66");
    private final UUID uuid_write = UUID.fromString("e973f2e2-b19e-11e2-9e96-0800200c9a66");
    private final UUID uuid_config = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if(mConnectionState==STATE_CONNECTING) {
                    mConnectionState = STATE_CONNECTED;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    mBluetoothGatt.discoverServices();
                    Log.i(TAG, "Connected to GATT server and call discoverServices.");
                }
                else{
                    broadcastUpdate(ACTION_GATT_ERROR);
                }

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if(mConnectionState==STATE_CONNECTING){
                    mBluetoothDeviceAddress = null;
                }
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                Log.i(TAG, "Disconnected from GATT server.");
            }
            else{
                Log.i(TAG, String.format("Other state received: %d", newState));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mConnectionState = STATE_DISCOVERED;
                mService = mBluetoothGatt.getService(uuid_service);
                mCharacteristic = mService.getCharacteristic(uuid_read);
                BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(uuid_config);
//              mBluetoothGatt.requestConnectionPriority(CONNECTION_PRIORITY_HIGH);
                mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
            else{
                broadcastUpdate(ACTION_GATT_ERROR);
            }
            Log.w(TAG, "onServicesDiscovered received: " + status);
        }
        @Override
        public void onCharacteristicWrite( BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic, int status
        ){
           super.onCharacteristicWrite(gatt, characteristic, status);
           Log.w(TAG, "onCharacteristicWrite: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead( gatt, characteristic, status );
            Log.w(TAG, "onCharacteristicRead: " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public boolean write(final byte[] in) {
        if(mConnectionState==STATE_DISCOVERED) {
            Log.i(TAG, String.format("write:%d", in.length));
            if (in != null) {
                BluetoothGattCharacteristic Characteristic =
                        mBluetoothGatt.getService(uuid_service).getCharacteristic(uuid_write);
                Characteristic.setValue(in);
                mBluetoothGatt.writeCharacteristic(Characteristic);
                return true;
            } else {
                return false;
            }
        }
        else{
            Log.i(TAG, "Dose not ready to write");
            return false;
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        if (uuid_read.equals(characteristic.getUuid())) {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                intent.putExtra(EXTRA_DATA, data);
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        else {
            mConnectionState = STATE_INITIALIZE;
        }
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mConnectionState < STATE_INITIALIZE) {
            Log.w(TAG, "Dose not initialized");
            return false;
        }
        if(mConnectionState>=STATE_CONNECTED) {
            Log.w(TAG, "Dose not disconnect before connect");
            disconnect();
            return false;
        }
        else {
            // Previously connected device.  Try to reconnect.
            if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
                if (mBluetoothGatt.connect()) {
                    mConnectionState = STATE_CONNECTING;
                    Log.d(TAG, "Try to use an existing mBluetoothGatt for connection.");
                    return true;
                } else {
                    Log.d(TAG, "Fail to use an existing mBluetoothGatt for connection.");
                }
            }
            mBluetoothDeviceAddress = null;
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
            mConnectionState = STATE_INITIALIZE;

            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            Log.d(TAG, "Trying to create a new connection.");
            mBluetoothDeviceAddress = address;
            mConnectionState = STATE_CONNECTING;
            return true;
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if(mConnectionState>=STATE_CONNECTED) {
            mBluetoothGatt.disconnect();
        }
    }
}
