package com.pl.wheelview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FromToTimePicker fromToTimePicker;
    FromToTimePicker fromToTimePicker1;

    TextView textView;
    TextView textView1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fromToTimePicker= (FromToTimePicker) findViewById(R.id.timePicker);
        fromToTimePicker1= (FromToTimePicker) findViewById(R.id.timePicker1);
        textView= (TextView) findViewById(R.id.result);
        textView1= (TextView) findViewById(R.id.result1);
        fromToTimePicker.setOnResultListener(new FromToTimePicker.OnResultListener() {
            @Override
            public void onConfirm( int fromHour, int fromMinute, int toHour, int toMinute) {
                textView.setText("From "+fromHour+":"+fromMinute+" To "+toHour+":"+toMinute);
            }

            @Override
            public void onCancel() {
                textView.setText("Canceled");
            }
        });
        fromToTimePicker.setCurrentDate(TimeUtil.getTimeString(),TimeUtil.addTime(TimeUtil.getTimeString(),"8:00").getTime());

        fromToTimePicker1.setOnResultListener(new FromToTimePicker.OnResultListener() {
            @Override
            public void onConfirm(int fromHour, int fromMinute, int toHour, int toMinute) {
                textView1.setText("From "+fromHour+":"+fromMinute+" To "+toHour+":"+toMinute);
            }

            @Override
            public void onCancel() {
                textView1.setText("Canceled");
            }
        });
        fromToTimePicker1.setWheelViewItemNumber(5);
    }
}
