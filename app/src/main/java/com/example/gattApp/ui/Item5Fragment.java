package com.example.gattApp.ui;

import static java.lang.Integer.parseInt;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.gattApp.MainActivity;
import com.example.gattApp.R;

import java.util.ArrayList;

public class Item5Fragment extends ItemFragment {

    private final static String TAG = Item5Fragment.class.getSimpleName();
    private Button mButton1;

    private messageManager mCallback;
    private EditText mAcount;
    private TextView textCount;
    private TextView textCount2;
    private EditText mPassword;
    private EditText mAddress;
    private EditText mRank;
    private EditText mScan;
    private EditText mTick;
    private EditText mInterval;
    private int mCurrent;

    private ArrayList<String> mTemp = new ArrayList<String>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach.");
        Activity a = getActivity();
        if (a instanceof Item5Fragment.messageManager == false) {
            throw new ClassCastException("Activity have to implement Item5Fragment.messageManager");
        }
        mCallback = (Item5Fragment.messageManager) a;
    }

    private void setAnime() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.1f, 1.0f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(100);
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        mButton1.startAnimation(animationSet);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView.");
        View rootView = inflater.inflate(R.layout.fragment_item5, container, false);

        Spinner spinner = rootView.findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this.getContext(),
                R.layout.spinner_device,
                MainActivity.d.getMeterList().split(","));
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        // spinner に adapter をセット
        spinner.setAdapter(adapter);

        // リスナーを登録
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            //　アイテムが選択された時
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view, int position, long id) {
                Spinner spinner = (Spinner) parent;
                mCurrent = position;
                mAcount.setText(MainActivity.d.Account(mCurrent));
                mPassword.setText(MainActivity.d.Password(mCurrent));
                mAddress.setText(MainActivity.d.getAddress(mCurrent));
                mRank.setText(MainActivity.d.getRank(mCurrent));
            }

            //　アイテムが選択されなかった
            public void onNothingSelected(AdapterView<?> parent) {
                //
            }
        });
        mButton1 = rootView.findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String name = String.format("%-8s", mAcount.getText().toString());
                MainActivity.d.Account(name, mCurrent);
                MainActivity.d.Password(mPassword.getText().toString(), mCurrent);
                MainActivity.d.writeAddress(mAddress.getText().toString(), mCurrent);
                MainActivity.d.writeRank(mRank.getText().toString(), mCurrent);
                MainActivity.d.updateAllMeterInformation();

                int tick = parseInt(mTick.getText().toString());
                MainActivity.mTick = tick;
                MainActivity.d.writeTick(mTick.getText().toString());

                int scan = parseInt(mScan.getText().toString());
                MainActivity.mScan = scan;
                MainActivity.d.writeScan(mScan.getText().toString());

                int interval = parseInt(mInterval.getText().toString());
                MainActivity.mInterval = interval;
                MainActivity.d.writeInterval(mInterval.getText().toString());
                mCallback.showToast("Updated setting");
                setAnime();
            }
        });
        mAcount = rootView.findViewById(R.id.editText1);
        textCount = rootView.findViewById(R.id.textCount);
        mPassword = rootView.findViewById(R.id.editText2);
        textCount2 = rootView.findViewById(R.id.textCount2);

        mAddress = rootView.findViewById(R.id.editText3);
        mRank = rootView.findViewById(R.id.editText4);
        mScan = rootView.findViewById(R.id.editText5);
        mTick = rootView.findViewById(R.id.editText6);
        mInterval = rootView.findViewById(R.id.editText7);

        mAcount.setText(MainActivity.d.Account(-1));
        mPassword.setText(MainActivity.d.Password(-1));
        mAddress.setText(MainActivity.d.getAddress(-1));
        mRank.setText(MainActivity.d.getRank(-1));
        mScan.setText(MainActivity.d.readScan());
        mTick.setText(MainActivity.d.readTick());
        mInterval.setText(MainActivity.d.readInterval());

        textCount.setText(String.format("%d", mAcount.length()) + "/8 max");
        mAcount.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textCount.setText(String.format("%d", mAcount.length()) + "/8 max");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });

        textCount2.setText(String.format("%2.1f", mPassword.length() / 2.0) + "/16.0 max");
        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textCount2.setText(String.format("%2.1f", mPassword.length() / 2.0) + "/16.0 max");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //       MainActivity.d.changeCurrent(mCurrent);
        Log.i(TAG, "onDestroyView.");
    }

    @Override
    public void DeviceInfo(String msg) {
        super.DeviceInfo(msg);
    }

    @Override
    public void Progress(final String msg, final int now, final int end) {
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public void DataArrived(final ArrayList<String> in, final boolean last) {
        super.DataArrived(in, last);
    }
    public void SetData(final ArrayList<String> in) {
    }
}