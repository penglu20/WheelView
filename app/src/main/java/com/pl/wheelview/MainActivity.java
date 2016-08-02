package com.pl.wheelview;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FromToTimePicker fromToTimePicker;
    CityPickerLayout cityPicker;

    TextView textView;
    TextView textView1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView= (TextView) findViewById(R.id.result);
        textView1= (TextView) findViewById(R.id.result1);

        fromToTimePicker= (FromToTimePicker) findViewById(R.id.timePicker);
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


        cityPicker = (CityPickerLayout) findViewById(R.id.cityPicker);
        cityPicker.setWheelViewItemNumber(5);
//        NumberPicker numberPicker= (NumberPicker) findViewById(R.id.numberpicker);
//        ArrayList<String> list = new ArrayList<String>();
//        for (int i = 0; i <= 120; i++) {
//            list.add(i + "ge");
//        }
////        numberPicker.setDisplayedValues(list.toArray(new String[120]));
//        numberPicker.setMinValue(30);
//        numberPicker.setMaxValue(120);

    }
}
