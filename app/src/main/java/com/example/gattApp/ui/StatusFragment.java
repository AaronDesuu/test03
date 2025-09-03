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


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class StatusFragment extends ItemFragment {

    private TextView mTextView1;
    private TextView mTextView2;
    private TextView mTextView3;
    private TextView mTextView4;
    private final static String TAG = StatusFragment.class.getSimpleName();
    private static int mCnt = 0;
    private static int mMaxEntry;
    private static int CapMax1, CapMax2, CapMax21, CapMax3, CapMin2;
    private float yMax1, yMax2, yMax21, yMax3, yMin2;
    private Button mButton1;
    private int mSelectButton;
    private messageManager mCallback;
    protected LineChart chart1;
    protected LineChart chart2;
    protected LineChart chart3;
    private ArrayList<Long> timestamp = new ArrayList<Long>();
    private ArrayList<String> mTemp = new ArrayList<String>();
    private int stage;
    private int records;
    private long cur;
    private Long mInterval;
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
                byte [] dat={0,1,1,1};
                mCallback.Multiple(dat);
                mButton1.setText("STARTING");
                mButton1.setEnabled(false);
                mTextView4.setText("");
                mCallback.Parameter1(null);
                mCallback.Parameter2(null);
                mInterval = Long.valueOf(mCallback.setInterval(true) + 2000);
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
                final DateFormat df = new SimpleDateFormat("HH:mm:ss");
                final Date date = new Date(timestamp.get((int) value));
                return df.format(date);
            }
        });
//        chart.getAxisLeft().setTypeface(MainActivity.tfLight);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
    }

    private void setupChart() {

        mCallback.Parameter1(null);
        records = 0;
        cur = 0;
        timestamp.clear();
        chart1.clear();
        setChartConfig(chart1);
        LineData data1 = new LineData();
        data1.setValueTextColor(Color.WHITE);
        chart1.setData(data1);
        yMax1 = 2f;
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
        yMin2 = 220f;
        yMax2 = 240f;
        chart2.getAxisLeft().setAxisMaximum(yMax2);
        chart2.getAxisLeft().setAxisMinimum(yMin2);
        chart2.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%4.1f V", value);
            }
        });

        chart3.clear();
        setChartConfig(chart3);
        LineData data3 = new LineData();
        data3.setValueTextColor(Color.WHITE);
        chart3.setData(data3);
        yMax3 = 1f;
        chart3.getAxisLeft().setAxisMaximum(yMax3);
        chart3.getAxisLeft().setAxisMinimum(0f);
        chart3.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%4.1f A", value);
            }
        });
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
        if (a instanceof StatusFragment.messageManager == false) {
            throw new ClassCastException("Activity have to implement StatusFragment.messageManager");
        }
        mCallback = (StatusFragment.messageManager) a;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView.");

        mSelectButton = -1;
        stopper = true;
        mCnt = 0;
        mMaxEntry = 60;

        View rootView = inflater.inflate(R.layout.fragment_status, container, false);
        mTextView1 = rootView.findViewById(R.id.textView1);
        mTextView2 = rootView.findViewById(R.id.textView2);
        mTextView3 = rootView.findViewById(R.id.textView3);
        mTextView4 = rootView.findViewById(R.id.textView4);

        mTextView1.setText(String.format("%.3f kW", 0f));
//        mTextView1.setBackgroundColor(Color.CYAN);
        mTextView2.setText(String.format("%.3f V", 0f));
//        mTextView2.setBackgroundColor(Color.CYAN);
        mTextView3.setText(String.format("%.3f A", 0f));
//        mTextView3.setBackgroundColor(Color.CYAN);
        mTextView4.setText("");
//        mTextView4.setBackgroundColor(Color.CYAN);

        mButton1 = rootView.findViewById(R.id.button1);
        mButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                buttonFunction(MainActivity.MSG_GET_POTENTIAL);
                buttonFunction(MainActivity.MSG_CHART_STATUS);
                setAnime(mButton1);
            }
        });
        mCallback.fragment(this);

//      chart1.setOnChartValueSelectedListener(MainActivity.getApplicationContext());
        chart1 = rootView.findViewById(R.id.lineChart1);
        chart2 = rootView.findViewById(R.id.lineChart2);
        chart3 = rootView.findViewById(R.id.lineChart3);
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
            mCnt++;
            if (mTemp.size() > 1) {
                switch (stage) {
                    case 0:
                        records = Integer.parseInt(mTemp.get(1));
                        int size;
                        if (records > 4) {
                            size = records - 4;
                        } else {
                            size = 1;
                        }
                        mCallback.Parameter1(null);
                        mCallback.Parameter1(String.format("020406%08x0600000000120001120000", size));
                        break;

                    case 1:
                        boolean go = false;
                        long jigen;
                        jigen = MainActivity.d.DatetimeToSec(mTemp.get(mTemp.size() - 3));
                        if (cur != jigen) {
                            cur = jigen;
                            go = true;
                        }
                        if (go) {
                            int i = 1, j = 0;
//                      mTextView4.setText(mTemp.get(i++));
                            mTextView4.setText("");
                            i = mTemp.size();
                            i--;
                            for (; i > 1; j++) {
                                if (j > 0) {
                                    mTextView4.append("\n");
                                }
                                mTextView4.append(mTemp.get(i - 2) + " ");
                                int code = Integer.parseInt(mTemp.get(i - 1).toString());
                                mTextView4.append(MainActivity.d.PQCODE[code]);
                                i -= 3;
                            }
                        }
                        break;

                    case 2:
                        float wat = Integer.parseInt(mTemp.get(11).toString());
                        wat /= 1000.0;
                        float vol = Integer.parseInt(mTemp.get(13).toString());
                        vol /= 100.0;
                        float amp = Integer.parseInt(mTemp.get(15).toString());
                        amp /= 100.0;

                        mTextView1.setText(String.format("%.3f kW", wat));
                        mTextView2.setText(String.format("%.2f V", vol));
                        mTextView3.setText(String.format("%.2f A", amp));

                        LineData data1 = chart1.getData();
                        LineData data2 = chart2.getData();
                        LineData data3 = chart3.getData();

                        if (data1 != null && data2 != null && data3 != null) {
                            LineDataSet set1 = (LineDataSet) data1.getDataSetByIndex(0);
                            // set.addEntry(...); // can be called as well
                            if (set1 == null) {
                                set1 = createSet();
                                data1.addDataSet(set1);
                            }
                            LineDataSet set2 = (LineDataSet) data2.getDataSetByIndex(0);
                            // set.addEntry(...); // can be called as well
                            if (set2 == null) {
                                set2 = createSet();
                                data2.addDataSet(set2);
                            }
                            LineDataSet set3 = (LineDataSet) data3.getDataSetByIndex(0);
                            // set.addEntry(...); // can be called as well
                            if (set3 == null) {
                                set3 = createSet();
                                data3.addDataSet(set3);
                            }
                            Long now = System.currentTimeMillis();
                            int pos = timestamp.size();
                            if (pos > 0) {
                                pos--;
                                Long prv = timestamp.get(pos);
                                Long diff = now - prv;
                                if (diff > (mInterval * 2)) {
                                    prv += mInterval;
                                    while (prv < now) {
                                        timestamp.add(prv);
                                        data1.addEntry(new Entry(set1.getEntryCount(), 0f), 0);
                                        data2.addEntry(new Entry(set2.getEntryCount(), 0f), 0);
                                        data3.addEntry(new Entry(set3.getEntryCount(), 0f), 0);
                                        prv += mInterval;
                                    }
                                    yMin2 = 0f;
                                    chart2.getAxisLeft().setAxisMinimum(yMin2);
                                }
                            }
                            timestamp.add(now);
                            data1.addEntry(new Entry(set1.getEntryCount(), wat), 0);
                            data2.addEntry(new Entry(set2.getEntryCount(), vol), 0);
                            data3.addEntry(new Entry(set3.getEntryCount(), amp), 0);

                            float res;
                            res = changeMax(wat, yMax1, 0.3f);
                            if (res != yMax1) {
                                if (res > yMax1) {
                                    CapMax1 = timestamp.size();
                                    yMax1 = res;
                                    chart1.getAxisLeft().setAxisMaximum(yMax1);
                                } else {
                                    int diff = timestamp.size() - CapMax1;
                                    if (diff > mMaxEntry) {
                                        CapMax1 = timestamp.size();
                                        yMax1 = res;
                                        chart1.getAxisLeft().setAxisMaximum(yMax1);
                                    }
                                }
                            }

                            res = changeMax(vol, yMax2, 5f);
                            if (res != yMax2) {
                                if (res > yMax2) {
                                    CapMax2 = timestamp.size();
                                    yMax2 = res;
                                    chart2.getAxisLeft().setAxisMaximum(yMax2);
                                } else {
                                    int diff = timestamp.size() - CapMax2;
                                    if (diff > mMaxEntry) {
                                        CapMax2 = timestamp.size();
                                        yMax2 = res;
                                        chart2.getAxisLeft().setAxisMaximum(yMax2);
                                    }
                                }
                            }
                            res = changeMin(vol, yMin2, 5f);
                            if (res != yMin2) {
                                if (res < yMin2) {
                                    CapMin2 = timestamp.size();
                                    yMin2 = res;
                                    chart2.getAxisLeft().setAxisMinimum(yMin2);
                                } else {
                                    int diff = timestamp.size() - CapMin2;
                                    if (diff > mMaxEntry) {
                                        CapMin2 = timestamp.size();
                                        yMin2 = res;
                                        chart2.getAxisLeft().setAxisMinimum(yMin2);
                                    }
                                }
                            }
                            res = changeMax(amp, yMax3, 0.3f);
                            if (res != yMax3) {
                                if (res > yMax3) {
                                    CapMax3 = timestamp.size();
                                    yMax3 = res;
                                    chart3.getAxisLeft().setAxisMaximum(yMax3);
                                } else {
                                    int diff = timestamp.size() - CapMax3;
                                    if (diff > mMaxEntry) {
                                        CapMax3 = timestamp.size();
                                        yMax3 = res;
                                        chart3.getAxisLeft().setAxisMaximum(yMax3);
                                    }
                                }
                            }
                            data1.notifyDataChanged();
                            chart1.notifyDataSetChanged();
                            chart1.setVisibleXRangeMaximum(mMaxEntry);
                            chart1.moveViewToX(data1.getEntryCount());

                            data2.notifyDataChanged();
                            chart2.notifyDataSetChanged();
                            chart2.setVisibleXRangeMaximum(mMaxEntry);
                            chart2.moveViewToX(data2.getEntryCount());

                            data3.notifyDataChanged();
                            chart3.notifyDataSetChanged();
                            chart3.setVisibleXRangeMaximum(mMaxEntry);
                            chart3.moveViewToX(data3.getEntryCount());
                        }
                }
                ;
                if (stage < 2)
                    stage++;
                else
                    stage = 0;
            } else {
            }
            mTemp.clear();
//            mScrollview.fullScroll(View.FOCUS_DOWN);
        }
    }
    public void SetData(final ArrayList<String> in) {
    }
}