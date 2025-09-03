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
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.gattApp.MainActivity;
import com.example.gattApp.R;


import java.util.ArrayList;

public class Item2Fragment extends ItemFragment {

    private final static String TAG = Item2Fragment.class.getSimpleName();
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
    private ScrollView  mScrollview;
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
        if (a instanceof Item2Fragment.messageManager == false) {
            throw new ClassCastException("Activity have to implement Item2Fragment.messageManager");
        }
        mCallback = (Item2Fragment.messageManager) a;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView.");

        mCnt = 0;
        mSelectButton = -1;
        stopper = true;

        View rootView = inflater.inflate(R.layout.fragment_item2, container, false);

        mDeviceView = rootView.findViewById(R.id.deviceView);
        mOutView = rootView.findViewById(R.id.itemView);

        mButton1 = rootView.findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                buttonFunction(MainActivity.MSG_GET_POTENTIAL);
                buttonFunction(MainActivity.MSG_GET_STATUS);
                setAnime(mButton1);
            }
        });

        mButton2 = rootView.findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_GET_CONFIG);
                setAnime(mButton2);
            }
        });

        mButton3 = rootView.findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_ACT_DEMAND_RESET);
                setAnime(mButton3);
            }
        });

        mButton4 = rootView.findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_LOG_BILLING);
                setAnime(mButton4);
            }
        });

        mButton5 = rootView.findViewById(R.id.button5);
        mButton5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_LOG_EVENT);
                setAnime(mButton5);
            }
        });

        mButton6 = rootView.findViewById(R.id.button6);
        mButton6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_LOG_LOAD);
                setAnime(mButton6);
            }
        });

        mButton7 = rootView.findViewById(R.id.button7);
        mButton7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_LOG_AMPERE);
                setAnime(mButton7);
            }
        });

        mScrollview = rootView.findViewById(R.id.scrollView);
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
        if (mCnt == 0) {
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
    }

    private void logFormat(final int columns) {

        for (int i = 0; i < mTemp.size(); i++) {
            if ((i % columns) > 0)
                mOutView.append(", ");
            else {
                if (i != 0) mOutView.append("\n");
            }
            mOutView.append(mTemp.get(i));
        }
        mTemp.clear();
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
        mTemp.addAll(in);
        if (last) {
            int i = 0;
            if (mTemp.size() > 0) {
                mOutView.setText(mTemp.get(i++));
                switch (mSelectButton) {
                    case MainActivity.MSG_GET_POTENTIAL:
                        mOutView.append(mTemp.get(i++));
                        break;
                    default:
                    case MainActivity.MSG_ACT_DEMAND_RESET:
                    case MainActivity.MSG_GET_STATUS:
                    case MainActivity.MSG_GET_CONFIG:
                        for (; i < mTemp.size(); ) {
                            mOutView.append("\n");
                            mOutView.append(mTemp.get(i++));
                        }
                        break;
                    case MainActivity.MSG_LOG_BILLING:
                        mOutView.append("\n");
                        for (int j = 1; i < mTemp.size(); j++) {
                            mOutView.append(String.format("---- Entry %d ----", j));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++) + "\n");
                        }
                        break;

                    case MainActivity.MSG_LOG_EVENT:
                        mOutView.append("\n");
                        for (int j = 1; i < mTemp.size(); j++) {
                            mOutView.append(String.format("---- Entry %d ----", j));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++) + "\n");
                        }
                        break;

                    case MainActivity.MSG_LOG_LOAD:
                        mOutView.append("\n");
                        for (int j = 1; i < mTemp.size(); j++) {
                            mOutView.append(String.format("---- Entry %d ----", j));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++) + "\n");
                        }
                        break;

                    case MainActivity.MSG_LOG_METER:
                        mOutView.append("\n");
                        for (; i < mTemp.size(); ) {
                            mOutView.append(mTemp.get(i++) + "\n");
                        }
                        break;

                    case MainActivity.MSG_LOG_AMPERE:
                        mOutView.append("\n");
                        for (int j = 1; i < mTemp.size(); j++) {
                            mOutView.append(String.format("---- Entry %d ----", j));
                            mOutView.append("\n" + mTemp.get(i++));
                            mOutView.append("\n" + mTemp.get(i++) + "\n");
                        }
                        break;
                }
                mTemp.clear();
            } else {
                mOutView.setText("No data ...");
            }
            mScrollview.fullScroll(View.FOCUS_DOWN);
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