package com.example.gattApp.ui;

import static java.lang.Integer.parseInt;

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

public class CustomizeFragment extends ItemFragment {

    private final static String TAG = CustomizeFragment.class.getSimpleName();
    private static int mCnt = 0;
    private Button mButton1;
    private Button mButton2;
    private Button mButton3;
    private Button mButton4;
    private Button mButton5;
    private Button mButton6;
    private Button mButton7;
    private Button mButton8;
    private TextView mOutView;
    private TextView mDeviceView;
    private int mSelectButton;
    private messageManager mCallback;
    private ScrollView mScrollview;
    private ArrayList<String> mTemp = new ArrayList<String>();
    private byte stage;
    private long cur1;
    private int record1;
    private long cur2;
    private int record2;
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
                        if (ret > 0) {
                            if (ret == 4) {
                                if (stopper) {
                                    ret = -2;
                                } else {
                                    mButton7.setEnabled(true);
                                    handler.postDelayed(this, MainActivity.mInterval);
                                }
                            } else {
                                if (ret == 5) {
                                    mCnt = 0;
                                    stage = 0;
                                }
                                handler.postDelayed(this, MainActivity.mTick);
                            }
                        }
                        if (ret <= 0) {
                            mCallback.fragmentMessage(-1);
                            mSelectButton = -1;
                            mButton7.setEnabled(true);
                            stopper = true;
                            mCallback.setInterval(false);
                        }
                    }
                };
                mButton7.setEnabled(false);
                mCallback.setInterval(true);
                MainActivity.CounterParameter1.setLength(0);
                MainActivity.CounterParameter2.setLength(0);
                mCnt = 0;
                stage = 0;
                mSelectButton = msg;
                stopper = false;
                handler.post(r);
            } else {
                mCallback.showToast("Previous task is running. Please wait.");
            }
        } else {
            stopper = true;
            mButton7.setEnabled(false);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach.");
        Activity a = getActivity();
        if (a instanceof CustomizeFragment.messageManager == false) {
            throw new ClassCastException("Activity have to implement CustomizeFragment.messageManager");
        }
        mCallback = (CustomizeFragment.messageManager) a;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView.");
        View rootView = inflater.inflate(R.layout.fragment_customize, container, false);

        mCnt = 0;
        mSelectButton = -1;
        stopper = true;

        mDeviceView = rootView.findViewById(R.id.deviceView);
        mOutView = rootView.findViewById(R.id.itemView);

        mButton1 = rootView.findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_SET_TYPE);
                setAnime(mButton1);
            }
        });

        mButton2 = rootView.findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_RESET_ABS);
                setAnime(mButton2);
            }
        });

        mButton3 = rootView.findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_MISS_NEU_ON);
                setAnime(mButton3);
            }
        });
        mButton4 = rootView.findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_MISS_NEU_OFF);
                setAnime(mButton4);
            }
        });

        mButton5 = rootView.findViewById(R.id.button5);
        mButton5.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_TAMPER_ON);
                setAnime(mButton5);
            }
        });
        mButton6 = rootView.findViewById(R.id.button6);
        mButton6.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_TAMPER_OFF);
                setAnime(mButton6);
            }
        });
        mButton7 = rootView.findViewById(R.id.button7);
        mButton7.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_GET_POTENTIAL);
                setAnime(mButton7);
            }
        });
        mButton8 = rootView.findViewById(R.id.button8);
        mButton8.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonFunction(MainActivity.MSG_GET_ENERGY);
                setAnime(mButton8);
            }
        });
        int rank = Integer.parseInt(MainActivity.d.getRank(-1));
        mButton1.setVisibility(View.INVISIBLE);
        mButton2.setVisibility(View.INVISIBLE);
        mButton3.setVisibility(View.INVISIBLE);
        mButton4.setVisibility(View.INVISIBLE);
        mButton5.setVisibility(View.INVISIBLE);
        mButton6.setVisibility(View.INVISIBLE);
//        mButton7.setVisibility(View.INVISIBLE);
        mButton8.setVisibility(View.INVISIBLE);

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
        if (MainActivity.mInterval == 0) {
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

    @Override
    public String getData() {
        return mOutView.getText().toString();
    }

    @Override
    public void DataArrived(final ArrayList<String> in, final boolean last) {
        if (stopper) {
            return;
        }
        super.DataArrived(in, last);
        if (MainActivity.mInterval == 0) {
            mOutView.setText("");
            for (int i = 0; i < in.size(); i++) {
                mOutView.append(in.get(i) + "\n");
            }
            mScrollview.fullScroll(View.FOCUS_DOWN);
       } else {
            String out = String.format("%s, %s\n", in.get(0), in.get(1));
            mOutView.append(out);
            MainActivity.d.addViewData(out);
            mScrollview.fullScroll(View.FOCUS_DOWN);
        }
    }
    public void SetData(final ArrayList<String> in) {
        if (in.size() > 0) {
            mOutView.setText("");
            for (int i = 0; i < in.size(); i++) {
                mOutView.append(in.get(i).toString());
            }
            mScrollview.fullScroll(View.FOCUS_DOWN);
        }
    }
}