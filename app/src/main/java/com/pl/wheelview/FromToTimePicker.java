package com.pl.wheelview;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;


import java.util.ArrayList;


public class FromToTimePicker extends LinearLayout {
    public static final String TAG = "FromToTimePicker";
    private WheelView mWheelFromHour;
    private WheelView mWheelFromMinute;
    private WheelView mWheelToHour;
    private WheelView mWheelToMinute;
    private Button mCancelBtn;
    private Button mConfirmBtn;
    private int mFromMinute;
    private int mFromHour;
    private int mToHour;
    private int mToMinute;

    private OnResultListener onResultListener;

    private WheelView.OnSelectListener mFromHourListener = new WheelView.OnSelectListener() {
        @Override
        public void endSelect(int hour, String text) {
            mFromHour = hour;
        }

        @Override
        public void selecting(int id, String text) {
            mFromHour = id;
        }
    };

    private WheelView.OnSelectListener mFromMinuteListener = new WheelView.OnSelectListener() {
        @Override
        public void endSelect(int minute, String text) {
            mFromMinute  = minute;
        }

        @Override
        public void selecting(int id, String text) {
            mFromMinute  = id;
        }
    };



    private WheelView.OnSelectListener mToHourListener = new WheelView.OnSelectListener() {
        @Override
        public void endSelect(int hour, String text) {
            mToHour = hour;
        }

        @Override
        public void selecting(int hour, String text) {
            mToHour = hour;
        }
    };
    private WheelView.OnSelectListener mToMinuteListener = new WheelView.OnSelectListener() {
        @Override
        public void endSelect(int minute, String text) {
            mToMinute = minute;
        }

        @Override
        public void selecting(int minute, String text) {
            mToMinute = minute;
        }
    };

    private Activity mContext;

    public FromToTimePicker(Context context) {
        this(context, null);
    }

    public FromToTimePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setWheelViewItemNumber(int number){
        mWheelFromHour  .setItemNumber(number);
        mWheelFromMinute.setItemNumber(number);
        mWheelToHour    .setItemNumber(number);
        mWheelToMinute  .setItemNumber(number);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContext = (Activity) getContext();
        LayoutInflater.from(mContext).inflate(R.layout.time_picker_situation, this);
        mWheelFromHour = (WheelView) findViewById(R.id.from_hour);
        mWheelFromMinute = (WheelView) findViewById(R.id.from_minute);
        mWheelToHour = (WheelView) findViewById(R.id.to_hour);
        mWheelToMinute = (WheelView) findViewById(R.id.to_minute);


        mCancelBtn = (Button) findViewById(R.id.cancel);
        mConfirmBtn = (Button) findViewById(R.id.confirm);
        mWheelFromHour.setOnSelectListener(mFromHourListener);
        mWheelFromMinute.setOnSelectListener(mFromMinuteListener);
        mWheelToHour.setOnSelectListener(mToHourListener);
        mWheelToMinute.setOnSelectListener(mToMinuteListener);
        mConfirmBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onResultListener!=null){
                    onResultListener.onConfirm(mFromHour,mFromMinute,mToHour,mToMinute);
                }
            }

        });
        mCancelBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onResultListener!=null){
                    onResultListener.onCancel();
                }
            }
        });
        setDate();
    }

    private ArrayList<String> getHourData() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < 24; i++) {
            list.add(i + "");
        }
        return list;
    }

    private ArrayList<String> getMinuteData(int max) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i <= max; i++) {
            list.add(i + "");
        }
        return list;
    }

    public interface OnResultListener{
        void onConfirm(int fromHour, int fromMinute, int toHour, int toMinute);
        void onCancel();
    }

    private void setDate() {
        mWheelFromHour.setData(getHourData());
        mWheelFromMinute.setData(getMinuteData(59));
        mWheelToHour.setData(getHourData());
        mWheelToMinute.setData(getMinuteData(59));
    }

    public void setCurrentDate(String from,String to){
        // 从外面设置当前的时间进来

        mFromHour=TimeUtil.getHourFromTime(from);
        mFromMinute=TimeUtil.getMinuteFromTime(from);
        mToHour=TimeUtil.getHourFromTime(to);
        mToMinute=TimeUtil.getMinuteFromTime(to);

//        mWheelFromHour.setItemNumber(5);
//        mWheelFromMinute.setItemNumber(5);
//        mWheelToHour.setItemNumber(7);
//        mWheelToMinute.setItemNumber(9);



//        mWheelFromHour.setCyclic(false);
//        mWheelFromMinute.setCyclic(false);
//        mWheelToHour.setCyclic(false);
//        mWheelToMinute.setCyclic(false);

        mWheelFromHour.setDefault(mFromHour);
        mWheelFromMinute.setDefault(mFromMinute);
        mWheelToHour.setDefault(mToHour);
        mWheelToMinute.setDefault(mToMinute);


        WeekDay[] allWeekDays=WeekDay.values();
    }

    public void setOnResultListener(OnResultListener onResultListener) {
        this.onResultListener = onResultListener;
    }
}