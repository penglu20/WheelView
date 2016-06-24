package com.pl.wheelview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

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
    }
}
