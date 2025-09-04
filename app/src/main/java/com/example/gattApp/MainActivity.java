package com.example.gattApp;

import static java.lang.Integer.parseInt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;

import com.example.gattApp.ui.ItemFragment;
import com.google.android.material.navigation.NavigationView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.example.gattApp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements
        ItemFragment.messageManager {

    public static StringBuffer CounterParameter = new StringBuffer();
    public static StringBuffer CounterParameter1 = new StringBuffer();
    public static StringBuffer CounterParameter2 = new StringBuffer();
    public SoundPool soundPool;
    private int[] sound = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final String TAG = MainActivity.class.getSimpleName();
    private AppBarConfiguration mAppBarConfiguration;
    // Stops scanning after 10 seconds.
    public static int mScan = 3000;
    public static int mTick = 250;
    public static int mInterval = 0;
    private static final int timeout = 30;
    private ArrayList<String> mReceive;
    private String mAddressShort = null;
    private byte mSel, mDataIndex;
    private StringBuffer mParameter = new StringBuffer();
    private String mPassword;
    private BluetoothLeService mBluetoothLeService;
    private String mAddress;
    private String mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private int mCurrentMessage;
    private boolean mArrived;
    private byte[] mData;
    private int mTimer;
    private int mCount;
    private int mTotal;
    private int mStage;
    private int mSubStage;
    private int mStep;
    private int mPrmState;
    public static DLMS d;
    private Handler mHandler;
    private int mSelect;
    private boolean mBind;
    private boolean mService;
    private boolean mScanning;
    private int mConnected;
    private boolean mMultiple;
    private static final int REQUEST_ENABLE_BT = 1;
    private ItemFragment mItemFragment;
    private String mTitle = null;
    private TextView btnConnect;
    private TextView btnDisconnect;
    private View connectStatusIndicator;
    private View disconnectStatusIndicator;

    private void setupToolbarButtons() {
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        connectStatusIndicator = findViewById(R.id.connect_status_indicator);
        disconnectStatusIndicator = findViewById(R.id.disconnect_status_indicator);

        LinearLayout connectContainer = findViewById(R.id.btn_connect_container);
        LinearLayout disconnectContainer = findViewById(R.id.btn_disconnect_container);

        // Set click listeners on both container and text view
        View.OnClickListener connectListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onConnectButtonClicked();
            }
        };

        View.OnClickListener disconnectListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDisconnectButtonClicked();
            }
        };

        if (btnConnect != null) btnConnect.setOnClickListener(connectListener);
        if (connectContainer != null) connectContainer.setOnClickListener(connectListener);

        if (btnDisconnect != null) btnDisconnect.setOnClickListener(disconnectListener);
        if (disconnectContainer != null) disconnectContainer.setOnClickListener(disconnectListener);

        updateToolbarButtonStates();
    }

    private void onConnectButtonClicked() {
        Log.i(TAG, "Connect button clicked");

        if (mConnected > 0 && mAddress != null) {
            // If already connected, this acts as a "new scan" - disconnect first
            Disconnect(true);
        }

        scanLeDevice();
    }

    private void onDisconnectButtonClicked() {
        Log.i(TAG, "Disconnect button clicked");

        // Immediately update UI to show disconnecting state
        btnDisconnect.setEnabled(false);
        btnDisconnect.setText("Disconnecting...");
        disconnectStatusIndicator.setSelected(false);
        disconnectStatusIndicator.setActivated(true); // Orange for disconnecting

        Handler handler = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int ret = 0;
                ret = Release();
                if (ret > 0) {
                    handler.postDelayed(this, mTick);
                } else {
                    Disconnect(true);
                    scanLeDevice();
                }
            }
        };
        handler.post(r);
    }

    private void updateToolbarButtonStates() {
        if (btnConnect == null || btnDisconnect == null) return;

        LinearLayout connectContainer = findViewById(R.id.btn_connect_container);
        LinearLayout disconnectContainer = findViewById(R.id.btn_disconnect_container);

        if (mService) {
            if (mConnected > 0 && mAddress != null) {
                // Connected state - show disconnect, hide connect
                if (connectContainer != null) connectContainer.setVisibility(View.GONE);
                if (disconnectContainer != null) disconnectContainer.setVisibility(View.VISIBLE);

                btnDisconnect.setEnabled(true);
                btnDisconnect.setText("Disconnect");
                if (disconnectStatusIndicator != null) {
                    disconnectStatusIndicator.setVisibility(View.VISIBLE);
                    disconnectStatusIndicator.setSelected(true);  // Green dot
                    disconnectStatusIndicator.setActivated(false);
                }

            } else if (mScanning) {
                // Scanning state - show connect as scanning
                if (connectContainer != null) connectContainer.setVisibility(View.VISIBLE);
                if (disconnectContainer != null) disconnectContainer.setVisibility(View.GONE);

                btnConnect.setEnabled(false);
                btnConnect.setText("Scanning...");
                if (connectStatusIndicator != null) {
                    connectStatusIndicator.setVisibility(View.VISIBLE);
                    connectStatusIndicator.setSelected(false);
                    connectStatusIndicator.setActivated(true);  // Orange dot
                }

            } else {
                // Disconnected state - show connect as ready
                if (connectContainer != null) connectContainer.setVisibility(View.VISIBLE);
                if (disconnectContainer != null) disconnectContainer.setVisibility(View.GONE);

                btnConnect.setEnabled(true);
                btnConnect.setText("Connect");
                if (connectStatusIndicator != null) {
                    connectStatusIndicator.setVisibility(View.VISIBLE);
                    connectStatusIndicator.setSelected(false);
                    connectStatusIndicator.setActivated(false);  // Red dot
                }
            }
        } else {
            // Service not available - show connect as disabled
            if (connectContainer != null) connectContainer.setVisibility(View.VISIBLE);
            if (disconnectContainer != null) disconnectContainer.setVisibility(View.GONE);

            btnConnect.setEnabled(false);
            btnConnect.setText("Connect");
            if (connectStatusIndicator != null) {
                connectStatusIndicator.setVisibility(View.VISIBLE);
                connectStatusIndicator.setSelected(false);
                connectStatusIndicator.setActivated(false);  // Red dot
            }
        }
    }

    private void Disconnect(final boolean all) {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
        if (all) {
            mStage = 0;
            mStep = 0;
            mPrmState = 0;
        }
        d.updateAllMeterInformation();
        mItemFragment.DeviceInfo("No E-meter selected...");
        mAddress = null;
        mDevice = null;
        updateToolbarButtonStates(); // Add this
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            ) {
                // パーミッションの許可を取得する
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
            }

        } else {
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                    (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ) {
                // パーミッションの許可を取得する
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstancewrite_read) {
        super.onCreate(savedInstancewrite_read);
        Log.i(TAG, "onCreate");
        if (d == null) {
            d = new DLMS(getApplicationContext());
        } else {
            Log.i(TAG, "re-use d.");
        }
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the main toolbar for navigation
        setSupportActionBar(binding.appBarMain.toolbar);

        // Setup the overflow menu on the second toolbar
        androidx.appcompat.widget.Toolbar overflowToolbar = findViewById(R.id.overflow_toolbar);
        if (overflowToolbar != null) {
            overflowToolbar.inflateMenu(R.menu.main);
            overflowToolbar.setOnMenuItemClickListener(new androidx.appcompat.widget.Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onOptionsItemSelected(item);
                }
            });
        }

        setupToolbarButtons();

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_item0, R.id.nav_item1, R.id.nav_item2, R.id.nav_item3, R.id.nav_item4, R.id.nav_item5)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH}, 1);
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 2);
        }
/*
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 4);
*/

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            showToast("PackageManager.FEATURE_BLUETOOTH_LE");
            finish();
            return;
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            showToast("Bluetooth IC dose not found!");
            finish();
            return;
        }
        if (mHandler == null) {
            mHandler = new Handler();
        } else {
            Log.i(TAG, "reuse mHandler.");
        }
        if (mLeDeviceListAdapter == null) {
            mLeDeviceListAdapter = new LeDeviceListAdapter();
        } else {
            Log.i(TAG, "reuse mLeDeviceListAdapter.");
        }

        // Get the intent that started this activity
        Intent intent = getIntent();
        if (intent.getType() != null) {
            ClipData clip = intent.getClipData();
            String data = clip.toString();
            // Figure out what to do based on the intent type
            if (intent.getType().equals("text/plain")) {
                // Handle intents with text ...
                // Create intent to deliver some kind of result data
                Intent result = new Intent("com.fujielectricmeter.com.example.gattApp.RESULT_ACTION", Uri.parse("content://result_uri"));
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        }
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                // USAGE_MEDIA
                // USAGE_GAME
                .setUsage(AudioAttributes.USAGE_MEDIA)
                // CONTENT_TYPE_MUSIC
                // CONTENT_TYPE_SPEECH, etc.
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(10)
                .build();

        sound[0] = soundPool.load(this, R.raw.itemget4, 1);
        sound[1] = soundPool.load(this, R.raw.itemget5, 1);
        sound[2] = soundPool.load(this, R.raw.itemget6, 1);
        sound[3] = soundPool.load(this, R.raw.picon, 1);
        sound[4] = soundPool.load(this, R.raw.receipt01, 1);
        sound[5] = soundPool.load(this, R.raw.receipt02, 1);
        sound[6] = soundPool.load(this, R.raw.receipt03, 1);
        sound[7] = soundPool.load(this, R.raw.receipt07, 1);
        sound[8] = soundPool.load(this, R.raw.savepoint2, 1);
        sound[9] = soundPool.load(this, R.raw.savepoint3, 1);

        // load が終わったか確認する場合
        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
            Log.d("debug", "sampleId=" + sampleId);
            Log.d("debug", "status=" + status);
        });

        // Initializes list view adapter.
        mScanning = false;
        mBind = false;
        mService = false;
        mConnected = 0;

        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
        mStage = 0;
        mStep = 0;
        mPrmState = 0;
        mItemFragment.DeviceInfo("No E-meter selected...");
        mAddress = null;
        mDevice = null;

        int tick = parseInt(d.readTick());
        if (tick > 99 && tick < 1001) {
            mTick = tick;
        } else {
            mTick = 200;
            d.writeTick("200");
        }
        int scan = parseInt(d.readScan());
        if (scan > 999 && scan < 10001) {
            mScan = scan;
        } else {
            mScan = 3000;
            d.writeScan("3000");
        }
        d.readMeterInformation();
        mTitle = mMsgLabel[0];
        mAddressShort = "UnknownMeter";
        mItemFragment.SetData(d.readViewData());
        mInterval = 0;
        mCurrentMessage = -1;
    }

    /*
        private final BroadcastReceiver receiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                    int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                    String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                    Log.i(TAG, name + ": " + rssi + "dBm");
                }
            }
        };
    */
    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
        Log.i(TAG, "onResume.");

        // Fix for Android 13+ (API 33+) BroadcastReceiver registration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter(), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        Disconnect(true);
        unregisterReceiver(mGattUpdateReceiver);
//      unregisterReceiver(receiver);
    }

    private void writeFile(String data, File file) {
        // try-with-resources
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean ret;
        final Handler handler;
        final Runnable r;

        Log.i(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            // Remove case R.id.menu_scan: and case R.id.menu_disconnect: blocks
            // as they are now handled by toolbar buttons

            case R.id.menu_select:
                Disconnect(true);
                handler = new Handler();
                r = new Runnable() {
                    @Override
                    public void run() {
                        int ret = fragmentMessage(0);
                        if (ret > 0) {
                            handler.postDelayed(this, mTick);
                        }
                    }
                };
                handler.post(r);
                ret = true;
                break;

            case R.id.menu_share:
                if (!mItemFragment.getData().isEmpty()) {
                    // ② 現在時刻を取得してくる値を 変数 date に格納
                    Date date = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_hhmmss");
                    String name = String.format("%s_%s_%s.txt", mTitle, mAddressShort, sdf.format(date).toString()).replace(':', '-');
                    if (false) {
//                Intent intent = new Intent(Intent.ACTION_SENDTO);
                        Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.setData(Uri.parse("mailto:")); // only email apps should handle this
//                String[] addresses = new String[1];
//                byte[] b = MainActivity.d.getEmail();
//                String s = MainActivity.d.setStr2Str(b, 0, b.length);
//                addresses[0] = s;
//                intent.putExtra(Intent.EXTRA_EMAIL, addresses);
                        intent.putExtra(Intent.EXTRA_SUBJECT, name);
                        intent.putExtra(Intent.EXTRA_TEXT, mItemFragment.getData());
                        intent.setType("text/plain");
//                Intent shareIntent = Intent.createChooser(intent, null);
                        startActivity(intent);
//                startActivity(shareIntent);
                    } else {
                        File file = new File(this.getCacheDir(), name);
                        writeFile(mItemFragment.getData(), file);
                        Uri contentUri = FileProvider.getUriForFile(this, "com.fujielectricmeter.com.example.gattApp", file);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        shareIntent.setType("text/plain");
                        this.startActivity(Intent.createChooser(shareIntent, "choose"));
                    }
                }
                ret = true;
                break;

            default:
                ret = super.onOptionsItemSelected(item);
                break;
        }
        return ret;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        // Don't inflate menu here anymore - it's handled by overflow toolbar

        // Update the overflow toolbar menu items
        androidx.appcompat.widget.Toolbar overflowToolbar = findViewById(R.id.overflow_toolbar);
        if (overflowToolbar != null && overflowToolbar.getMenu() != null) {
            Menu overflowMenu = overflowToolbar.getMenu();

            if (mService) {
                if (overflowMenu.findItem(R.id.menu_select) != null) {
                    overflowMenu.findItem(R.id.menu_select).setVisible(true);
                }
            } else {
                if (overflowMenu.findItem(R.id.menu_select) != null) {
                    overflowMenu.findItem(R.id.menu_select).setVisible(false);
                }
            }

            if (mItemFragment != null && mItemFragment.getData().isEmpty()) {
                if (overflowMenu.findItem(R.id.menu_share) != null) {
                    overflowMenu.findItem(R.id.menu_share).setVisible(true);
                    overflowMenu.findItem(R.id.menu_share).setEnabled(false);
                }
            } else {
                if (overflowMenu.findItem(R.id.menu_share) != null) {
                    overflowMenu.findItem(R.id.menu_share).setVisible(true);
                    overflowMenu.findItem(R.id.menu_share).setEnabled(true);
                }
            }
        }

        updateToolbarButtonStates();
        return true;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.i(TAG, "onCreateOptionsMenu");
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//
//        if (mService) {
//            menu.findItem(R.id.menu_select).setVisible(true);
//        } else {
//            menu.findItem(R.id.menu_select).setVisible(false);
//        }
//
//        if (mItemFragment != null && mItemFragment.getData().isEmpty()) {
//            menu.findItem(R.id.menu_share).setVisible(true);
//            menu.findItem(R.id.menu_share).setEnabled(false);
//        } else {
//            menu.findItem(R.id.menu_share).setVisible(true);
//            menu.findItem(R.id.menu_share).setEnabled(true);
//        }
//
//        // Update toolbar button states whenever menu is created
//        updateToolbarButtonStates();
//
//        return true;
//    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void run() {
                            if (device.getName() != null) {
                                if (device.getName().contains("LWF") || device.getName().contains("F5")) {
                                    mLeDeviceListAdapter.addDevice(device, rssi);
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
                }
            };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_ERROR);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//        intentFilter.addAction(BluetoothLeService.EXTRA_DATA);
        return intentFilter;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            showToast("You choose disable bluetooth. Exit this app");
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("MissingPermission")
    // Replace your existing scanLeDevice method with this updated version:
    private void scanLeDevice() {
        if (!mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            // Update button states immediately when starting scan
            updateToolbarButtonStates();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "scanLeDevice-run");
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mItemFragment.DeviceInfo("No E-meter selected...");
                    updateToolbarButtonStates(); // Update buttons when scan completes
                    invalidateOptionsMenu();
                }
            }, mScan);

            mItemFragment.DeviceInfo("Scan...");
            Log.i(TAG, "scanLeDevice-true");
            mScanning = true;
            mLeDeviceListAdapter.clear();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            updateToolbarButtonStates(); // Update buttons when scan starts
        }
    }

    // Adapter for holding devices found through scanning.
    public class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<Integer> mrssi;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mrssi = new ArrayList<Integer>();
        }

        public void addDevice(BluetoothDevice device, Integer rssi) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                mrssi.add(rssi);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            mrssi.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }
        @SuppressLint("MissingPermission")
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            class ViewHolder {
                TextView deviceName;
            }
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
                view = inflater.inflate(android.R.layout.simple_list_item_1, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(android.R.id.text1);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                int find = d.findMeter(device.getAddress());
                if (find != 0) {
                    d.changeCurrent(find);
                    Long before = d.Access();
                    if (before > 0) {
                        Long diff = System.currentTimeMillis() - d.Access();
                        diff /= 1000;
                        Long day = diff / 86400;
                        diff %= 86400;
                        Long hour = diff / 3600;
                        diff %= 3600;
                        Long min = diff / 60;
                        if (day > 0) {
                            viewHolder.deviceName.setText(String.format("%s (%d dBm) - %d days ago", device.getAddress(), mrssi.get(i), day.intValue()));
                        } else {
                            if (hour > 0) {
                                viewHolder.deviceName.setText(String.format("%s (%d dBm) - %d hour ago ", device.getAddress(), mrssi.get(i), hour.intValue()));
                            } else {
                                viewHolder.deviceName.setText(String.format("%s (%d dBm) - %d min ago", device.getAddress(), mrssi.get(i), min.intValue()));
                            }
                        }
                    } else {
                        viewHolder.deviceName.setText(String.format("%s (%d dBm)", device.getAddress(), mrssi.get(i)));
                    }
                } else {
                    viewHolder.deviceName.setText(String.format("%s (%d dBm) - New!", device.getAddress(), mrssi.get(i)));
                }
            } else {
                viewHolder.deviceName.setText("Unknown");
            }
            return view;
        }
    }

    private boolean showCandidateMeter() {

        boolean ret = false;

        if (!mScanning) {
            mSelect = 0;
            ret = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("E-Meter list");
            if (mLeDeviceListAdapter.getCount() > 0) {
                builder.setAdapter(mLeDeviceListAdapter, new DialogInterface.OnClickListener() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        mSelect = 1;
                        mAddress = mLeDeviceListAdapter.getDevice(position).getAddress();
                        mDevice = mLeDeviceListAdapter.getDevice(position).getName();
                        Log.i(TAG, "onSelectList");
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        mSelect = -1;
                        Log.i(TAG, "onCancel");
                    }
                });
            } else {
                builder.setMessage("No meter could be found!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mSelect == 0) {
                            mSelect = -1;
                            Log.i(TAG, "onClick");
                        }
                    }
                });
            }
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (mSelect == 0) {
                        mSelect = -1;
                        Log.i(TAG, "onDismiss");
                    }
                }
            });
            builder.show();
        }
        return ret;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder write_read) {
            Log.i(TAG, "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) write_read).getService();
            if (!mBluetoothLeService.initialize()) {
                showToast("Fail to initialize of Bluetooth service.");
                finish();
            }
            mService = true;
            mBind = true;
            updateToolbarButtonStates(); // Add this
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            updateToolbarButtonStates(); // Add this
        }
    };
    // Handles various events fired by the write_read.
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
                Log.i(TAG, "ACTION_GATT_CONNECTED");
                updateToolbarButtonStates(); // Add this
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG, "ACTION_GATT_DISCONNECTED");
                mConnected = 0;
                mAddress = null;
                mDevice = null;
                invalidateOptionsMenu();
                updateToolbarButtonStates();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG, "ACTION_GATT_SERVICES_DISCOVERED");
                mConnected = 1;
                mArrived = false;
                invalidateOptionsMenu();
                updateToolbarButtonStates();
            } else if (BluetoothLeService.ACTION_GATT_ERROR.equals(action)) {
                mConnected = -1;
                Log.i(TAG, "ACTION_GATT_ERROR");
                updateToolbarButtonStates(); // Add this
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i(TAG, "ACTION_DATA_AVAILABLE");
                mData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                mArrived = true;
            }
        }
    };

    public int Release() {
        int ret = 0;
        if (mTimer > timeout) {
            mTimer = 0;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Communication timeout detected !");
            builder.setMessage("No data received from meter.\nAbort connection. please try again.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Disconnect(true);
                    scanLeDevice();
                }
            });
            builder.show();
        } else {
            switch (mStage) {
                case 2:
                    ret = sessionRelease();
                    if (ret == 2) {
                        mStage--;
                    }
                    break;
                case 1:
                    break;
                default:
                    if (ret == 0) {
                        mSubStage = 0;
                    }
                    break;
            }
        }
        return ret;
    }

    private int sessionRelease() {
        byte[] send = null;
        int ret;
        int[] res = new int[2];
        ret = 0;/*0:fail,1:processing,2:established*/

        switch (mStep) {
            case 0:
                mItemFragment.Progress("Release session...", mTimer, 0);
                send = d.Release();
                if (send != null) {
                    mStep++;
                    mTimer = 0;
                    mBluetoothLeService.write(send);
                    ret = 1;
                    Log.i(TAG, String.format("Release:%d", send.length));
                }
                break;
            case 2:
                send = d.Close(res, mData);
                if (res[0] != 0) {
                    if (send != null) {
                        mItemFragment.Progress("Close connecting...", mTimer, 0);
                        mStep++;
                        mTimer = 0;
                        mBluetoothLeService.write(send);
                        ret = 1;
                        Log.i(TAG, String.format("Open:%d", send.length));
                    }
                }
                break;
            case 4:
                d.Finish(res, mData);
                if (res[0] != 0) {
                    mItemFragment.Progress("Finish.", mTimer, 0);
                    ret = 2;
                    Log.i(TAG, "Finish");
                } else {
                    mItemFragment.Progress("Fail to finish.", mTimer, 0);
                }
                break;
            case 1:
            case 3:
                mTimer++;
                if (mArrived) {
                    mArrived = false;
                    mStep++;
                    Log.i(TAG, String.format("mArrived:%d", mData.length));
                }
                ret = 1;
                break;
            default:
                break;
        }
        if (ret != 1) {
            mStep = 0;
        }
        return ret;
    }


    private int Connection1() {
        int ret = 0;

        switch (mStep) {
            case 0:
                if (!mService) {
                    if (mBind) {
                        unbindService(mServiceConnection);
                        mBind = false;
                    }
                    Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                    Log.i(TAG, "bindService");
                    mStep++;
                    mTimer = 0;
                } else {
                    mStep++;
                }
                ret = 1;
                break;
            case 1:
                if (!mService) {
                    mTimer++;
                } else {
                    mStep++;
                    mTimer = 0;
                }
                ret = 1;
                break;
            case 2:
                if (showCandidateMeter()) {
                    mStep++;
                }
                ret = 1;
                break;
            case 3:
                ret = 1;
                if (mSelect != 0) {
                    mStep++;
                }
                break;
            case 4:
                if (mAddress != null) {
                    if (mBluetoothLeService.connect(mAddress)) {
                        mItemFragment.Progress("BLE service connecting...", 0, 0);
                        mAddressShort = mAddress.replace(":", "");
                        mStep++;
                        mTimer = 0;
                        ret = 1;
                    } else {
                        mItemFragment.Progress("Fail to connect service...", 0, 0);
                        mService = false;
                        ret = -1;
                        mSelect = 0;
                    }
                } else {
                    ret = -1;
                    mSelect = 0;
                    /*mSelect = -1(Cancel)*/
                }
                break;
            case 5:
                ret = 1;
                if (mConnected != 0) {
                    mStep++;
                } else {
                    mTimer++;   /*20*/
                }
                break;
            case 6:
                if (mConnected > 0) {
                    d.setCurrentMeter(mAddress, mDevice);
                    mStep++;
                    mTimer = 0;
                    ret = 2;
                } else {
                    mItemFragment.Progress("Reject to connect service...", 0, 0);
                    ret = -1;
                }
                break;
            default:
                break;
        }

        if (ret != 1) {
            mStep = 0;
        }
        return ret;
    }

    private int Connection2() {
        int ret = 0;

        switch (mStep) {
            case 0:
                if (!mService) {
                    if (mBind) {
                        unbindService(mServiceConnection);
                        mBind = false;
                    }
                    Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
                    Log.i(TAG, "bindService");
                    mStep++;
                    mTimer = 0;
                } else {
                    mStep++;
                }
                ret = 1;
                break;
            case 1:
                if (!mService) {
                    mTimer++;
                } else {
                    scanLeDevice();
                    mStep++;
                    mTimer = 0;
                }
                ret = 1;
                break;
            case 2:
                if (!mScanning) {
                    for (int i = 1; i < d.Count(); i++) {
                        String Select = d.getPhysical(i);
                        for (int j = 0; j < mLeDeviceListAdapter.getCount(); j++) {
                            String Address = mLeDeviceListAdapter.getDevice(j).getAddress();
                            if (Address.equals(Select)) {
                                int now = d.Status(i);
                                if (now > 0) {
                                    d.Status(11, i);
                                }
                            }
                        }
                    }
                    mStep++;
                    mTimer = 0;
                } else {
                    ret = 1;
                }
                break;
            case 3:
                ret = 1;
                if (mSelect != 0) {
                    mStep++;
                }
                break;
            case 4:
                if (mAddress != null) {
                    int max = d.Count();
                    for (int i = mSelect; i < max; i++) {
                        int now = d.Status(i);
                        if (now == 11) {
                            mAddress = d.getPhysical(i);
                            mDevice = d.getDevice(i);
                            if (mBluetoothLeService.connect(mAddress)) {
                                mItemFragment.Progress("BLE service connecting...", 0, 0);
                                mAddressShort = mAddress.replace(":", "");
                                mStep++;
                                mTimer = 0;
                                ret = 1;
                            } else {
                                mItemFragment.Progress("Fail to connect service...", 0, 0);
                                mService = false;
                                ret = -1;
                            }
                            d.Status(1, i);
                            mSelect = i;
                            break;
                        }
                    }
                } else {
                    ret = -1;
                    mSelect = 0;
                    /*mSelect = -1(Cancel)*/
                }
                break;
            case 5:
                ret = 1;
                if (mConnected != 0) {
                    mStep++;
                } else {
                    mTimer++;   /*20*/
                }
                break;
            case 6:
                if (mConnected > 0) {
                    d.setCurrentMeter(mAddress, mDevice);
                    mStep++;
                    mTimer = 0;
                    ret = 2;
                } else {
                    mItemFragment.Progress("Reject to connect service...", 0, 0);
                    ret = -1;
                }
                break;
            default:
                break;
        }

        if (ret != 1) {
            mStep = 0;
        }
        return ret;
    }

    private int sessionEstablish() {
        byte[] send = null;
        int ret;
        int[] res = new int[2];
        ret = 0;/*0:fail,1:processing,2:established*/

        switch (mStep) {
            case 0:
                mItemFragment.Progress("HDLC connecting...", mTimer, 0);
                send = d.Open();
                if (send != null) {
                    mStep++;
                    mTimer = 0;
                    mBluetoothLeService.write(send);
                    ret = 1;
                    Log.i(TAG, String.format("Open:%d", send.length));
                }
                break;
            case 2:
                send = d.Session(res, mData);
                if (res[0] != 0) {
                    if (send != null) {
                        mItemFragment.Progress("Session...", mTimer, 0);
                        mStep++;
                        mTimer = 0;
                        mBluetoothLeService.write(send);
                        ret = 1;
                        Log.i(TAG, String.format("Session:%d", send.length));
                    }
                } else {
                    ret = -1;
                    mItemFragment.Progress("Fail to connect HDLC.", mTimer, 0);
                }
                break;

            case 4:
                send = d.Challenge(res, mData);
                if (res[0] != 0) {
                    if (send != null) {
                        mItemFragment.Progress("Challenge...", mTimer, 0);
                        mStep++;
                        mTimer = 0;
                        mBluetoothLeService.write(send);
                        ret = 1;
                        Log.i(TAG, String.format("Challenge:%d", send.length));
                    } else {/*チャレンジ不要*/
                        if (d.Rank() == d.RANK_POWER || d.Rank() == d.RANK_READER || d.Rank() == d.RANK_PUBLIC) {
                            mItemFragment.Progress("Established NON/LLS session.", 0, 0);
                            mItemFragment.DeviceInfo(String.format("%s(%s)", d.getDevice(-1), d.getPhysical(-1)));
                            ret = 2;
                        } else {
                            ret = -1;
                            mItemFragment.Progress("Fail to connect AARQ.", mTimer, 0);
                        }
                    }
                } else {
                    ret = -1;
                    mItemFragment.Progress("Fail to establish session.", mTimer, 0);
/*
                    final EditText editText = new EditText(this);
                    editText.setHint("16 characters(padding 0 or trimming)");
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Cannot establish session");
                    builder.setMessage(String.format("Please input correct global key and retry.\nCurrent key is \"%s\"", d.Password()));
                    builder.setView(editText);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            StringBuffer str = new StringBuffer(editText.getText().toString());
                            int len = str.length();
                            if (len != 16) {
                                if (len < 16) {
                                    len = 16 - len;
                                    for (int i = 0; i < len; i++) str.append("0");
                                } else {
                                    str.delete(16, len);
                                }
                            }
                            d.Password(str.toString());
                        }
                    });
                    builder.show();
*/
                }
                break;
            case 6:
                send = d.Confirm(res, mData);
                if (res[0] != 0) {
                    if (d.Rank() == d.RANK_ADMIN || d.Rank() == d.RANK_SUPER) {
                        mItemFragment.Progress("Established HLS session.", 0, 0);
                        mItemFragment.DeviceInfo(String.format("%s(%s)", d.getDevice(-1), d.getPhysical(-1)));
                        ret = 2;
                        Log.i(TAG, "Confirm");
                    } else {

                    }
                } else {
                    ret = -1;
                    mItemFragment.Progress("Fail to challenge.", mTimer, 0);
                }
                break;
            case 1:
            case 3:
            case 5: /*received */
                mTimer++;
                if (mArrived) {
                    mArrived = false;
                    mStep++;
                    Log.i(TAG, String.format("mArrived:%d", mData.length));
                }
                ret = 1;
                break;
            default:
                break;
        }
        if (ret != 1) {
            mStep = 0;
        }
        return ret;
    }

    private int accessData(final int mode, final int index, final int attr, final boolean modeling) {
        byte[] send = null;

        int ret = 0;
        switch (mStep) {
            case 0:
                mCount++;
                switch (mode) {
                    case 0:
                        mItemFragment.Progress("Getting...", mCount, mTotal);
                        send = d.getReq(index, (byte) attr, mSel, mParameter.toString(), mDataIndex);
                        break;
                    case 1:
                        mItemFragment.Progress("Setting...", mCount, mTotal);
                        send = d.setReq(index, (byte) attr, mSel, mParameter.toString(), mDataIndex);
                        break;
                    case 2:
                        mItemFragment.Progress("Calling action...", mCount, mTotal);
                        send = d.actReq(index, (byte) attr, mParameter.toString(), mDataIndex);
                        break;
                }
                mTimer = 0;
                mBluetoothLeService.write(send);
                mStep++;
                ret = 1;
                break;
            case 2:
                int[] res = new int[2];
                res[0] = 0;
                res[1] = 0;
                mReceive = d.DataRes(res, mData, modeling);
                if (res[1] < 0) {
                    mItemFragment.DataArrived(mReceive, true);
                    ret = res[1];
                } else {
                    ret = res[0];
                    mItemFragment.DataArrived(mReceive, res[0] == 0);
                }
                break;
            case 1:
            default:
                mTimer++;
                if (mArrived) {
                    mArrived = false;
                    mStep++;
                    Log.i(TAG, String.format("mArrived:%d", mData.length));
                }
                ret = 1;
                break;
        }
        if (ret != 1) {
            mStep = 0;
        }
        return ret;
    }

    @Override
    public void fragment(ItemFragment fragment) {
        mItemFragment = fragment;
        if (mStage > 1) {
            mItemFragment.DeviceInfo(String.format("%s(%s)", d.getDevice(-1), d.getPhysical(-1)));
        } else {
            mItemFragment.DeviceInfo("No E-meter selected...");
        }
    }

    public final static int MSG_CONNECT = 0;
    public final static int MSG_GET_POTENTIAL = MSG_CONNECT + 1;
    public final static int MSG_GET_STATUS = MSG_GET_POTENTIAL + 1;
    public final static int MSG_GET_CONFIG = MSG_GET_STATUS + 1;
    public final static int MSG_ACT_DEMAND_RESET = MSG_GET_CONFIG + 1;
    public final static int MSG_LOG_METER = MSG_ACT_DEMAND_RESET + 1;
    public final static int MSG_LOG_BILLING = MSG_LOG_METER + 1;
    public final static int MSG_LOG_EVENT = MSG_LOG_BILLING + 1;
    public final static int MSG_LOG_LOAD = MSG_LOG_EVENT + 1;
    public final static int MSG_LOG_AMPERE = MSG_LOG_LOAD + 1;
    public final static int MSG_SET_CLOCK = MSG_LOG_AMPERE + 1;
    public final static int MSG_SET_FILTER = MSG_SET_CLOCK + 1;
    public final static int MSG_SET_PASSWORD = MSG_SET_FILTER + 1;
    public final static int MSG_RESET_EVENT = MSG_SET_PASSWORD + 1;
    public final static int MSG_RESET_LOAD = MSG_RESET_EVENT + 1;
    public final static int MSG_RESET_AMP = MSG_RESET_LOAD + 1;
    public final static int MSG_RESET_BILL = MSG_RESET_AMP + 1;
    public final static int MSG_SET_TYPE = MSG_RESET_BILL + 1;
    public final static int MSG_SET_POWER = MSG_SET_TYPE + 1;
    public final static int MSG_GET_ENERGY = MSG_SET_POWER + 1;
    public final static int MSG_SET_VOLT = MSG_GET_ENERGY + 1;
    public final static int MSG_GET_VOLTAMP = MSG_SET_VOLT + 1;
    public final static int MSG_RESET_ABS = MSG_GET_VOLTAMP + 1;
    public final static int MSG_SET_DEFAULT0 = MSG_RESET_ABS + 1;
    public final static int MSG_SET_DEFAULT1 = MSG_SET_DEFAULT0 + 1;
    public final static int MSG_MISS_NEU = MSG_SET_DEFAULT1 + 1;
    public final static int MSG_MISS_NEU_ON = MSG_MISS_NEU + 1;
    public final static int MSG_MISS_NEU_OFF = MSG_MISS_NEU_ON + 1;
    public final static int MSG_TAMPER_ON = MSG_MISS_NEU_OFF + 1;
    public final static int MSG_TAMPER_OFF = MSG_TAMPER_ON + 1;
    public final static int MSG_GET_SOURCE = MSG_TAMPER_OFF + 1;
    public final static int MSG_SET_SOURCE = MSG_GET_SOURCE + 1;
    public final static int MSG_SET_ELECTROSCOPE = MSG_SET_SOURCE + 1;
    public final static int MSG_CHART_STATUS = MSG_SET_ELECTROSCOPE + 1;
    public final static int MSG_CHART_RECORD = MSG_CHART_STATUS + 1;

    final String[] mMsgLabel = {
            "PreviousData",
            "GET_POTENTIAL",
            "GET_STATUS",
            "GET_CONFIG",
            "ACT_DEMAND_RESET",
            "LOG_METER",
            "LOG_BILLING",
            "LOG_EVENT",
            "LOG_LOAD",
            "LOG_AMPERE",
            "SET_CLOCK",
            "SET_FILTER",
            "SET_PASSWORD",
            "RESET_EVENT",
            "RESET_LOAD",
            "RESET_AMP",
            "RESET_BILL",
            "SET_TYPE",
            "SET_POWER",
            "GET_POWER",
            "SET_VOLT",
            "GET_VOLT",
            "RESET_ABS",
            "SET_DEFAULT0",
            "SET_DEFAULT1",
            "MISS_NEU",
            "MISS_NEU_ON",
            "MISS_NEU_OFF",
            "TAMPER_ON",
            "TAMPER_OFF",
            "GET_SOURCE",
            "SET_SOURCE",
            "SET_ELECTROSCOPE",
            "CHART_STATUS",
            "CHART_RECORD",
    };
    private int mYear;
    private int mMonth;
    private int mDay;
    private int mHour;
    private int mMinute;
    private int mSecond;

    private int Parameter(final int message_id) {

        int ret = 0;

        AlertDialog.Builder builder = null;
        mTitle = mMsgLabel[message_id];
        switch (message_id) {
            case MSG_CHART_STATUS:
            case MSG_CHART_RECORD:
                switch (mSubStage) {
                    case 1:
                    case 5:
                        mSel = 0;
                        mParameter.setLength(0);
                        ret = 3;
                        break;

                    case 3:
                        mSel = 2;
                        mParameter.setLength(0);
                        if (CounterParameter1.length() == 0) {
                            mParameter.append("020406000000010600000000120001120000");
                        } else {
                            mParameter.append(CounterParameter1.toString());
                        }
                        ret = 3;
                        break;

                    case 7:
                        mSel = 2;
                        mParameter.setLength(0);
                        if (CounterParameter2.length() == 0) {
                            mParameter.append("020406000000010600000000120001120000");
                        } else {
                            mParameter.append(CounterParameter2.toString());
                        }
                        ret = 3;
                        break;
                    default:
                        break;
                }
                break;

            case MSG_CONNECT:
            case MSG_GET_POTENTIAL:
            case MSG_GET_SOURCE:
            case MSG_SET_FILTER:
            case MSG_GET_ENERGY:
            case MSG_GET_VOLTAMP:
            case MSG_GET_STATUS:
            case MSG_GET_CONFIG:
                mSel = 0;
                mParameter.setLength(0);
                ret = 3;
                break;

            case MSG_ACT_DEMAND_RESET:
                switch (mStep) {
                    case 0:
                        mSel = 0;
                        mParameter.setLength(0);
                        builder = new AlertDialog.Builder(this);
                        builder.setTitle("Demand reset");
                        builder.setMessage("Demand reset scripts will be issued.\nAre you sure?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mStep = 2;
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mStep = 3;
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 1) {
                                    mStep = 3;
                                }
                            }
                        });
                        builder.show();
                        ret = 1;
                        mStep++;
                        break;
                    case 1:
                        ret = 1;
                        break;
                    case 2:
                        mParameter.append("120001");
                        ret = 3;
                    default:
                        break;
                }
                break;

            case MSG_LOG_METER:
            case MSG_LOG_EVENT:
            case MSG_LOG_BILLING:
            case MSG_LOG_LOAD:
            case MSG_LOG_AMPERE:
                switch (mStep) {
                    case 0:
                        mParameter.setLength(0);
                        final String[] items = {"Buffer", "Date", "Entry"};
                        builder = new AlertDialog.Builder(this);
                        builder.setTitle("Selector");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSel = (byte) which;
                                if (mSel == 0) {
                                    mStep = 6;
                                } else {
                                    mPrmState = 0;
                                    if (mSel == 1) {
                                        //0204020412000809060000010000ff0f02120000090c07e60114ff080000ff800000090c07e60114ff091e00ff8000000100
                                        final Calendar calendar = Calendar.getInstance();
                                        mYear = calendar.get(Calendar.YEAR);
                                        mMonth = calendar.get(Calendar.MONTH);
                                        mDay = calendar.get(Calendar.DAY_OF_MONTH);
                                        mHour = calendar.get(Calendar.HOUR_OF_DAY);
                                        mMinute = calendar.get(Calendar.MINUTE);
                                        mParameter.append("0204020412000809060000010000ff0f02120000");
                                    }
                                    mStep++;
                                }
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 1) {
                                    mStep = 7;
                                }
                            }
                        });
                        builder.show();
                        ret = 1;
                        mStep++;
                        break;
                    case 1:
                    case 3:
                    case 5:
                        ret = 1;
                        break;
                    case 2:
                        if (mSel == 1) {
                            DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                                    mYear = i;
                                    mMonth = i1;
                                    mDay = i2;
                                    mParameter.append(String.format("090c%04x%02x%02xff", mYear, mMonth + 1, mDay));
                                    Log.i(TAG, "onDateSet");
                                    mStep++;
                                }
                            }, mYear, mMonth, mDay);
                            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialogInterface) {
                                    Log.i(TAG, "onCancel");
                                    mStep = 7;
                                }
                            });
                            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    if (mStep == 3) {
                                        Log.i(TAG, "onDismiss");
                                        mStep = 7;
                                    }
                                }
                            });
                            if (mPrmState == 0) {
                                dialog.setTitle("From datetime");
                            } else {
                                dialog.setTitle("To datetime");
                            }
                            dialog.show();
                            mStep++;
                        } else {
                            /*selective access2*/
                            final EditText editText0 = new EditText(this);
                            editText0.setInputType(InputType.TYPE_CLASS_NUMBER);
                            final EditText editText1 = new EditText(this);
                            editText1.setInputType(InputType.TYPE_CLASS_NUMBER);
                            LinearLayout layout = new LinearLayout(getApplicationContext());
                            layout.setOrientation(LinearLayout.VERTICAL);
                            layout.addView(editText0);
                            layout.addView(editText1);
                            editText0.setHint("From entry(older)");
                            editText1.setHint("To entry(newer)");
                            builder = new AlertDialog.Builder(this);
                            builder.setTitle("Entry access");
                            builder.setMessage("Input \"From\" and \"To\" entry\n(Up to 5 digit each)");
                            builder.setView(layout);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String from = editText0.getText().toString();
                                    String to = editText1.getText().toString();
                                    if (from.length() > 0 && to.length() > 0) {
                                        mParameter.append(String.format("020406%08x06%08x", Integer.parseInt(from), Integer.parseInt(to)));
                                        mStep++;
                                    } else {
                                        mStep = 7;
                                    }
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mStep = 7;
                                }
                            });
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    if (mStep == 5) {
                                        mStep = 7;
                                    }
                                }
                            });
                            builder.show();
                            mStep = 5;
                        }
                        ret = 1;
                        break;
                    case 4:
                        TimePickerDialog dialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                                mHour = i;
                                mMinute = i1;
                                mParameter.append(String.format("%02x%02x00ff800000", mHour, mMinute));
                                mStep++;
                            }
                        }, mHour, mMinute, true);
                        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                mStep = 7;
                            }
                        });
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 5) {
                                    mStep = 7;
                                }
                            }
                        });
                        if (mPrmState == 0) {
                            dialog.setTitle("From datetime");
                        } else {
                            dialog.setTitle("To datetime");
                        }
                        dialog.show();
                        ret = 1;
                        mStep++;
                        break;
                    case 6:
                        switch (mSel) {
                            default:
                            case 0:
                                mPrmState = 0;
                                ret = 3;
                                break;
                            case 1:
                                if (mPrmState == 0) {
                                    mPrmState++;
                                    mStep = 2;
                                    ret = 1;
                                } else {
                                    mParameter.append("0100");
                                    mPrmState = 0;
                                    ret = 3;
                                }
                                break;
                            case 2:
                                mParameter.append("120001120000");
                                mPrmState = 0;
                                ret = 3;
                                break;
                        }
                        break;
                    case 7:
                        mPrmState = 0;
                        break;
                }
                break;

            case MSG_SET_CLOCK:
                switch (mStep) {
                    case 0:
                        mSel = 0;
                        mParameter.setLength(0);
                        builder = new AlertDialog.Builder(this);
                        builder.setTitle("Meter clock setting");
                        builder.setMessage("Do you set clock by system?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mPrmState = 0;
                                mStep++;
                            }
                        });
                        builder.setNegativeButton("Manual", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mPrmState = 1;
                                mStep++;
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 1) {
                                    mStep = 5;
                                }
                            }
                        });
                        builder.show();
                        ret = 1;
                        mStep++;
                        break;
                    case 1:
                    case 3:
                        ret = 1;
                        break;
                    case 2:
                        TextView sep1 = new TextView(this);
                        sep1.setText("/");
                        TextView sep2 = new TextView(this);
                        sep2.setText("/");
                        TextView sep3 = new TextView(this);
                        sep3.setText(" ");
                        TextView sep4 = new TextView(this);
                        sep4.setText(":");
                        TextView sep5 = new TextView(this);
                        sep5.setText(":");

                        final LinearLayout layout = new LinearLayout(this);
                        layout.setOrientation(LinearLayout.HORIZONTAL);

                        final EditText edDay = new EditText(this);
                        edDay.setHint("dd");
                        edDay.setInputType(InputType.TYPE_CLASS_NUMBER);
                        layout.addView(edDay);
                        layout.addView(sep1);

                        final EditText edMon = new EditText(this);
                        edMon.setInputType(InputType.TYPE_CLASS_NUMBER);
                        edMon.setHint("MM");
                        layout.addView(edMon);
                        layout.addView(sep2);

                        final EditText edYear = new EditText(this);
                        edYear.setInputType(InputType.TYPE_CLASS_NUMBER);
                        edYear.setHint("yyyy");
                        layout.addView(edYear);
                        layout.addView(sep3);

                        final EditText edHour = new EditText(this);
                        edHour.setInputType(InputType.TYPE_CLASS_NUMBER);
                        edHour.setHint("HH");
                        layout.addView(edHour);
                        layout.addView(sep4);

                        final EditText edMin = new EditText(this);
                        edMin.setInputType(InputType.TYPE_CLASS_NUMBER);
                        edMin.setHint("mm");
                        layout.addView(edMin);
                        layout.addView(sep5);

                        final EditText edSec = new EditText(this);
                        edSec.setInputType(InputType.TYPE_CLASS_NUMBER);
                        edMin.setHint("ss");
                        layout.addView(edSec);

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
                        Date date = new Date();
                        String datetime = sdf.format(date);
                        edYear.setText(datetime.substring(0, 4));
                        edMon.setText(datetime.substring(4, 6));
                        edDay.setText(datetime.substring(6, 8));
                        edHour.setText(datetime.substring(8, 10));
                        edMin.setText(datetime.substring(10, 12));
                        edSec.setText(datetime.substring(12, 14));
                        if (mPrmState == 1) {
                            builder = new AlertDialog.Builder(this);
                            builder.setTitle("Input datetime");
                            builder.setView(layout);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mYear = Integer.parseInt(edYear.getText().toString());
                                    mMonth = Integer.parseInt(edMon.getText().toString());
                                    mDay = Integer.parseInt(edDay.getText().toString());
                                    mHour = Integer.parseInt(edHour.getText().toString());
                                    mMinute = Integer.parseInt(edMin.getText().toString());
                                    mSecond = Integer.parseInt(edSec.getText().toString());
                                    mStep++;
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mStep = 5;
                                }
                            });
                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    if (mStep == 3) {
                                        mStep = 5;
                                    }
                                }
                            });
                            builder.show();
                            mStep++;
                        } else {
                            mYear = Integer.parseInt(edYear.getText().toString());
                            mMonth = Integer.parseInt(edMon.getText().toString());
                            mDay = Integer.parseInt(edDay.getText().toString());
                            mHour = Integer.parseInt(edHour.getText().toString());
                            mMinute = Integer.parseInt(edMin.getText().toString());
                            mSecond = Integer.parseInt(edSec.getText().toString());
                            mStep = 4;
                        }
                        ret = 1;
                        break;
                    case 4:
                        mParameter.append(String.format("090c%04x%02x%02xff%02x%02x%02xff800000",
                                mYear, mMonth, mDay, mHour, mMinute, mSecond));
                        ret = 3;
                    case 5:
                        mPrmState = 0;
                        break;
                }
                break;
            case MSG_SET_SOURCE:
                switch (mStep) {
                    case 0:
                        mParameter.setLength(0);
                        builder = new AlertDialog.Builder(this);
                        builder.setTitle("Change source");
                        builder.setMessage("Change to neutral measure.\nAre you sure?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mStep++;
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mStep = 3;
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 1) {
                                    mStep = 4;
                                }
                            }
                        });
                        builder.show();
                        ret = 1;
                        mStep++;
                        break;
                    case 1:
                        ret = 1;
                        break;
                    case 2:
                        mParameter.append("02071100110011021124114811fa127530");
                        ret = 3;
                        break;
                    case 3:
                        mParameter.append("02071100110011011124114811fa127530");
                        ret = 3;
                        break;
                    default:
                        break;
                }
                break;

            case MSG_SET_DEFAULT0:
                mParameter.setLength(0);
                mParameter.append("020c1200071200001204001224001200001235001200511200801280511203021200001100");
                ret = 3;
                break;

            case MSG_SET_DEFAULT1:
                mParameter.setLength(0);
                mParameter.append("020c1150120622124050120000124000120006121f00120000124900120000120000120000");
                ret = 3;
                break;

            case MSG_SET_PASSWORD:
                switch (mStep) {
                    case 0:
                        mParameter.setLength(0);
                        final EditText editText = new EditText(this);
                        editText.setHint("Up to 32 characters");
                        editText.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder = new AlertDialog.Builder(this);
                        builder.setTitle("Change password");
                        builder.setMessage(String.format("Please input password.\nCurrent is \"%s\"", d.Password(-1)));
                        builder.setView(editText);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                StringBuffer str = new StringBuffer(editText.getText().toString());
                                int len = str.length();
                                if (len != 0) {
                                    if (len > 32) {
                                        str.delete(32, len);
                                    }
                                    mPassword = str.toString();
                                    d.Password(mPassword, -1);
                                    mStep++;
                                } else {
                                    mStep = 3;
                                }
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                mStep = 3;
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 1) {
                                    mStep = 3;
                                }
                            }
                        });
                        builder.show();
                        mStep++;
                        ret = 1;
                        break;
                    case 1:
                        ret = 1;
                        break;
                    case 2:
                        mParameter.setLength(0);
                        mParameter.append(String.format("09%02x", mPassword.length()));
                        for (int i = 0; i < mPassword.length(); i++) {
                            mParameter.append(String.format("%02x", mPassword.getBytes()[i]));
                        }
                        ret = 3;
                    case 3:
                        mPrmState = 0;
                        break;
                }
                break;
            case MSG_RESET_EVENT:
            case MSG_RESET_LOAD:
            case MSG_RESET_AMP:
            case MSG_RESET_BILL:
            case MSG_RESET_ABS:
                switch (mStep) {
                    case 0:
                        mParameter.setLength(0);
                        builder = new AlertDialog.Builder(this);
                        switch (message_id) {
                            case MSG_RESET_EVENT:
                                builder.setTitle("Reset Power quality");
                                builder.setMessage("Power quality reset command will be issued.\nAre you sure?");
                                break;
                            case MSG_RESET_LOAD:
                                builder.setTitle("Reset Load profile");
                                builder.setMessage("Load profile reset command will be issued.\nAre you sure?");
                                break;
                            case MSG_RESET_AMP:
                                builder.setTitle("Reset L1 current profile");
                                builder.setMessage("L1 current profile reset command will be issued.\nAre you sure?");
                                break;
                            case MSG_RESET_BILL:
                                builder.setTitle("Reset Billing data");
                                builder.setMessage("Billing data reset command will be issued.\nAre you sure?");
                                break;
                            case MSG_RESET_ABS:
                                builder.setTitle("Reset Measured data");
                                builder.setMessage("Measured data reset command will be issued.\nAre you sure?");
                                break;
                        }
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mStep++;
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mStep = 3;
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 1) {
                                    mStep = 3;
                                }
                            }
                        });
                        builder.show();
                        ret = 1;
                        mStep++;
                        break;
                    case 1:
                        ret = 1;
                        break;
                    case 2:
                        mParameter.append("0f00");
                        ret = 3;
                    default:
                        break;
                }
                break;

            case MSG_MISS_NEU:
                mDataIndex = 12;
                mParameter.setLength(0);
                ret = 3;
                break;

            case MSG_MISS_NEU_ON:
            case MSG_MISS_NEU_OFF:
                mDataIndex = 12;
                switch (mSubStage) {
                    case 1:
                        mParameter.setLength(0);
                        if (message_id == MSG_MISS_NEU_ON) {
                            mParameter.append("0101020412003c12003211011181");
                        } else {
                            mParameter.append("0101020412000012003211011181");
                        }
                        ret = 3;
                        break;
                    case 3:
                        mParameter.setLength(0);
                        ret = 3;
                        break;
                    default:
                        break;
                }
                break;

            case MSG_TAMPER_ON:
            case MSG_TAMPER_OFF:
                switch (mSubStage) {
                    case 1:
                        mParameter.setLength(0);
                        if (message_id == MSG_TAMPER_ON) {
                            mParameter.append("120200");
                        } else {
                            mParameter.append("120000");
                        }
                        ret = 3;
                        break;
                    case 3:
                        mParameter.setLength(0);
                        ret = 3;
                        break;
                    default:
                        break;
                }
                break;

            case MSG_SET_TYPE:
                switch (mStep) {
                    case 0:
                        mParameter.setLength(0);
                        final String[] items = {"Imp/Exp meter", "ABS meter", "NET meter"};
                        builder = new AlertDialog.Builder(this);
                        builder.setTitle("Meter setting");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPrmState = which;
                                mStep++;
                            }
                        });
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (mStep == 1) {
                                    mStep = 3;
                                }
                            }
                        });
                        builder.show();
                        ret = 1;
                        mStep++;
                        break;
                    case 1:
                        ret = 1;
                        break;
                    case 2:
                        mParameter.append(String.format("11%02x", mPrmState));
                        ret = 3;
                    case 3:
                    default:
                        mPrmState = 0;
                        break;
                }
                break;

            case MSG_SET_POWER:
                switch (mSubStage) {
                    case 1:
                        mParameter.setLength(0);
                        ret = 3;
                        break;
                    case 3:
                        switch (mStep) {
                            case 0:
                                mParameter.setLength(0);
                                mParameter.append("020c");
                                final TextView balanceView = new TextView(this);
                                balanceView.setText("Balance(%):");
                                final EditText editText0 = new EditText(this);
                                editText0.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                editText0.setHint("0.00");

                                final TextView phaseView = new TextView(this);
                                phaseView.setText("Phase(%):");
                                final EditText editText1 = new EditText(this);
                                editText1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                editText1.setHint("0.00");

                                LinearLayout layout = new LinearLayout(getApplicationContext());
                                layout.setOrientation(LinearLayout.VERTICAL);
                                layout.addView(balanceView);
                                layout.addView(editText0);
                                layout.addView(phaseView);
                                layout.addView(editText1);

                                builder = new AlertDialog.Builder(this);
                                builder.setTitle("Power calibration");
                                builder.setView(layout);
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String a = editText0.getText().toString();
                                        String b = editText1.getText().toString();
                                        if (a.length() > 0 && b.length() > 0) {
                                            double balance = Double.parseDouble(editText0.getText().toString());
                                            double phase = Double.parseDouble(editText1.getText().toString());
                                            double data;
                                            for (int i = 0; i < mReceive.size(); i++) {
                                                switch (i) {
                                                    case 3:
                                                        balance += 100.0;
                                                        balance /= 100.0;
                                                        data = balance * Integer.parseInt(mReceive.get(i));
                                                        mParameter.append(String.format("12%04x", (long) data));
                                                        break;
                                                    case 6:
                                                        phase *= 100.0;
                                                        phase /= 4.0;
                                                        data = Integer.parseInt(mReceive.get(i)) - (long) phase;
                                                        mParameter.append(String.format("12%04x", (long) data));
                                                        break;
                                                    case 11:
                                                        mParameter.append(String.format("11%02x", Integer.parseInt(mReceive.get(i))));
                                                        break;
                                                    default:
                                                        mParameter.append(String.format("12%04x", Integer.parseInt(mReceive.get(i))));
                                                        break;
                                                }
                                            }
                                            mStep++;
                                        } else {
                                            mStep = 3;
                                        }
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mStep = 3;
                                    }
                                });
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        if (mStep == 1) {
                                            mStep = 3;
                                        }
                                    }
                                });
                                builder.show();
                                ret = 1;
                                mStep++;
                                break;
                            case 1:
                                ret = 1;
                                break;
                            case 2:
                                ret = 3;
                                break;
                            case 3:
                                break;
                        }
                        break;
                    default:
                        break;
                }
                break;

            case MSG_SET_VOLT:
                switch (mSubStage) {
                    case 1:
                        mParameter.setLength(0);
                        ret = 3;
                        break;
                    case 3:
                        switch (mStep) {
                            case 0:
                                mParameter.setLength(0);
                                mParameter.append("020c");
                                final TextView voltView = new TextView(this);
                                voltView.setText("Volt(%):");
                                final EditText editText0 = new EditText(this);
                                editText0.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                editText0.setHint("0.00");

                                final TextView amp1View = new TextView(this);
                                amp1View.setText("Current1(%):");
                                final EditText editText1 = new EditText(this);
                                editText1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                editText1.setHint("0.00");

                                final TextView amp2View = new TextView(this);
                                amp2View.setText("Current2(%):");
                                final EditText editText2 = new EditText(this);
                                editText2.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                editText2.setHint("0.00");

                                LinearLayout layout = new LinearLayout(getApplicationContext());
                                layout.setOrientation(LinearLayout.VERTICAL);
                                layout.addView(voltView);
                                layout.addView(editText0);
                                layout.addView(amp1View);
                                layout.addView(editText1);
                                layout.addView(amp2View);
                                layout.addView(editText2);

                                builder = new AlertDialog.Builder(this);
                                builder.setTitle("Volt and current calibration");
                                builder.setView(layout);
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String a = editText0.getText().toString();
                                        String b = editText1.getText().toString();
                                        String c = editText1.getText().toString();
                                        if (a.length() > 0 && b.length() > 0 && c.length() > 0) {
                                            double volt = Double.parseDouble(editText0.getText().toString());
                                            double amp1 = Double.parseDouble(editText1.getText().toString());
                                            double amp2 = Double.parseDouble(editText2.getText().toString());
                                            double data;
                                            for (int i = 0; i < mReceive.size(); i++) {
                                                switch (i) {
                                                    case 0:
                                                        mParameter.append(String.format("11%02x", Integer.parseInt(mReceive.get(i))));
                                                        break;
                                                    case 2:
                                                        volt += 100.0;
                                                        volt /= 100.0;
                                                        data = volt * Integer.parseInt(mReceive.get(i));
                                                        mParameter.append(String.format("12%04x", (long) data));
                                                        break;
                                                    case 6:
                                                        amp1 += 100.0;
                                                        amp1 /= 100.0;
                                                        data = amp1 * Integer.parseInt(mReceive.get(i));
                                                        mParameter.append(String.format("12%04x", (long) data));
                                                        break;
                                                    case 8:
                                                        amp2 += 100.0;
                                                        amp2 /= 100.0;
                                                        data = amp2 * Integer.parseInt(mReceive.get(i));
                                                        mParameter.append(String.format("12%04x", (long) data));
                                                        break;
                                                    default:
                                                        mParameter.append(String.format("12%04x", Integer.parseInt(mReceive.get(i))));
                                                        break;
                                                }
                                            }
                                            mStep++;
                                        } else {
                                            mStep = 3;
                                        }
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mStep = 3;
                                    }
                                });
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        if (mStep == 1) {
                                            mStep = 3;
                                        }
                                    }
                                });
                                builder.show();
                                ret = 1;
                                mStep++;
                                break;
                            case 1:
                                ret = 1;
                                break;
                            case 2:
                                ret = 3;
                                break;
                            case 3:
                                break;
                        }
                        break;
                    default:
                        break;
                }
                break;

            case MSG_SET_ELECTROSCOPE:
                mDataIndex = 11;
                switch (mSubStage) {
                    case 1:
                        mParameter.setLength(0);
                        ret = 3;
                        break;
                    case 3:
                        switch (mStep) {
                            case 0:
                                mParameter.setLength(0);
                                final TextView valView = new TextView(this);
                                valView.setText("Current threshold");
                                final EditText editText0 = new EditText(this);
                                editText0.setInputType(InputType.TYPE_CLASS_NUMBER);
                                editText0.setHint(mReceive.get(2));

                                LinearLayout layout = new LinearLayout(getApplicationContext());
                                layout.setOrientation(LinearLayout.VERTICAL);
                                layout.addView(valView);
                                layout.addView(editText0);

                                builder = new AlertDialog.Builder(this);
                                builder.setTitle("Potential");
                                builder.setView(layout);
                                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String a = editText0.getText().toString();
                                        if (a.length() > 0) {
                                            int val1 = parseInt(mReceive.get(1));
                                            int val2 = parseInt(a);
                                            int val3 = parseInt(mReceive.get(3));
                                            int val4 = parseInt(mReceive.get(4));
                                            mParameter.append(String.format("0101020412%04x12%04x11%02x11%02x", val1, val2, val3, val4));
                                            mStep++;
                                        } else {
                                            mStep = 3;
                                        }
                                    }
                                });
                                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        mStep = 3;
                                    }
                                });
                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialogInterface) {
                                        if (mStep == 1) {
                                            mStep = 3;
                                        }
                                    }
                                });
                                builder.show();
                                ret = 1;
                                mStep++;
                                break;
                            case 1:
                                ret = 1;
                                break;
                            case 2:
                                ret = 3;
                                break;
                            case 3:
                                break;
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
        if (ret != 1) {
            mStep = 0;
        }
        return ret;
    }

    private int Access(final int message_id) {
        int ret = 0;
        int cmd = 0;
        switch (message_id) {
            case MSG_CONNECT:/*接続のみ*/
                break;
            case MSG_GET_POTENTIAL:
                ret = accessData(0, DLMS.IST_VOLT3, 2, false);
                break;
            case MSG_GET_STATUS:
                ret = accessData(0, DLMS.IST_CHECK_MEASURE, 2, mInterval == 0);
                break;
            case MSG_GET_CONFIG:
                ret = accessData(0, DLMS.IST_SPECIFICATION, 2, mInterval == 0);
                break;
            case MSG_CHART_STATUS:
                switch (mSubStage) {
                    case 2:
                        ret = accessData(0, DLMS.IST_POWER_QUALITY, 7, mInterval == 0);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(0, DLMS.IST_POWER_QUALITY, 2, mInterval == 0);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 6:
                        ret = accessData(0, DLMS.IST_CHECK_MEASURE, 2, mInterval == 0);
                        break;
                    default:
                        break;
                }
                break;
            case MSG_CHART_RECORD:
                switch (mSubStage) {
                    case 2:
                        ret = accessData(0, DLMS.IST_CHECK_MEASURE, 2, mInterval == 0);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(0, DLMS.IST_POWER_QUALITY, 2, mInterval == 0);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 6:
                        ret = accessData(0, DLMS.IST_LOAD_PROFILE, 7, mInterval == 0);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 8:
                        ret = accessData(0, DLMS.IST_LOAD_PROFILE, 2, mInterval == 0);
                        break;
                    default:
                        break;
                }
                break;
            case MSG_GET_SOURCE:
                ret = accessData(0, DLMS.IST_SETUP_PULS, 2, mInterval == 0);
                break;
            case MSG_ACT_DEMAND_RESET:
                cmd = 2;
                ret = accessData(2, DLMS.IST_DEMAND_RESET, 1, mInterval == 0);
                break;
            case MSG_LOG_BILLING:
                ret = accessData(0, DLMS.IST_BILLING_PARAMS, 2, mInterval == 0);
                break;
            case MSG_LOG_EVENT:
                ret = accessData(0, DLMS.IST_POWER_QUALITY, 2, mInterval == 0);
            case MSG_LOG_LOAD:
                ret = accessData(0, DLMS.IST_LOAD_PROFILE, 2, mInterval == 0);
                break;
            case MSG_LOG_AMPERE:
                ret = accessData(0, DLMS.IST_AMPR_RECORD, 2, mInterval == 0);
                break;
            case MSG_LOG_METER:
                ret = accessData(0, DLMS.IST_METER_LOG, 2, mInterval == 0);
                break;
            case MSG_SET_CLOCK:
                cmd = 1;
                ret = accessData(1, DLMS.IST_DATETIME_NOW, 2, true);
                break;
            case MSG_SET_FILTER:
                cmd = 1;
                switch (mSubStage) {
                    case 2:
                        ret = accessData(1, DLMS.IST_ALARM_FIL1, 2, true);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(1, DLMS.IST_ALARM_FIL2, 2, true);
                        break;
                    default:
                        break;
                }
                break;
            case MSG_SET_PASSWORD:
                cmd = 1;
                ret = accessData(1, DLMS.IST_ASSO_LN3, 7, true);
                break;
            case MSG_RESET_EVENT:
                cmd = 2;
                ret = accessData(2, DLMS.IST_POWER_QUALITY, 1, true);
                break;
            case MSG_RESET_LOAD:
                cmd = 2;
                ret = accessData(2, DLMS.IST_LOAD_PROFILE, 1, true);
                break;
            case MSG_RESET_AMP:
                cmd = 2;
                ret = accessData(2, DLMS.IST_AMPR_RECORD, 1, true);
                break;
            case MSG_RESET_BILL:
                cmd = 2;
                ret = accessData(2, DLMS.IST_BILLING_PARAMS, 1, true);
                break;
            case MSG_SET_TYPE:
                cmd = 1;
                ret = accessData(1, DLMS.IST_TYPE, 2, true);
                break;
            case MSG_SET_POWER:
                cmd = 1;
                switch (mSubStage) {
                    case 2:
                        ret = accessData(0, DLMS.IST_CAL_ENERGY, 2, false);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(1, DLMS.IST_CAL_ENERGY, 2, true);
                        break;
                    default:
                        break;
                }
                break;
            case MSG_SET_VOLT:
                cmd = 1;
                switch (mSubStage) {
                    case 2:
                        ret = accessData(0, DLMS.IST_CAL_VOLTAMP, 2, false);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(1, DLMS.IST_CAL_VOLTAMP, 2, true);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    default:
                        break;
                }
                break;
            case MSG_GET_ENERGY:
                ret = accessData(0, DLMS.IST_CAL_ENERGY, 2, false);
                break;
            case MSG_GET_VOLTAMP:
                ret = accessData(0, DLMS.IST_CAL_VOLTAMP, 2, false);
                break;
            case MSG_SET_SOURCE:
                cmd = 1;
                ret = accessData(1, DLMS.IST_SETUP_PULS, 2, true);
                break;
            case MSG_SET_DEFAULT0:
                cmd = 1;
                ret = accessData(1, DLMS.IST_CAL_ENERGY, 2, true);
                break;
            case MSG_SET_DEFAULT1:
                cmd = 1;
                ret = accessData(1, DLMS.IST_CAL_VOLTAMP, 2, true);
                break;
            case MSG_RESET_ABS:
                cmd = 2;
                ret = accessData(2, DLMS.IST_ABS_ENERGY, 1, true);
                break;
            case MSG_MISS_NEU:
                ret = accessData(0, DLMS.IST_DETECT, 2, false);
                break;
            case MSG_MISS_NEU_ON:
            case MSG_MISS_NEU_OFF:
                cmd = 1;
                switch (mSubStage) {
                    case 2:
                        ret = accessData(1, DLMS.IST_DETECT, 2, false);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(0, DLMS.IST_DETECT, 2, false);
                        break;
                    default:
                        break;
                }
                break;
            case MSG_TAMPER_ON:
            case MSG_TAMPER_OFF:
                cmd = 1;
                switch (mSubStage) {
                    case 2:
                        ret = accessData(1, DLMS.IST_SETTING0, 2, false);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(0, DLMS.IST_SETTING0, 2, false);
                        break;
                    default:
                        break;
                }
                break;
            case MSG_SET_ELECTROSCOPE:
                cmd = 1;
                switch (mSubStage) {
                    case 2:
                        ret = accessData(0, DLMS.IST_DETECT, 2, false);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    case 4:
                        ret = accessData(1, DLMS.IST_DETECT, 2, false);
                        if (ret == 0) {
                            ret = 3;
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                mItemFragment.Progress("Not implemented function...", 0, 0);
                break;
        }
        if (ret == 0) {
            if (mInterval == 0) {
                switch (cmd) {
                    case 1:
                        soundPool.play(sound[4], 1.0f, 1.0f, 0, 0, 1);
                        break;
                    case 2:
                        soundPool.play(sound[5], 1.0f, 1.0f, 0, 0, 1);
                        break;
                    default:
                        soundPool.play(sound[3], 1.0f, 1.0f, 0, 0, 1);
                        break;
                }
                showToast("Finish");
            }
        }
        return ret;
    }

    @Override
    public void Parameter1(final String argument) {
        if (argument == null) {
            CounterParameter1.setLength(0);
        } else {
            CounterParameter1.append(argument);
        }
    }

    @Override
    public void Parameter2(final String argument) {
        if (argument == null) {
            CounterParameter2.setLength(0);
        } else {
            CounterParameter2.append(argument);
        }
    }

    @Override
    public void Multiple(final byte[] list) {
        if (list.length == 0) {
            mMultiple = false;
        } else {
            mMultiple = true;
            for (int i = 0; i < d.Count(); i++) {
                d.Status(1, i);
            }
        }
        mMultiple = false;
        mSelect = 0;
    }

    @Override
    public int setInterval(final boolean enable) {
        if (enable) {
            mInterval = parseInt(MainActivity.d.readInterval());
        } else {
            mInterval = 0;
        }
        return mInterval;
    }

    @Override
    public int messageID() {
        return mCurrentMessage;
    }

    @Override
    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void Sound(final int id) {
        soundPool.play(sound[id], 1.0f, 1.0f, 0, 0, 1);
    }

    @Override
    public int fragmentMessage(final int message) {
        int ret = 0;

        Log.i(TAG, String.format("Current:%d, In: %d", mCurrentMessage, message));

        mCurrentMessage = message;
        if (message <= 0) {
            if (message == 0) {
                mCurrentMessage = -1;
            } else {
                return ret;
            }
        }
        if (mTimer > timeout) {
            mTimer = 0;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Communication timeout detected !");
            builder.setMessage("No data received from meter.\nAbort connection. please try again.");
            builder.setPositiveButton("OK", null);
            builder.show();
            ret = -1;
        } else {
            switch (mStage) {
                case 0:
                    if (mMultiple) {
                        ret = Connection2();
                    } else {
                        d.clearViewData();
                        ret = Connection1();
                    }
                    if (ret == 2) {
                        mStage++;
                    }
                    break;
                case 1:
                    ret = sessionEstablish();
                    if (ret == 2) {
                        mStage++;
                        mSubStage = 0;
                    }
                    break;
                default:
                    switch (mSubStage) {
                        case 0:
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            mCount = 0;
                            mTotal = 0;
                            mDataIndex = 0;
                            mSel = 0;
                            mSubStage++;
                            ret = 1;
                            break;
                        case 1:
                        case 3:
                        case 5:
                        case 7:
                            ret = Parameter(message);
                            if (ret == 3) {
                                ret = 1;
                                mSubStage++;
                            }
                            break;
                        default:    //mSubStage= 2 or 4
                            ret = Access(message);
                            if (ret == 3) {
                                mSubStage++;
                            } else {
                                if (ret <= 0) {
                                    invalidateOptionsMenu();
                                    if (ret < 0) {
                                        mItemFragment.Progress("Detect error...", 0, 0);
                                        showToast("Detect error...");
                                        if (mInterval > 0) {
                                            if (mMultiple) {
                                                mSelect %= d.Count();
                                            } else {
                                                ret = 5;
                                            }
                                        }
                                    } else {
                                        if (mInterval > 0) {
                                            if (mMultiple) {
                                                Disconnect(true);
                                                mSelect++;
                                                mSelect %= d.Count();
                                                ret = 14;
                                            } else {
                                                ret = 4;
                                            }
                                        } else {
                                            /*0*/
                                            if (mMultiple) {
                                                Disconnect(true);
                                                mSelect++;
                                                mSelect %= d.Count();
                                                ret += 10;
                                            } else {

                                            }
                                        }
                                    }
                                    mSubStage = 0;
                                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                }
                            }
                            break;
                    }
                    break;
            }
        }
        if (ret < 0) {
            Disconnect(true);
            scanLeDevice();
        }
        return ret;
    }
}