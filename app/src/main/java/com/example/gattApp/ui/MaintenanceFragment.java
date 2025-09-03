package com.example.gattApp.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.gattApp.MainActivity;
import com.example.gattApp.R;


import java.util.ArrayList;

public class MaintenanceFragment extends ItemFragment {

    private final static String TAG = MaintenanceFragment.class.getSimpleName();
    private static int mCnt = 0;
    private Button mButton1;
    private Button mButton2;
    private Button mButton3;
    private Button mButton4;
    private Button mButton5;
    private Button mButton6;
    private Button mButton7;
    private TextView mOutView;
    private TextView mDeviceView;
    private int mSelectButton;
    private messageManager mCallback;
    private ArrayList<String> mTemp = new ArrayList<String>();
    private boolean stopper;

    private void buttonFunction(final int msg) {
        if (mSelectButton < 0) {
            if (mCallback.messageID() < 0) {
                final Handler handler = new Handler();
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        int ret = 0;
                        ret = mCallback.fragmentMessage(mSelectButton);
                        if (stopper) {
                            ret = -2;
                        }
                        if (ret > 0) {
                            handler.postDelayed(this, MainActivity.mTick);
                        } else {
                            stopper = true;
                            mSelectButton = -1;
                            mCallback.fragmentMessage(-1);
                        }
                    }
                };
                byte[] dat = {0};
                mCallback.Multiple(dat);
                mCallback.setInterval(false);
                stopper = false;
                mSelectButton = msg;
                handler.post(r);
            } else {
                mCallback.showToast("Before task is running. Please wait.");
            }
        } else {
            stopper = true;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach.");
        Activity a = getActivity();
        if (a instanceof MaintenanceFragment.messageManager == false) {
            throw new ClassCastException("Activity have to implement MaintenanceFragment.messageManager");
        }
        mCallback = (MaintenanceFragment.messageManager) a;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView.");
        View rootView = inflater.inflate(R.layout.fragment_maintenence, container, false);

        mCnt = 0;
        mSelectButton = -1;
        stopper = true;

        mDeviceView = rootView.findViewById(R.id.deviceView);
        mOutView = rootView.findViewById(R.id.itemView);

        mButton1 = rootView.findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_SET_CLOCK);
                setAnime(mButton1);
            }
        });

        mButton2 = rootView.findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_RESET_BILL);
                setAnime(mButton2);
            }
        });

        mButton3 = rootView.findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_RESET_EVENT);
                setAnime(mButton3);
            }
        });

        mButton4 = rootView.findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_RESET_LOAD);
                setAnime(mButton4);
            }
        });
        mButton5 = rootView.findViewById(R.id.button5);
        mButton5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_RESET_AMP);
                setAnime(mButton5);
            }
        });

        mCallback.fragment(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopper = true;
        Log.i(TAG, "onDestroyView.");
    }

    @Override
    public void DeviceInfo(String msg) {
        super.DeviceInfo(msg);
        mDeviceView.setText(msg);
    }

    @Override
    public void Progress(final String msg, final int now, final int end) {
        if(stopper) {
            return;
        }
        if (end == 0) {
            if (now == 0) {
                mOutView.setText(msg);
            } else {
                mOutView.setText(String.format("%s %d ", msg, now));
            }
        } else {
            mOutView.setText(String.format("%s : %d / %d", msg, now, end));
        }
    }

    @Override
    public String getData() {
        return mOutView.getText().toString();
    }

    @Override
    public void DataArrived(final ArrayList<String> in, final boolean last) {
        if(stopper) {
            return;
        }
        super.DataArrived(in, last);
        mOutView.setText("");
        for (int i = 0; i < in.size(); i++) {
            mOutView.append(in.get(i) + "\n");
        }
    }
    public void SetData(final ArrayList<String> in) {
        if (in.size() > 0) {
            mOutView.setText("");
            for (int i = 0; i < in.size(); i++) {
                mOutView.append(in.get(i).toString());
            }
        }
    }
}