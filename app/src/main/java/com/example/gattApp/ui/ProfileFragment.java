package com.example.gattApp.ui;

import static java.lang.Integer.parseInt;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import androidx.annotation.NonNull;

import com.example.gattApp.MainActivity;
import com.example.gattApp.R;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;


import java.util.ArrayList;

public class ProfileFragment extends ItemFragment {

    private TextView mTextView1;
    private TextView mTextView2;
    private TextView mTextView4;
    private final static String TAG = ProfileFragment.class.getSimpleName();
    private static int mCnt = 0;
    private float yMax1, yMax2;
    private Button mButton1;
    private int mSelectButton;
    private messageManager mCallback;
    private ScrollView mScrollview;
    protected LineChart chart1;
    protected LineChart chart2;
    private ArrayList<String> timestamp = new ArrayList<String>();
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
                                    mButton1.setText("STOP");
                                    mButton1.setEnabled(true);
                                    handler.postDelayed(this, MainActivity.mInterval);
                                }
                            } else {
                                if(ret==5){
                                    mCnt = 0;
                                    stage = 0;
                                }
                                handler.postDelayed(this, MainActivity.mTick);
                            }
                        }
                        if (ret <= 0) {
                            mCallback.fragmentMessage(-1);
                            mSelectButton = -1;
                            mButton1.setText("RUN");
                            mButton1.setEnabled(true);
                            stopper = true;
                            mCallback.setInterval(false);
                        }
                    }
                };
                mButton1.setText("STARTING");
                mButton1.setEnabled(false);
                mTextView4.setText("");
                mCallback.setInterval(true);
                MainActivity.CounterParameter1.setLength(0);
                MainActivity.CounterParameter2.setLength(0);
                mCnt = 0;
                stage = 0;
                mSelectButton = msg;
                stopper = false;
                handler.post(r);
            } else {
                mCallback.showToast("Before task is running. Please wait.");
            }
        } else {
            stopper = true;
            mButton1.setText("ENDING");
            mButton1.setEnabled(false);
        }
    }

    private void setChartConfig(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(true);
//        chart.setBackgroundColor(Color.CYAN);
        chart.getLegend().setForm(Legend.LegendForm.LINE);
//        chart.getLegend().setTypeface(MainActivity.tfLight);
        chart.getLegend().setTextColor(Color.WHITE);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
//        chart.getXAxis().setTypeface(MainActivity.tfLight);
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getXAxis().setLabelRotationAngle(18);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setAvoidFirstLastClipping(true);
        chart.getXAxis().setEnabled(true);
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return timestamp.get((int) value);
            }
        });
//        chart.getAxisLeft().setTypeface(MainActivity.tfLight);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
    }

    private void setupChart() {

        record1 = 0;
        record2 = 0;
        cur1 = 0;
        cur2 = 0;
        chart1.clear();
        setChartConfig(chart1);
        LineData data1 = new LineData();
        data1.setValueTextColor(Color.WHITE);
        chart1.setData(data1);
        yMax1 = 1f;
        chart1.getAxisLeft().setAxisMaximum(yMax1);
        chart1.getAxisLeft().setAxisMinimum(0f);
        chart1.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%4.1f kW", value);
            }
        });

        chart2.clear();
        setChartConfig(chart2);
        LineData data2 = new LineData();
        data2.setValueTextColor(Color.WHITE);
        chart2.setData(data2);
        yMax2 = 230f;
        chart2.getAxisLeft().setAxisMaximum(yMax2);
        chart2.getAxisLeft().setAxisMinimum(0f);
        chart2.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%4.1f V", value);
            }
        });
        timestamp.clear();
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.WHITE);
        set.setCircleColor(Color.YELLOW);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(Color.YELLOW);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach.");
        Activity a = getActivity();
        if (a instanceof ProfileFragment.messageManager == false) {
            throw new ClassCastException("Activity have to implement ProfileFragment.messageManager");
        }
        mCallback = (ProfileFragment.messageManager) a;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView.");

        mSelectButton = -1;
        mCnt = 0;
        stopper = true;

        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);
        mTextView1 = rootView.findViewById(R.id.textView1);
        mTextView2 = rootView.findViewById(R.id.textView2);
        mTextView4 = rootView.findViewById(R.id.textView4);

        mTextView1.setText("");
//        mTextView1.setBackgroundColor(Color.CYAN);
        mTextView2.setText("");
//        mTextView2.setBackgroundColor(Color.CYAN);
        mTextView4.setText("");
//        mTextView4.setBackgroundColor(Color.CYAN);

        mButton1 = rootView.findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                buttonFunction(MainActivity.MSG_GET_POTENTIAL);
                buttonFunction(MainActivity.MSG_CHART_RECORD);
                setAnime(mButton1);
            }
        });
        mCallback.fragment(this);

        chart1 = rootView.findViewById(R.id.lineChart1);
        chart2 = rootView.findViewById(R.id.lineChart2);
        setupChart();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView.");
        stopper = true;
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
        String ret = "";
        return ret;
    }

    private float changeMax(final float now, final float max, final float step) {
        float new_max = max;

        double val = new_max - (now + step * 1.2f);
        if (val > (step * 1.5f)) {
            int times = (int) (val / step);
            new_max -= times * step;
        } else {
            if (val < 0f) {
                val = 0f - val;
                int times = (int) (val / step) + 1;
                new_max += times * step;
            }
        }
        return new_max;
    }

    private float changeMin(final float now, final float min, final float step) {
        float new_min = min;

        double val = (now - step * 1.2f) - new_min;
        if (val > (step * 1.5f)) {
            int times = (int) (val / step);
            new_min += times * step;
        } else {
            if (val < 0f) {
                val = 0f - val;
                int times = (int) (val / step) + 1;
                new_min -= times * step;
            }
        }
        return new_min;
    }

    @Override
    public void DataArrived(final ArrayList<String> in, final boolean last) {
        if (stopper) {
            return;
        }
        super.DataArrived(in, last);
        mTemp.addAll(in);
        if (last) {
            if (mTemp.size() > 0) {
                mCnt++;
                if (stage == 0) {
                    if (mTemp.size() > 3) {
                        mTextView1.setText(mTemp.get(1) + " ");
                        mTextView2.setText(String.format("%.3f kWh ", Integer.parseInt(mTemp.get(3)) / 1000f));
                    }
                }
                if (stage == 1) {
                    int size = mTemp.size();
                    size -= 1;
                    if ((size%3)==0) {
                        size /= 3;
                        if (record1 == 0) {
                            record1 = size;
                            MainActivity.CounterParameter1.setLength(0);
                            MainActivity.CounterParameter1.append(String.format("020406%08x0600000000120001120000", size + 1));
                            mTextView4.setText("");
                        } else {
                            if (size > 0) {
                                record1 += size;
                                size = record1;
                                MainActivity.CounterParameter1.setLength(0);
                                MainActivity.CounterParameter1.append(String.format("020406%08x0600000000120001120000", size + 1));
                            }
                        }
                    }
                    if (mTemp.size() > 1) {
                        CharSequence backup = mTextView4.getText();
                        mTextView4.setText("");
                        int i;
                        i = mTemp.size() - 1;
                        for (; i > 1; ) {
                            mTextView4.append(mTemp.get(i - 2) + " ");
                            int code = Integer.parseInt(mTemp.get(i - 1).toString());
                            mTextView4.append(MainActivity.d.PQCODE[code]);
                            i -= 3;
                            mTextView4.append("\n");
                        }
                        mTextView4.append(backup);
                    }
                }
                if (stage == 2) {
                    int size = Integer.parseInt(mTemp.get(1));
                    if (record2 == 0) {
                        record2 = size;
                        if (size > 100) {
                            size -= 100;
                        } else {
                            size = 1;
                        }
                        MainActivity.CounterParameter2.setLength(0);
                        MainActivity.CounterParameter2.append(String.format("020406%08x0600000000120001120000", size));
                    } else {
                        record2 = size;
                        MainActivity.CounterParameter2.setLength(0);
                        MainActivity.CounterParameter2.append(String.format("020406%08x0600000000120001120000", record2));
                    }
                }
                if (stage == 3) {
                    if (mTemp.size() > 1) {
                        boolean go = false;
                        long jigen;
                        if (cur2 == 0) {
                            jigen = MainActivity.d.DatetimeToSec(mTemp.get(mTemp.size() - 5));
                            cur2 = jigen;
                            go = true;
                        } else {
                            jigen = MainActivity.d.DatetimeToSec(mTemp.get(1));
                            if (cur2 != jigen) {
                                cur2 = jigen;
                                go = true;
                            }
                        }
                        if (go) {
                            LineData data1 = chart1.getData();
                            LineData data2 = chart2.getData();
                            if (data1 != null && data2 != null) {
                                LineDataSet set1 = (LineDataSet) data1.getDataSetByIndex(0);
                                if (set1 == null) {
                                    set1 = createSet();
                                    data1.addDataSet(set1);
                                }
                                LineDataSet set2 = (LineDataSet) data2.getDataSetByIndex(0);
                                if (set2 == null) {
                                    set2 = createSet();
                                    data2.addDataSet(set2);
                                }
                                int i;
                                long now = 0;
                                for (i = 1; i < mTemp.size(); ) {
                                    boolean add = false;
                                    long sec = MainActivity.d.DatetimeToSec(mTemp.get(i));
                                    if (now == 0) {
                                        add = true;
                                    } else {
                                        now += 900;
                                        while (sec > now) {
                                            timestamp.add(MainActivity.d.SecToDatetime(now).substring(11));
                                            data1.addEntry(new Entry(set1.getEntryCount(), 0f), 0);
                                            data2.addEntry(new Entry(set2.getEntryCount(), 0f), 0);
                                            now += 900;
                                        }
                                        add = true;
                                    }
                                    if (add) {
                                        now = sec;
                                        timestamp.add(mTemp.get(i++).substring(11));
                                        i++;
                                        float vol = Integer.parseInt(mTemp.get(i++).toString()) / 100f;
                                        float imp = Integer.parseInt(mTemp.get(i++).toString()) / 1000f;
                                        float exp = Integer.parseInt(mTemp.get(i++).toString()) / 1000f;

                                        float res;
                                        res = changeMax(imp, yMax1, 0.3f);
                                        if (res != yMax1) {
                                            yMax1 = res;
                                        }
                                        res = changeMax(vol, yMax2, 5f);
                                        if (res != yMax2) {
                                            yMax2 = res;
                                        }
                                        data1.addEntry(new Entry(set1.getEntryCount(), imp), 0);
                                        data2.addEntry(new Entry(set2.getEntryCount(), vol), 0);
                                    }
                                }
                                chart1.getAxisLeft().setAxisMaximum(yMax1);
                                chart2.getAxisLeft().setAxisMaximum(yMax2);

                                data1.notifyDataChanged();
                                chart1.notifyDataSetChanged();
                                chart1.setVisibleXRangeMaximum(100);
                                chart1.moveViewToX(data1.getEntryCount());
                                data2.notifyDataChanged();
                                chart2.notifyDataSetChanged();
                                chart2.setVisibleXRangeMaximum(100);
                                chart2.moveViewToX(data2.getEntryCount());
                            }
                        }
                    }
                }
                if (stage < 3)
                    stage++;
                else
                    stage = 0;
            } else {
            }
            mTemp.clear();
//          mScrollview.fullScroll(View.FOCUS_DOWN);
        }
    }
    public void SetData(final ArrayList<String> in) {
    }

}