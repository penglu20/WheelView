package com.pl.wheelview;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by l4656_000 on 2015/11/26.
 */
public class TimeUtil {

    public static final long ONEDAY=1*24*60*60*1000;
    public static final String TIMEFORMAT="HH:mm";
    public static final String DATEFORMAT="yyyy-MM-dd";
    /*
    *按照时间格式hh:mm:ss对两个字符串time1和time2进行比较
    * 若相等，返回0
    * 若time1>time2，返回正数
    * 若time2>time1，返回负数
     */
    public static int compare(String time1,String time2){
        if (time1==null&&time2!=null){
            //todo remain rethinking
            return -1;
        }
        if (time2==null&&time1!=null){
            //todo remain rethinking
            return 1;
        }
        if (time1==null&&time2==null){
            return 0;
        }
        String[] t1=time1.split(":");
        String[] t2=time2.split(":");
        for (int i=0;i<t1.length;i++){
            if (t2.length<=i){
                //2比1短
                return 1;
            }
            int a=Integer.valueOf(t1[i])-Integer.valueOf(t2[i]);
            if (a!=0){
                return a;
            }
        }
        if (t2.length>t1.length){
            //2比1长
            return -1;
        }
        return 0;
    }

    public static SimpleDateFormat getTimeFormate(){
        return getTimeFormate(TIMEFORMAT);
    }

    public static SimpleDateFormat getTimeFormate(String formate){
        return new SimpleDateFormat(formate);
    }

    public static SimpleDateFormat getDateFormate(){
        return getDateFormate(DATEFORMAT);
    }

    public static SimpleDateFormat getDateFormate(String formate){
        return new SimpleDateFormat(formate);
    }

    public static String getTimeString(){
        SimpleDateFormat sf=getTimeFormate();
        String time=sf.format(new Date(System.currentTimeMillis()));
        return time;
    }


    public static String getDateString(){
        return getDateString(System.currentTimeMillis());
    }

    public static String getDateString(long time){
        SimpleDateFormat sf=getDateFormate();
        String date=sf.format(new Date(time));
        return date;
    }

    public static int getCurrentHour(){
        Calendar date=Calendar.getInstance();
        return date.get(Calendar.HOUR_OF_DAY);
    }

    public static int getCurrentMin(){
        Calendar date=Calendar.getInstance();
        return date.get(Calendar.MINUTE);
    }

    public static int getHourFromTime(String time){//time="13:25"
        TimeResult timeResult=isTimeStandard(time);
        if (timeResult.isStandard){
            return timeResult.hour;
        }else {
            return -1;
        }
    }
    public static int getMinuteFromTime(String time){//time="13:25"
        TimeResult timeResult=isTimeStandard(time);
        if (timeResult.isStandard){
            return timeResult.minute;
        }else {
            return -1;
        }
    }

    public static TimeResult addTime(String time,String added){
        TimeResult timeResult=isTimeStandard(time);
        TimeResult addedTime=isTimeStandard(added);

        if (timeResult.isStandard&&addedTime.isStandard){
            timeResult.hour+=addedTime.hour;
            timeResult.minute+=addedTime.minute;
            if (timeResult.minute>=60){
                timeResult.minute-=60;
                timeResult.hour+=1;
            }
            if (timeResult.hour>=24){
                timeResult.hour-=24;
                timeResult.isNextDay=true;
            }
            return timeResult;
        }else {
            addedTime.isStandard=false;
            return addedTime;
        }

    }

    private static int hourOfTime(String time){
        String hour=time.split(":")[0];
        return Integer.parseInt(hour);
    }

    private static int minuteOfTime(String time){
        String minute=time.split(":")[1];
        return Integer.parseInt(minute);
    }

    public static TimeResult isTimeStandard(String time){
        TimeResult timeResult=new TimeResult();
        timeResult.isStandard=true;
        if (time.split(":").length!=2){
            timeResult.isStandard=false;
        }
        int hour=hourOfTime(time);
        int minute=minuteOfTime(time);
        if (hour==24&&minute==0){
            timeResult.isStandard=true;
        }else if (hour>23||hour<0){
            timeResult.isStandard=false;
        }else if (minute>59||minute<0){
            timeResult.isStandard=false;
        }
        timeResult.hour=hour;
        timeResult.minute=minute;
        return timeResult;
    }

    public static int getWeekDay(){
        Calendar date=Calendar.getInstance();
        return date.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY;
    }

    public static TimeResult minusTime(String time,String toMinus){
        TimeResult timeResult=isTimeStandard(time);
        TimeResult minusTime=isTimeStandard(toMinus);
        if (timeResult.isStandard&&minusTime.isStandard){
            timeResult.hour-=minusTime.hour;
            timeResult.minute-=minusTime.minute;
            if (timeResult.minute<0){
                timeResult.minute+=60;
                timeResult.hour-=1;
            }
            if (timeResult.hour<0){
                timeResult.hour+=24;
                timeResult.isNextDay=true;
            }
            return timeResult;
        }else {
            minusTime.isStandard=false;
            return minusTime;
        }

    }

    public static class TimeResult{
        public int hour;
        public int minute;
        public boolean isNextDay;
        public boolean isStandard;
        public String getTime(){
            StringBuilder sb=new StringBuilder();
            if (hour<10){
                sb.append("0"+hour);
            }else {
                sb.append(hour);
            }
            sb.append(":");
            if (minute<10){
                sb.append("0"+minute);
            }else {
                sb.append(minute);
            }
            return sb.toString();
        }
    }

}
